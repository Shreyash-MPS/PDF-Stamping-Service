package com.stamping.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JournalMetadataRequest {

    // Environment mode — when set to "demo", any null/blank field gets a demo default value
    private String env;

    // Core details
    private String pdfFilePath;
    private String outputPath;

    @NotBlank(message = "publisherId is required")
    private String publisherId;

    @NotBlank(message = "jcode is required")
    private String jcode;

    // Metadata fields
    private String articleTitle;
    private String authors;
    private String doiValue;
    private String articleCopyright;
    private String articleIssn;
    private String articleId;
    private String downloadedBy;

    // Inline config
    private Map<String, DynamicStampRequest.Configuration> positions;

    @JsonIgnore
    public boolean isDemoMode() {
        return "demo".equalsIgnoreCase(env);
    }

    /**
     * Fills in demo default values for any metadata field that is null or blank.
     * Only applies when env = "demo".
     */
    public void applyDemoDefaults() {
        if (!isDemoMode()) return;

        String pub = (publisherId != null && !publisherId.isBlank()) ? publisherId : "demoPub";
        String jc  = (jcode != null && !jcode.isBlank()) ? jcode : "demoJcode";

        if (publisherId == null || publisherId.isBlank())          publisherId = pub;
        if (jcode == null || jcode.isBlank())                      jcode = jc;
        if (articleTitle == null || articleTitle.isBlank())         articleTitle = "Sample Article Title for Testing";
        if (authors == null || authors.isBlank())                  authors = "Author One, Author Two, Author Three";
        if (doiValue == null || doiValue.isBlank())                doiValue = "10.xxxx/sample.doi.2026";
        if (articleCopyright == null || articleCopyright.isBlank()) articleCopyright = "\u00a9 2026 " + pub + ". All rights reserved.";
        if (articleIssn == null || articleIssn.isBlank())           articleIssn = "1234-5678";
        if (articleId == null || articleId.isBlank())               articleId = "ART-" + jc.toUpperCase() + "-001";
        if (downloadedBy == null || downloadedBy.isBlank())        downloadedBy = "demo.user@example.com";
    }
}
