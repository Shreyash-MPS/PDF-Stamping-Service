package com.stamping.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.stamping.model.StampRequest;
import com.stamping.model.ad.AdData;
import com.stamping.model.ad.AdLocation;
import com.stamping.model.ad.AdResponse;
import com.stamping.model.ad.Section;
import com.stamping.service.stamper.Stamper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AdStampServiceTest {

    @Mock
    private AdFetchService adFetchService;

    @Mock
    private Stamper htmlStamper;

    private AdStampService adStampService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adStampService = new AdStampService(adFetchService, htmlStamper);
    }

    private byte[] createMinimalPdf() throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
            pdfDoc.addNewPage();
            pdfDoc.close();
            return baos.toByteArray();
        }
    }

    @Test
    void testProcessAdJson_NoAdHtml() throws Exception {
        AdResponse adResponse = new AdResponse();
        when(adFetchService.fetchAds(anyString())).thenReturn(adResponse);

        byte[] inputPdf = createMinimalPdf();
        byte[] result = adStampService.processAdJson(inputPdf, "http://example.com/ads.json", "all");

        // When no ad is found, it should return the original byte array
        assertArrayEquals(inputPdf, result);
        verifyNoInteractions(htmlStamper);
    }

    @Test
    void testProcessAdJson_WithHeaderAd() throws Exception {
        // Prepare Mock Ad Data
        AdResponse adResponse = new AdResponse();
        Section section = new Section();
        AdLocation locations = new AdLocation();
        AdData adData = new AdData();

        adData.setAdHtml("<div><h1>Header Ad</h1></div>");
        adData.setAdId("123");

        locations.setPositionName("header");
        locations.setAdData(Collections.singletonList(adData));

        section.setAdLocation(Collections.singletonList(locations));
        adResponse.setSection(Collections.singletonList(section));

        when(adFetchService.fetchAds(anyString())).thenReturn(adResponse);

        byte[] inputPdf = createMinimalPdf();
        byte[] expectedStampedPdf = "stamped-pdf-bytes".getBytes();

        when(htmlStamper.stamp(eq(inputPdf), any(StampRequest.class), any(byte[].class)))
                .thenReturn(expectedStampedPdf);

        // Execute
        byte[] result = adStampService.processAdJson(inputPdf, "http://example.com/ads.json", "header");

        // Verify that the stamper was called and returned the expected output
        assertArrayEquals(expectedStampedPdf, result);

        // Verify stamper interactions
        ArgumentCaptor<StampRequest> requestCaptor = ArgumentCaptor.forClass(StampRequest.class);
        verify(htmlStamper).stamp(eq(inputPdf), requestCaptor.capture(), any(byte[].class));
        assertEquals(com.stamping.model.StampType.HTML, requestCaptor.getValue().getStampType());
    }

    @Test
    void testProcessAdJson_WithPdfAdOne() throws Exception {
        // Prepare Mock Ad Data
        AdResponse adResponse = new AdResponse();
        Section section = new Section();
        AdLocation locations = new AdLocation();
        AdData adData = new AdData();

        adData.setAdHtml("<div><h1>Ad Page</h1><p>Test Content</p></div>");
        adData.setAdId("123");

        locations.setPositionName("pdf ad one");
        locations.setAdData(Collections.singletonList(adData));

        section.setAdLocation(Collections.singletonList(locations));
        adResponse.setSection(Collections.singletonList(section));

        when(adFetchService.fetchAds(anyString())).thenReturn(adResponse);

        byte[] inputPdf = createMinimalPdf();

        // Check input length
        PdfDocument originalDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(inputPdf)));
        int originalPages = originalDoc.getNumberOfPages(); // Should be 1
        originalDoc.close();

        // Execute
        byte[] result = adStampService.processAdJson(inputPdf, "http://example.com/ads.json", "all");

        // Verify output length
        PdfDocument resultDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(result)));
        int resultPages = resultDoc.getNumberOfPages(); // Should be original + ad page (1 + 1 = 2)
        resultDoc.close();

        assertEquals(originalPages + 1, resultPages, "Result PDF should have one extra page");
        verifyNoInteractions(htmlStamper); // Ensure header stamper wasn't called
    }
}
