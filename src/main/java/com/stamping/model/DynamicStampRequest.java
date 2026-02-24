package com.stamping.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicStampRequest {

    private String publisherId;
    private String jcode;
    private String strategy;
    private Configuration configuration;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Configuration {
        private String position;
        private String alignment;
        private boolean addLogo;
        private boolean addText;
        private String textContent;
        private boolean addHtml;
        private String htmlContent;
        private boolean addDoi;
        private String doiValue;
        private boolean addDate;

        @JsonProperty("isAd")
        private boolean isAd;
        private String adLink;
        private String optionalText;
    }
}
