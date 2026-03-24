package com.stamping.service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stamping.config.StampingProperties;
import com.stamping.model.DynamicStampRequest;
import com.stamping.model.JournalMetadataRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * DEV-ONLY service — generates a demo JournalMetadataRequest JSON file
 * from a saved frontend configuration. Used for quick API testing.
 * Only active when spring.profiles.active includes "dev".
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Profile("dev")
public class DemoConfigGeneratorService {

    private final ObjectMapper objectMapper;
    private final StampingProperties properties;

    private static final String DEMO_PDF_PATH = "C:/stamp-test/1.pdf";
    private static final String DEMO_OUTPUT_PATH = "C:/stamp-test/1_stamp.pdf";

    /**
     * Generates a demo JournalMetadataRequest JSON file from a saved frontend config.
     * Output is written to test_requests/journal_metadata_{pubId}_{jcode}.json
     *
     * @param rawConfig the raw frontend config map (as saved to configs/)
     * @return the path of the generated file
     */
    public String generateDemoTestConfig(Map<String, Object> rawConfig) {
        try {
            String pubId = (String) rawConfig.get("pubId");
            String jcode = (String) rawConfig.get("jcode");

            JournalMetadataRequest demo = JournalMetadataRequest.builder()
                    .pdfFilePath(DEMO_PDF_PATH)
                    .outputPath(DEMO_OUTPUT_PATH)
                    .publisherId(pubId)
                    .jcode(jcode)
                    .articleTitle("Sample Article Title for Testing")
                    .authors("Author One, Author Two, Author Three")
                    .doiValue("10.xxxx/sample.doi.2026")
                    .articleCopyright("© 2026 " + pubId + ". All rights reserved.")
                    .articleIssn("1234-5678")
                    .articleId("ART-" + jcode.toUpperCase() + "-001")
                    .downloadedBy("demo.user@example.com")
                    .positions(buildPositionsFromConfig(rawConfig))
                    .build();

            File outputDir = new File(properties.getTestRequestsDir());
            if (!outputDir.exists()) outputDir.mkdirs();

            String filename = "journal_metadata_" + pubId + "_" + jcode + ".json";
            File outputFile = new File(outputDir, filename);

            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(demo);
            Files.writeString(outputFile.toPath(), json, StandardCharsets.UTF_8);

            log.info("[DEV] Generated demo test config: {}", outputFile.getAbsolutePath());
            return outputFile.getAbsolutePath();

        } catch (Exception e) {
            log.warn("[DEV] Failed to generate demo test config: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Public entry point — builds the positions map from a raw frontend config.
     * Used by DemoStampService to construct a demo JournalMetadataRequest.
     */
    public Map<String, DynamicStampRequest.Configuration> buildDemoPositions(Map<String, Object> rawConfig) {
        return buildPositionsFromConfig(rawConfig);
    }

    /**
     * Translates the frontend config positions (newPage, header, footer, leftMargin, rightMargin)
     * into the positions map expected by JournalMetadataRequest.
     */
    private Map<String, DynamicStampRequest.Configuration> buildPositionsFromConfig(Map<String, Object> rawConfig) {
        Map<String, DynamicStampRequest.Configuration> positions = new LinkedHashMap<>();

        String templateName = (String) rawConfig.getOrDefault("templateName", "default_metadata");

        addPositionIfEnabled(positions, rawConfig, "newPage",    "NEW_PAGE",    templateName);
        addPositionIfEnabled(positions, rawConfig, "header",     "HEADER",      null);
        addPositionIfEnabled(positions, rawConfig, "footer",     "FOOTER",      null);
        addPositionIfEnabled(positions, rawConfig, "leftMargin", "LEFT_MARGIN", null);
        addPositionIfEnabled(positions, rawConfig, "rightMargin","RIGHT_MARGIN",null);

        return positions;
    }

    @SuppressWarnings("unchecked")
    private void addPositionIfEnabled(
            Map<String, DynamicStampRequest.Configuration> positions,
            Map<String, Object> rawConfig,
            String configKey,
            String positionKey,
            String templateName) {

        Object posObj = rawConfig.get(configKey);
        if (!(posObj instanceof Map)) return;

        Map<String, Object> pos = (Map<String, Object>) posObj;
        Boolean enabled = (Boolean) pos.get("enabled");
        if (!Boolean.TRUE.equals(enabled)) return;

        DynamicStampRequest.Configuration.ConfigurationBuilder builder =
                DynamicStampRequest.Configuration.builder();

        // Template (only for NEW_PAGE)
        if (templateName != null) {
            builder.templateName(templateName);
        }

        // pagePosition (only relevant for NEW_PAGE, but harmless to always set if present)
        String pagePosition = (String) pos.get("pagePosition");
        if (pagePosition != null && !pagePosition.isBlank()) {
            builder.pagePosition(pagePosition);
        }

        // Metadata toggles — only set when true to keep JSON clean
        if (getBool(pos, "articleTitle"))    builder.includeArticleTitle(true);
        if (getBool(pos, "articleAuthors"))  builder.includeAuthors(true);
        if (getBool(pos, "articleDoi"))      builder.includeDoi(true);
        if (getBool(pos, "dateOfDownload"))  builder.includeDate(true);
        if (getBool(pos, "downloadBy"))      builder.includeCurrentUser(true);
        if (getBool(pos, "articleCopyright"))builder.includeCopyright(true);
        if (getBool(pos, "articleIssn"))     builder.includeIssn(true);
        if (getBool(pos, "articleId"))       builder.includeArticleId(true);

        // Text
        Map<String, Object> textObj = getMap(pos, "text");
        if (textObj != null && Boolean.TRUE.equals(textObj.get("enabled"))) {
            builder.text((String) textObj.get("value"));
        }

        // HTML
        Map<String, Object> htmlObj = getMap(pos, "html");
        if (htmlObj != null && Boolean.TRUE.equals(htmlObj.get("enabled"))) {
            builder.html((String) htmlObj.get("value"));
        }

        // Logo
        Map<String, Object> logoObj = getMap(pos, "logo");
        if (logoObj != null && Boolean.TRUE.equals(logoObj.get("enabled"))) {
            String logoValue = (String) logoObj.get("value");
            if (logoValue != null && !logoValue.isBlank()) {
                builder.logo(logoValue);
                // Try to detect mime type from data URI prefix
                if (logoValue.startsWith("data:")) {
                    int semicolon = logoValue.indexOf(';');
                    if (semicolon > 5) {
                        builder.logoMimeType(logoValue.substring(5, semicolon));
                    }
                    // Strip the data URI prefix — controller expects raw base64
                    int comma = logoValue.indexOf(',');
                    if (comma >= 0) {
                        builder.logo(logoValue.substring(comma + 1));
                    }
                }
            }
        }

        // Link
        Map<String, Object> linkObj = getMap(pos, "link");
        if (linkObj != null && Boolean.TRUE.equals(linkObj.get("enabled"))) {
            builder.linkUrl((String) linkObj.get("url"));
            builder.linkText((String) linkObj.get("text"));
        }

        // Ads Banner
        Map<String, Object> adsBannerObj = getMap(pos, "adsBanner");
        if (adsBannerObj != null && Boolean.TRUE.equals(adsBannerObj.get("enabled"))) {
            builder.adsEnabled(true);
            String legacyDomain = (String) adsBannerObj.get("legacyDomain");
            if (legacyDomain != null && !legacyDomain.isBlank()) {
                builder.legacyDomain(legacyDomain);
            }
        }

        positions.put(positionKey, builder.build());
    }

    private boolean getBool(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return Boolean.TRUE.equals(val);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return (val instanceof Map) ? (Map<String, Object>) val : null;
    }
}
