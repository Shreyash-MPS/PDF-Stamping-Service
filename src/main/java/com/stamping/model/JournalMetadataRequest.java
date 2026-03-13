package com.stamping.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JournalMetadataRequest {

    // Core details
    private String pdfFilePath;      // Absolute path to the source PDF file
    private String outputPath;       // Optional: Path to save the stamped PDF
    private String publisherId;      // Used to load saved config
    private String jcode;            // Used to load saved config

    // Metadata fields — Drupal sends only the values for fields marked true in the saved config
    private String articleTitle;
    private String authors;
    private String doiValue;         // e.g. "10.3174/ajnr.A8959"
}
