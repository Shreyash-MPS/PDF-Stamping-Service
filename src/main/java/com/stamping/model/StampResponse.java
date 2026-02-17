package com.stamping.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response model for file path stamping operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StampResponse {
    private boolean success;
    private String message;
    private String outputFilePath;
    private long fileSizeBytes;
}
