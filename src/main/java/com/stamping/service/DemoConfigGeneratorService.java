package com.stamping.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.stamping.model.DynamicStampRequest;

import lombok.RequiredArgsConstructor;

/**
 * Translates a saved frontend config map into the positions map
 * expected by JournalMetadataRequest. Used by DemoStampService
 * to build demo/sample PDF requests from stored configurations.
 */
@Service
@RequiredArgsConstructor
public class DemoConfigGeneratorService {

    /**
     * Builds the positions map from a raw frontend config.
     */
    public Map<String, DynamicStampRequest.Configuration> buildDemoPositions(Map<String, Object> rawConfig) {
        return buildPositionsFromConfig(rawConfig);
    }

    private Map<String, DynamicStampRequest.Configuration> buildPositionsFromConfig(Map<String, Object> rawConfig) {
        Map<String, DynamicStampRequest.Configuration> positions = new LinkedHashMap<>();

        String templateName = (String) rawConfig.getOrDefault("templateName", "default_metadata");

        addPositionIfEnabled(positions, rawConfig, "newPage",     "NEW_PAGE",    templateName);
        addPositionIfEnabled(positions, rawConfig, "header",      "HEADER",      null);
        addPositionIfEnabled(positions, rawConfig, "footer",      "FOOTER",      null);
        addPositionIfEnabled(positions, rawConfig, "leftMargin",  "LEFT_MARGIN", null);
        addPositionIfEnabled(positions, rawConfig, "rightMargin", "RIGHT_MARGIN",null);

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
        if (!Boolean.TRUE.equals(pos.get("enabled"))) return;

        DynamicStampRequest.Configuration.ConfigurationBuilder builder =
                DynamicStampRequest.Configuration.builder();

        if (templateName != null) {
            builder.templateName(templateName);
        }

        String pagePosition = (String) pos.get("pagePosition");
        if (pagePosition != null && !pagePosition.isBlank()) {
            builder.pagePosition(pagePosition);
        }

        if (getBool(pos, "articleTitle"))     builder.includeArticleTitle(true);
        if (getBool(pos, "articleAuthors"))   builder.includeAuthors(true);
        if (getBool(pos, "articleDoi"))       builder.includeDoi(true);
        if (getBool(pos, "dateOfDownload"))   builder.includeDate(true);
        if (getBool(pos, "downloadBy"))       builder.includeCurrentUser(true);
        if (getBool(pos, "articleCopyright")) builder.includeCopyright(true);
        if (getBool(pos, "articleIssn"))      builder.includeIssn(true);
        if (getBool(pos, "articleId"))        builder.includeArticleId(true);

        Map<String, Object> textObj = getMap(pos, "text");
        if (textObj != null && Boolean.TRUE.equals(textObj.get("enabled"))) {
            builder.text((String) textObj.get("value"));
        }

        Map<String, Object> htmlObj = getMap(pos, "html");
        if (htmlObj != null && Boolean.TRUE.equals(htmlObj.get("enabled"))) {
            builder.html((String) htmlObj.get("value"));
        }

        Map<String, Object> logoObj = getMap(pos, "logo");
        if (logoObj != null && Boolean.TRUE.equals(logoObj.get("enabled"))) {
            String logoValue = (String) logoObj.get("value");
            if (logoValue != null && !logoValue.isBlank()) {
                if (logoValue.startsWith("data:")) {
                    int semicolon = logoValue.indexOf(';');
                    if (semicolon > 5) builder.logoMimeType(logoValue.substring(5, semicolon));
                    int comma = logoValue.indexOf(',');
                    if (comma >= 0) logoValue = logoValue.substring(comma + 1);
                }
                builder.logo(logoValue);
            }
        }

        Map<String, Object> linkObj = getMap(pos, "link");
        if (linkObj != null && Boolean.TRUE.equals(linkObj.get("enabled"))) {
            builder.linkUrl((String) linkObj.get("url"));
            builder.linkText((String) linkObj.get("text"));
        }

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
        return Boolean.TRUE.equals(map.get(key));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return (val instanceof Map) ? (Map<String, Object>) val : null;
    }
}
