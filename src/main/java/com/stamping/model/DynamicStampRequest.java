package com.stamping.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DynamicStampRequest {

    private String publisherId;
    private String jcode;
    private String strategy;

    /** Single-position config (legacy, used by /stamp/dynamic) */
    private Configuration configuration;

    /** Multi-position config map: position name -> config (used by /config/save) */
    private Map<String, Configuration> positions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Configuration {
        private String alignment;
        private Boolean includeCurrentUser;
        private Boolean includeArticleTitle;
        private Boolean includeAuthors;
        private Boolean includeDoi;
        private Boolean includeDate;
        private Boolean includeCopyright;
        private Boolean includeIssn;
        private Boolean includeArticleId;

        // NEW_PAGE position: where to insert the page ("front" or "back")
        private String pagePosition;

        // Metadata fields (populated by Drupal at stamp time for NEW_PAGE position)
        private String articleTitle;
        private String authors;
        private String doiValue;

        // Template configuration
        private String templateName;

        // Flattened configuration properties
        private String logo; // base64
        private String logoMimeType;
        private String text;
        private String html;
        private String doi;
        private Boolean adsEnabled; // When true, BAM ads URL is built at runtime from publisherId + jcode
        private String legacyDomain; // Legacy domain for resolving relative ad paths (e.g. "hwmaint.genome.cshlp.org")
        
        // Link fields
        private String linkUrl;
        private String linkText;
    }
}
