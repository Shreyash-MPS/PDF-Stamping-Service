package com.stamping.service;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stamping.config.StampingProperties;
import com.stamping.model.ad.AdResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AdFetchService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AdFetchService(RestTemplateBuilder restTemplateBuilder,
                          ObjectMapper objectMapper,
                          StampingProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getAds().getConnectTimeout());
        factory.setReadTimeout(properties.getAds().getReadTimeout());
        this.restTemplate = restTemplateBuilder.requestFactory(() -> factory).build();
        // Configure a dedicated mapper that ignores unknown properties
        this.objectMapper = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Fetches ads from the given BAM URL.
     * Returns null (instead of throwing) when the fetch fails, so callers can
     * decide whether to skip ads gracefully or retry.
     */
    public AdResponse fetchAds(String url) {
        log.info("Fetching ads from URL: '{}'", url);
        try {
            ResponseEntity<String> raw = restTemplate.getForEntity(url.trim(), String.class);
            log.debug("Ad response status={}, body length={}", raw.getStatusCode(),
                    raw.getBody() != null ? raw.getBody().length() : 0);

            if (raw.getBody() == null || raw.getBody().isBlank()) {
                log.warn("Empty response body from ad URL: {}", url);
                return null;
            }

            return objectMapper.readValue(raw.getBody(), AdResponse.class);
        } catch (Exception e) {
            log.error("Failed to fetch/parse ads from URL '{}': {}", url, e.getMessage(), e);
            return null;
        }
    }
}
