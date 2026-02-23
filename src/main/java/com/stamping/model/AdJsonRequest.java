package com.stamping.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request model for JSON-based ad stamping.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdJsonRequest {
    private String inputPath;
    private String outputPath; // Optional
    private String adType;
    private String adJsonUrl;
}
