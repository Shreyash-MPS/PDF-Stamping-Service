package com.stamping.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StampRequest {

    private StampType stampType;

    @Builder.Default
    private StampPosition position = StampPosition.CENTER;

    /** Custom X coordinate (used when position = CUSTOM) */
    private Float x;

    /** Custom Y coordinate (used when position = CUSTOM) */
    private Float y;

    /** Opacity from 0.0 (fully transparent) to 1.0 (fully opaque) */
    @Builder.Default
    private float opacity = 1.0f;

    /** Rotation angle in degrees */
    @Builder.Default
    private float rotation = 0f;

    /** Scale factor for image/HTML stamps */
    @Builder.Default
    private float scale = 1.0f;

    /**
     * Page selection expression.
     * Supported: "ALL", "FIRST", "LAST", or comma-separated with ranges e.g. "1,3,5-7"
     */
    @Builder.Default
    private String pages = "ALL";

    // --- Text-specific fields ---

    /** The text content to stamp (only for TEXT type) */
    private String text;

    /** Font size in points */
    @Builder.Default
    private float fontSize = 14f;

    /** Font color as hex string, e.g. "#FF0000" */
    @Builder.Default
    private String fontColor = "#000000";

    /** Width of the stamp area (for HTML stamps or text wrapping) */
    private Float stampWidth;

    /** Height of the stamp area */
    private Float stampHeight;
}
