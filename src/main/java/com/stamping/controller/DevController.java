package com.stamping.controller;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stamping.model.StampResponse;
import com.stamping.service.DemoConfigGeneratorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * DEV-ONLY controller — exposes development/testing utilities.
 * Only active when spring.profiles.active includes "dev".
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/dev")
@RequiredArgsConstructor
@Profile("dev")
public class DevController {

    private final DemoConfigGeneratorService demoConfigGeneratorService;
    private final ObjectMapper objectMapper;

    /**
     * Manually trigger demo test config generation for an existing saved config.
     * POST /api/v1/dev/generate-test-config/{pubId}/{jcode}
     */
    @PostMapping(value = "/generate-test-config/{pubId}/{jcode}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StampResponse> generateTestConfig(
            @PathVariable String pubId,
            @PathVariable String jcode) {
        try {
            File configFile = new File("configs", "config_" + pubId + "_" + jcode + ".json");
            if (!configFile.exists()) {
                return ResponseEntity.status(404).body(StampResponse.builder()
                        .success(false)
                        .message("No saved config found for pubId=" + pubId + ", jcode=" + jcode)
                        .build());
            }

            String content = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> rawConfig = objectMapper.readValue(content, Map.class);

            String outputPath = demoConfigGeneratorService.generateDemoTestConfig(rawConfig);

            if (outputPath == null) {
                return ResponseEntity.internalServerError().body(StampResponse.builder()
                        .success(false).message("Failed to generate demo test config").build());
            }

            return ResponseEntity.ok(StampResponse.builder()
                    .success(true)
                    .message("Demo test config generated")
                    .outputFilePath(outputPath)
                    .build());

        } catch (Exception e) {
            log.error("[DEV] Error generating test config", e);
            return ResponseEntity.internalServerError().body(StampResponse.builder()
                    .success(false).message("Error: " + e.getMessage()).build());
        }
    }
}
