package com.stamping.controller;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stamping.config.StampingProperties;
import com.stamping.exception.StampingException;
import com.stamping.model.JournalMetadataRequest;
import com.stamping.model.StampResponse;
import com.stamping.service.DemoStampService;
import com.stamping.service.InputSanitizer;
import com.stamping.service.StampOrchestrationService;
import com.stamping.service.StampOrchestrationService.StampResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class StampController {

    private final StampOrchestrationService orchestrationService;
    private final ObjectMapper objectMapper;
    private final InputSanitizer inputSanitizer;
    private final StampingProperties properties;
    private final DemoStampService demoStampService;

    @Autowired
    public StampController(StampOrchestrationService orchestrationService,
                           ObjectMapper objectMapper,
                           InputSanitizer inputSanitizer,
                           StampingProperties properties,
                           DemoStampService demoStampService) {
        this.orchestrationService = orchestrationService;
        this.objectMapper = objectMapper;
        this.inputSanitizer = inputSanitizer;
        this.properties = properties;
        this.demoStampService = demoStampService;
    }

    // ─── Stamping Endpoints ─────────────────────────────────────────────

    @PostMapping(value = "/stamp/journal-metadata", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> processJournalMetadata(@RequestBody JournalMetadataRequest request) {
        File demoTempFile = null;
        try {
            request.applyDemoDefaults();

            // In demo mode, auto-fill positions and create blank PDF if needed
            if (request.isDemoMode()) {
                demoTempFile = prepareDemoMode(request);
            }

            StampResult result = orchestrationService.processJournalMetadata(request);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(result.pdfBytes().length)
                    .body(result.pdfBytes());

        } catch (StampingException e) {
            throw e;
        } catch (Exception e) {
            throw new StampingException("Failed to process journal metadata request: " + e.getMessage(), e);
        } finally {
            if (demoTempFile != null && demoTempFile.exists()) {
                demoTempFile.delete();
            }
        }
    }

    @GetMapping(value = "/stamp/demo-pdf/{pubId}/{jcode}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadDemoPdf(@PathVariable String pubId, @PathVariable String jcode) {
        File tempFile = null;
        try {
            inputSanitizer.validateIdentifier(pubId, "pubId");
            inputSanitizer.validateIdentifier(jcode, "jcode");

            JournalMetadataRequest demoRequest = demoStampService.buildDemoRequest(pubId, jcode);
            byte[] blankPdf = demoStampService.createBlankPdf();

            tempFile = File.createTempFile("demo_blank_", ".pdf");
            tempFile.deleteOnExit();
            Files.write(tempFile.toPath(), blankPdf);
            demoRequest.setPdfFilePath(tempFile.getAbsolutePath());
            demoRequest.setOutputPath(null);

            StampResult result = orchestrationService.processJournalMetadata(demoRequest);

            String filename = "demo_" + pubId + "_" + jcode + "_stamped.pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(result.pdfBytes().length)
                    .body(result.pdfBytes());
        } catch (StampingException e) {
            throw e;
        } catch (Exception e) {
            throw new StampingException("Failed to generate demo PDF: " + e.getMessage(), e);
        } finally {
            if (tempFile != null && tempFile.exists()) tempFile.delete();
        }
    }

    // ─── Config CRUD ────────────────────────────────────────────────────

    @GetMapping(value = "/configs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> listConfigs() {
        try {
            File configDir = new File(properties.getConfigDir());
            List<Object> results = new ArrayList<>();
            if (configDir.exists() && configDir.isDirectory()) {
                File[] files = configDir.listFiles((dir, name) -> name.startsWith("config_") && name.endsWith(".json"));
                if (files != null) {
                    Arrays.sort(files);
                    for (File f : files) {
                        String content = Files.readString(f.toPath(), StandardCharsets.UTF_8);
                        results.add(objectMapper.readValue(content, Object.class));
                    }
                }
            }
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Failed to list configs", e);
            return ResponseEntity.internalServerError().body(StampResponse.builder()
                    .success(false).message("Failed to list configurations").build());
        }
    }

    @GetMapping(value = "/configs/{pubId}/{jcode}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getConfig(@PathVariable String pubId, @PathVariable String jcode) {
        try {
            inputSanitizer.validateIdentifier(pubId, "pubId");
            inputSanitizer.validateIdentifier(jcode, "jcode");

            File configFile = new File(properties.getConfigDir(), "config_" + pubId + "_" + jcode + ".json");
            if (!configFile.exists()) {
                return ResponseEntity.status(404).body(StampResponse.builder()
                        .success(false).message("No configuration found for pubId=" + pubId + ", jcode=" + jcode).build());
            }
            String content = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
            return ResponseEntity.ok(objectMapper.readValue(content, Object.class));
        } catch (StampingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get config", e);
            return ResponseEntity.internalServerError().body(StampResponse.builder()
                    .success(false).message("Failed to get configuration").build());
        }
    }

    @PostMapping(value = "/configs", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StampResponse> saveConfig(@RequestBody Map<String, Object> rawConfig) {
        try {
            String pubId = (String) rawConfig.get("pubId");
            String jcode = (String) rawConfig.get("jcode");
            inputSanitizer.validateIdentifier(pubId, "pubId");
            inputSanitizer.validateIdentifier(jcode, "jcode");

            // Strip unused "value" field from adsBanner in each section
            for (String section : new String[]{"newPage", "header", "footer", "leftMargin", "rightMargin"}) {
                Object sectionObj = rawConfig.get(section);
                if (sectionObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> sectionMap = (Map<String, Object>) sectionObj;
                    Object adsBannerObj = sectionMap.get("adsBanner");
                    if (adsBannerObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> adsBanner = (Map<String, Object>) adsBannerObj;
                        adsBanner.remove("value");
                    }
                }
            }

            File configDir = new File(properties.getConfigDir());
            if (!configDir.exists()) configDir.mkdirs();

            File outputFile = new File(configDir, "config_" + pubId + "_" + jcode + ".json");
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rawConfig);
            Files.writeString(outputFile.toPath(), json, StandardCharsets.UTF_8);

            log.info("Config saved: {}", outputFile.getAbsolutePath());

            return ResponseEntity.ok(StampResponse.builder()
                    .success(true).message("Configuration saved successfully")
                    .outputFilePath(outputFile.getAbsolutePath()).build());
        } catch (StampingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to save config", e);
            return ResponseEntity.internalServerError().body(StampResponse.builder()
                    .success(false).message("Failed to save configuration").build());
        }
    }

    @DeleteMapping(value = "/configs/{pubId}/{jcode}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StampResponse> archiveConfig(@PathVariable String pubId, @PathVariable String jcode) {
        try {
            inputSanitizer.validateIdentifier(pubId, "pubId");
            inputSanitizer.validateIdentifier(jcode, "jcode");

            File configFile = new File(properties.getConfigDir(), "config_" + pubId + "_" + jcode + ".json");
            if (!configFile.exists()) {
                return ResponseEntity.status(404).body(StampResponse.builder()
                        .success(false).message("Configuration not found in active directory").build());
            }

            File archiveDir = new File(properties.getArchiveDir());
            if (!archiveDir.exists()) archiveDir.mkdirs();

            File archiveFile = new File(archiveDir, configFile.getName());
            Files.move(configFile.toPath(), archiveFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return ResponseEntity.ok(StampResponse.builder().success(true).message("Configuration archived successfully").build());
        } catch (StampingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to archive config", e);
            return ResponseEntity.internalServerError().body(StampResponse.builder()
                    .success(false).message("Failed to archive configuration").build());
        }
    }

    @PutMapping(value = "/configs/{pubId}/{jcode}/restore", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StampResponse> restoreConfig(@PathVariable String pubId, @PathVariable String jcode) {
        try {
            inputSanitizer.validateIdentifier(pubId, "pubId");
            inputSanitizer.validateIdentifier(jcode, "jcode");

            File archiveFile = new File(properties.getArchiveDir(), "config_" + pubId + "_" + jcode + ".json");
            if (!archiveFile.exists()) {
                return ResponseEntity.status(404).body(StampResponse.builder()
                        .success(false).message("Configuration not found in archive directory").build());
            }

            File configDir = new File(properties.getConfigDir());
            if (!configDir.exists()) configDir.mkdirs();

            File configFile = new File(configDir, archiveFile.getName());
            Files.move(archiveFile.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return ResponseEntity.ok(StampResponse.builder().success(true).message("Configuration restored successfully").build());
        } catch (StampingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to restore config", e);
            return ResponseEntity.internalServerError().body(StampResponse.builder()
                    .success(false).message("Failed to restore configuration").build());
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────

    private File prepareDemoMode(JournalMetadataRequest request) throws Exception {
        if (request.getPositions() == null || request.getPositions().isEmpty()) {
            if (request.getPublisherId() != null && !request.getPublisherId().isBlank()
                    && request.getJcode() != null && !request.getJcode().isBlank()) {
                JournalMetadataRequest demoReq =
                        demoStampService.buildDemoRequest(request.getPublisherId(), request.getJcode());
                request.setPositions(demoReq.getPositions());
                log.info("  [DEMO] Auto-loaded positions from saved config for {}/{}",
                        request.getPublisherId(), request.getJcode());
            }
        }
        File demoTempFile = null;
        if (request.getPdfFilePath() == null || request.getPdfFilePath().isBlank()) {
            byte[] blankPdf = demoStampService.createBlankPdf();
            demoTempFile = File.createTempFile("demo_blank_", ".pdf");
            demoTempFile.deleteOnExit();
            Files.write(demoTempFile.toPath(), blankPdf);
            request.setPdfFilePath(demoTempFile.getAbsolutePath());
            log.info("  [DEMO] Created blank placeholder PDF: {}", demoTempFile.getAbsolutePath());
        }
        return demoTempFile;
    }
}
