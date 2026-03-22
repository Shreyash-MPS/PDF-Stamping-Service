package com.stamping.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JournalMetadataRequest {

    // Environment mode — when set to "demo", any null/blank field gets a demo default value
    private String env;

    // Core details
    private String pdfFilePath;      // Absolute path to the source PDF file
    private String outputPath;       // Optional: Path to save the stamped PDF
    private String publisherId;      // Used to load saved config (if no inline config)
    private String jcode;            // Used to load saved config (if no inline config)

    // Metadata fields — Drupal sends only the values for fields marked true in the saved config
    private String articleTitle;
    private String authors;
    private String doiValue;         // e.g. "10.3174/ajnr.A8959"
    private String articleCopyright;
    private String articleIssn;
    private String articleId;
    private String downloadedBy;     // Username of the person downloading (populated by Drupal)

    // Inline config — if provided, backend uses this instead of loading from disk
    private Map<String, DynamicStampRequest.Configuration> positions;

    /**
     * Returns true if this request is running in demo mode.
     */
    @JsonIgnore
    public boolean isDemoMode() {
        return "demo".equalsIgnoreCase(env);
    }

    /**
     * Fills in demo default values for any metadata field that is null or blank.
     * Only applies when env = "demo". Call this before processing the request.
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
