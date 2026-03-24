package com.stamping.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.web.client.RestTemplate;

import com.stamping.model.ad.AdResponse;

class AdFetchServiceTest {

    private AdFetchService adFetchService;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        RestTemplateBuilder builder = new RestTemplateBuilder()
                .requestFactory(() -> restTemplate.getRequestFactory());
        adFetchService = new AdFetchService(builder);

        // Get the actual RestTemplate the service built, via reflection
        try {
            java.lang.reflect.Field field = AdFetchService.class.getDeclaredField("restTemplate");
            field.setAccessible(true);
            RestTemplate actualTemplate = (RestTemplate) field.get(adFetchService);
            mockServer = MockRestServiceServer.createServer(actualTemplate);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testFetchAds_Success() {
        String jsonResponse = "{\n" +
                "  \"publisherId\": \"cshl\",\n" +
                "  \"journlcode\": \"genome\",\n" +
                "  \"section\": [\n" +
                "    {\n" +
                "      \"sectionId\": \"2\",\n" +
                "      \"adLocation\": [\n" +
                "        {\n" +
                "          \"positionId\": \"1\",\n" +
                "          \"positionName\": \"header\",\n" +
                "          \"adData\": [\n" +
                "            {\n" +
                "              \"adId\": \"58009\",\n" +
                "              \"adHtml\": \"<div>Ad Content</div>\"\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        mockServer.expect(requestTo("http://example.com/ads.json"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        AdResponse response = adFetchService.fetchAds("http://example.com/ads.json");

        assertNotNull(response);
        assertEquals("cshl", response.getPublisherId());
        assertEquals(1, response.getSection().size());
        assertEquals("header", response.getSection().get(0).getAdLocation().get(0).getPositionName());
        assertEquals("<div>Ad Content</div>",
                response.getSection().get(0).getAdLocation().get(0).getAdData().get(0).getAdHtml());

        mockServer.verify();
    }
}
