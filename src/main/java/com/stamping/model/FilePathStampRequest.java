package com.stamping.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request model for JSON-based stamping with file paths.
 * Allows specifying input/output file paths instead of uploading files.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilePathStampRequest {

    // File paths
    private String inputFilePath;
    private String outputFilePath;
    private String stampFilePath; // For IMAGE and HTML types

    // Stamp configuration (same as StampRequest)
    private StampType stampType;
    private StampPosition position;
    private Float x;
    private Float y;
    @Builder.Default
    private float opacity = 1.0f;
    @Builder.Default
    private float rotation = 0f;
    @Builder.Default
    private float scale = 1.0f;
    @Builder.Default
    private String pages = "ALL";

    // Text stamp properties
    private String text;
    @Builder.Default
    private float fontSize = 14f;
    @Builder.Default
    private String fontColor = "#000000";

    // Image/HTML stamp size
    private Float stampWidth;
    private Float stampHeight;
}
