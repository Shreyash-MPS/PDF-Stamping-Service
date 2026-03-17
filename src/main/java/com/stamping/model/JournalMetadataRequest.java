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
public class JournalMetadataRequest {

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

    // Inline config — if provided, backend uses this instead of loading from disk
    private Map<String, DynamicStampRequest.Configuration> positions;
}
