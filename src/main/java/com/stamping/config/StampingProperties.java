package com.stamping.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Typed configuration properties for the stamping service.
 * All values are bound from the "stamping" prefix in application.yml.
 *
 * <p>Example application.yml block:
 * <pre>
 * stamping:
 *   temp-dir: temp
 *   config-dir: configs
 *   archive-dir: archive_configs
 *   allowed-pdf-base-path: /var/data/pdfs
 *   ads:
 *     base-url: https://bam-ads-presenter.highwire.org/api/ads
 *   cors:
 *     allowed-origins: https://myapp.example.com
 *   pdf-download:
 *     connect-timeout: 5000
 *     read-timeout: 30000
 *     max-file-size: 52428800
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "stamping")
public class StampingProperties {

    /** Directory where downloaded and demo temp PDFs are written. Cleaned up by PdfDownloadService scheduler. */
    private String tempDir;
    private int defaultFontSize = 12;
    private String defaultFontColor = "#000000";
    private double defaultOpacity = 0.8;
    /** Directory where active publisher/journal stamping configs are stored. */
    private String configDir = "configs";
    /** Directory where archived (soft-deleted) configs are moved to. */
    private String archiveDir = "archive_configs";
    /**
     * Optional base path restriction for local PDF file access.
     * When set, any pdfFilePath outside this directory is rejected.
     * Leave blank to allow any accessible path.
     */
    private String allowedPdfBasePath = "";

    private Ads ads = new Ads();
    private Cors cors = new Cors();
    private PdfDownload pdfDownload = new PdfDownload();

    @Data
    public static class Ads {
        private String baseUrl = "https://bam-ads-presenter.highwire.org/api/ads";
        private String sectionPath = "xpdf";
        private int connectTimeout = 5000;
        private int readTimeout = 10000;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:5173", "http://localhost:3000");
    }

    @Data
    public static class PdfDownload {
        /** Connection timeout in milliseconds for remote PDF downloads */
        private int connectTimeout = 5000;
        /** Read timeout in milliseconds for remote PDF downloads */
        private int readTimeout = 30000;
        /** Maximum allowed PDF file size in bytes (default 50 MB) */
        private long maxFileSize = 52428800L;
    }
}
