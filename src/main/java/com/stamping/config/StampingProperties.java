package com.stamping.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "stamping")
public class StampingProperties {

    private String tempDir;
    private int defaultFontSize = 12;
    private String defaultFontColor = "#000000";
    private double defaultOpacity = 0.8;
    private String configDir = "configs";
    private String archiveDir = "archive_configs";
    private String testRequestsDir = "test_requests";
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
