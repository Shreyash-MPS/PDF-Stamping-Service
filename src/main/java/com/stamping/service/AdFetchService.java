package com.stamping.service;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.stamping.model.ad.AdResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AdFetchService {

    private final RestTemplate restTemplate;

    public AdFetchService(RestTemplateBuilder restTemplateBuilder) {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(15_000);
        this.restTemplate = restTemplateBuilder.requestFactory(() -> factory).build();
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

            // Parse manually so we can log the raw body on failure
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper.readValue(raw.getBody(), AdResponse.class);
        } catch (Exception e) {
            log.error("Failed to fetch/parse ads from URL '{}': {}", url, e.getMessage(), e);
            return null;
        }
    }
}
