package com.stamping.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetadataFrontPageRequest {
    private String inputPath;
    private String outputPath;

    // Left column top
    private String logoUrl; // Optional image URL for logo
    private String logoText; // Fallback text, e.g. "AJNR"

    // Right column top
    private String articleTitle; // Journal name / Article Title / Consensus Statement

    // Right column middle
    private String authors; // Author names string

    // Left column middle
    private boolean addCurrentDate;

    // Right column bottom
    private String citationText; // e.g. "AJNR Am J Neuroradiol 2026, 47 (2) 281-288"
    private boolean addDoi;
    private String doi; // e.g. "10.3174/ajnr.A8959"
    private String additionalLink; // e.g. "http://www.ajnr.org/content/47/2/281"
}
