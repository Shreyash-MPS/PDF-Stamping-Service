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
    public static class Configuration {
        private String alignment;
        private boolean addNewPage;
        private boolean includeCurrentUser;
        private boolean includeArticleTitle;
        private boolean includeAuthors;
        private boolean includeDoi;
        private boolean includeDate;

        // Metadata fields (populated by Drupal at stamp time for CUSTOM position)
        private String articleTitle;
        private String authors;
        private String doiValue;

        private LogoConfig logo;
        private TextConfig text;
        private HtmlConfig html;
        private DoiConfig doi;
        private DateConfig date;
        private AdConfig ad;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class LogoConfig {
            private String base64;
            private String mimeType;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class TextConfig {
            private String content;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class HtmlConfig {
            private String content;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DoiConfig {
            private String value;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DateConfig {
            // Placeholder for future date format options
            private boolean enabled;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AdConfig {
            private String link;
        }
    }
}
