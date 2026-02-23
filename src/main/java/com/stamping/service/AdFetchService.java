package com.stamping.service;

import com.stamping.model.ad.AdResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class AdFetchService {

    private final RestTemplate restTemplate;

    public AdFetchService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public AdResponse fetchAds(String url) {
        log.info("Fetching ads from URL: {}", url);
        try {
            return restTemplate.getForObject(url, AdResponse.class);
        } catch (Exception e) {
            log.error("Error fetching ads from URL: {}", url, e);
            throw new RuntimeException("Failed to fetch ads", e);
        }
    }
}
