package com.stamping.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.stamping.config.StampingProperties;
import com.stamping.exception.StampingException;

import lombok.extern.slf4j.Slf4j;

/**
 * Downloads a remote PDF to a local temp file for processing.
 * Includes scheduled cleanup of stale temp files to prevent disk bloat.
 */
@Slf4j
@Service
public class PdfDownloadService {

    private static final String TEMP_FILE_PREFIX = "pdf_download_";
    private static final long STALE_FILE_AGE_MINUTES = 30;

    private final StampingProperties properties;
    private final HttpClient httpClient;
    private final File tempDir;

    public PdfDownloadService(StampingProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getPdfDownload().getConnectTimeout()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        // Use the configured temp dir, creating it if needed
        this.tempDir = new File(properties.getTempDir());
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
    }

    /**
     * Downloads the PDF at the given URL to a temp file.
     * The caller is responsible for deleting the returned file after use.
     */
    public File download(String url) {
        log.info("  Downloading PDF from URL: {}", url);

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(properties.getPdfDownload().getReadTimeout()))
                    .GET()
                    .build();
        } catch (IllegalArgumentException e) {
            throw new StampingException("Invalid PDF URL: " + e.getMessage(), e);
        }

        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StampingException("Failed to connect to PDF URL: " + e.getMessage(), e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new StampingException(
                    "Remote server returned HTTP " + response.statusCode() + " for URL: " + url);
        }

        try {
            File tempFile = File.createTempFile(TEMP_FILE_PREFIX, ".pdf", tempDir);
            streamToFile(response.body(), tempFile);
            log.info("  Downloaded {} KB to {}", tempFile.length() / 1024, tempFile.getAbsolutePath());
            return tempFile;
        } catch (IOException e) {
            throw new StampingException("Failed to write downloaded PDF to temp file: " + e.getMessage(), e);
        }
    }

    /**
     * Scheduled cleanup — runs every 15 minutes.
     * Deletes any pdf_download_*.pdf files older than 30 minutes.
     */
    @Scheduled(fixedRate = 15 * 60 * 1000, initialDelay = 5 * 60 * 1000)
    public void cleanupStaleTempFiles() {
        if (!tempDir.exists() || !tempDir.isDirectory()) return;

        File[] staleFiles = tempDir.listFiles((dir, name) ->
                name.startsWith(TEMP_FILE_PREFIX) && name.endsWith(".pdf"));

        if (staleFiles == null || staleFiles.length == 0) return;

        Instant cutoff = Instant.now().minus(Duration.ofMinutes(STALE_FILE_AGE_MINUTES));
        int deleted = 0;

        for (File file : staleFiles) {
            if (Instant.ofEpochMilli(file.lastModified()).isBefore(cutoff)) {
                if (file.delete()) {
                    deleted++;
                }
            }
        }

        if (deleted > 0) {
            log.info("Temp cleanup: deleted {} stale PDF download(s) from {}", deleted, tempDir.getAbsolutePath());
        }
    }

    private void streamToFile(InputStream in, File dest) throws IOException {
        long maxBytes = properties.getPdfDownload().getMaxFileSize();
        try (OutputStream out = Files.newOutputStream(dest.toPath())) {
            byte[] buf = new byte[8192];
            long total = 0;
            int read;
            while ((read = in.read(buf)) != -1) {
                total += read;
                if (total > maxBytes) {
                    dest.delete();
                    throw new StampingException(
                            "Downloaded PDF exceeds maximum allowed size of " + (maxBytes / 1024 / 1024) + " MB");
                }
                out.write(buf, 0, read);
            }
        }
    }
}
