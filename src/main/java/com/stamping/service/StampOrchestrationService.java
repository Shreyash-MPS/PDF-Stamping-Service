package com.stamping.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.stamping.config.StampingProperties;
import com.stamping.exception.StampingException;
import com.stamping.model.DynamicStampRequest;
import com.stamping.model.JournalMetadataRequest;
import com.stamping.model.StampPosition;
import com.stamping.model.StampRequest;
import com.stamping.model.StampType;
import com.stamping.model.ad.AdResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates the full journal-metadata stamping pipeline.
 * Extracted from StampController to keep the controller thin.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StampOrchestrationService {

    private final StampService stampService;
    private final AdStampService adStampService;
    private final MetadataFrontPageService metadataFrontPageService;
    private final AdFetchService adFetchService;
    private final TemplateService templateService;
    private final PdfFontExtractor pdfFontExtractor;
    private final InputSanitizer inputSanitizer;
    private final StampingProperties properties;

    /**
     * Result of the stamping pipeline — the stamped PDF bytes and a suggested filename.
     */
    public record StampResult(byte[] pdfBytes, String filename) {}

    /**
     * Runs the full stamping pipeline: validate → read PDF → extract font → process positions → return result.
     */
    public StampResult processJournalMetadata(JournalMetadataRequest request) {
        long startTime = System.currentTimeMillis();

        // 1. Validate inputs
        validateRequest(request);

        log.info("==========================================================");
        log.info("  STAMP REQUEST  pubId={}  jcode={}  env={}",
                request.getPublisherId(), request.getJcode(),
                request.getEnv() != null ? request.getEnv() : "default");

        // 2. Read PDF
        byte[] currentPdfBytes = readPdf(request.getPdfFilePath());

        // 3. Extract page size and font
        Rectangle pageSize = extractPageSize(currentPdfBytes);
        PdfFontExtractor.FontInfo pdfFont = pdfFontExtractor.extractPrimaryFont(currentPdfBytes);
        String fontFamily = buildFontFamily(pdfFont);
        logFontInfo(pdfFont, fontFamily);

        log.info("----------------------------------------------------------");

        // 4. Process each position
        Map<String, DynamicStampRequest.Configuration> positions = request.getPositions();
        log.info("  Positions: {}", positions.keySet());

        int prependedPages = 0;
        int appendedPages = 0;

        for (var entry : positions.entrySet()) {
            String posStr = entry.getKey();
            DynamicStampRequest.Configuration c = entry.getValue();
            if (c == null) continue;

            if ("NEW_PAGE".equalsIgnoreCase(posStr)) {
                NewPageResult result = processNewPage(c, request, pdfFont, fontFamily, pageSize, currentPdfBytes);
                currentPdfBytes = result.pdfBytes;
                prependedPages += result.prepended;
                appendedPages += result.appended;
            } else {
                currentPdfBytes = processOverlayPosition(posStr, c, request, pdfFont, fontFamily,
                        pageSize, currentPdfBytes, prependedPages, appendedPages);
            }
        }

        // 5. Save to disk if outputPath provided
        if (request.getOutputPath() != null && !request.getOutputPath().isBlank()) {
            saveOutput(request.getOutputPath(), currentPdfBytes);
        }

        String outputFilename = buildOutputFilename(new File(request.getPdfFilePath()).getName());
        long elapsed = System.currentTimeMillis() - startTime;
        log.info("----------------------------------------------------------");
        log.info("  DONE  {}ms  output={}  size={} KB", elapsed, outputFilename, currentPdfBytes.length / 1024);
        log.info("==========================================================");

        return new StampResult(currentPdfBytes, outputFilename);
    }

    // ─── Validation ─────────────────────────────────────────────────────

    private void validateRequest(JournalMetadataRequest request) {
        inputSanitizer.validateFilePath(request.getPdfFilePath());
        inputSanitizer.validateIdentifier(request.getPublisherId(), "publisherId");
        inputSanitizer.validateIdentifier(request.getJcode(), "jcode");

        File inputFile = new File(request.getPdfFilePath());
        if (!inputFile.exists() || !inputFile.canRead()) {
            throw new StampingException("Cannot read input file: " + request.getPdfFilePath());
        }
        if (request.getPositions() == null || request.getPositions().isEmpty()) {
            throw new StampingException("positions map is required in the request JSON");
        }
        if (request.getOutputPath() != null && !request.getOutputPath().isBlank()) {
            inputSanitizer.validateFilePath(request.getOutputPath());
        }
    }

    // ─── PDF I/O ────────────────────────────────────────────────────────

    private byte[] readPdf(String path) {
        try {
            log.info("  File: {}", path);
            return Files.readAllBytes(Paths.get(path));
        } catch (Exception e) {
            throw new StampingException("Failed to read PDF file: " + e.getMessage(), e);
        }
    }

    private Rectangle extractPageSize(byte[] pdfBytes) {
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfBytes)))) {
            Rectangle ps = doc.getPage(1).getPageSize();
            log.info("  PDF: {} pages, page size {}x{}pt",
                    doc.getNumberOfPages(), Math.round(ps.getWidth()), Math.round(ps.getHeight()));
            return ps;
        } catch (Exception e) {
            throw new StampingException("Failed to read PDF page size: " + e.getMessage(), e);
        }
    }

    private void saveOutput(String outputPath, byte[] pdfBytes) {
        try {
            File outputFile = new File(outputPath);
            File outputDir = outputFile.getParentFile();
            if (outputDir != null && !outputDir.exists()) {
                outputDir.mkdirs();
            }
            Files.write(Paths.get(outputPath), pdfBytes);
            log.info("  Saved to: {}", outputPath);
        } catch (Exception e) {
            throw new StampingException("Failed to save output file: " + e.getMessage(), e);
        }
    }

    // ─── Font helpers ───────────────────────────────────────────────────

    private String buildFontFamily(PdfFontExtractor.FontInfo pdfFont) {
        return pdfFont != null
                ? "'" + pdfFont.getFontFamily() + "', Verdana, Arial, Helvetica, sans-serif"
                : "Verdana, Arial, Helvetica, sans-serif";
    }

    private void logFontInfo(PdfFontExtractor.FontInfo pdfFont, String fontFamily) {
        if (pdfFont != null) {
            log.info("  Font: family='{}' baseFont='{}' format={} embedded={} subset={} {}",
                    pdfFont.getFontFamily(), pdfFont.getBaseFontName(), pdfFont.getFormat(),
                    pdfFont.isEmbedded(), pdfFont.isSubset(),
                    pdfFont.isUsableForStamping()
                            ? "(@font-face will be injected, " + (pdfFont.getFontBytes().length / 1024) + " KB)"
                            : pdfFont.isEmbedded() ? "(subset -- font-family name only)" : "(name-only)");
        } else {
            log.info("  Font: none extracted -- using fallback Verdana/Arial/Helvetica");
        }
        log.info("  CSS font-family: {}", fontFamily);
    }

    // ─── NEW_PAGE processing ────────────────────────────────────────────

    private record NewPageResult(byte[] pdfBytes, int prepended, int appended) {}

    private NewPageResult processNewPage(DynamicStampRequest.Configuration c, JournalMetadataRequest request,
                                         PdfFontExtractor.FontInfo pdfFont, String fontFamily,
                                         Rectangle pageSize, byte[] currentPdfBytes) {
        log.info("  [NEW_PAGE] template={}  pagePosition={}  ads={}",
                c.getTemplateName() != null ? c.getTemplateName() : "default_metadata",
                c.getPagePosition() != null ? c.getPagePosition() : "front",
                Boolean.TRUE.equals(c.getAdsEnabled()) ? "enabled" : "disabled");

        String html = "";

        // 1. Resolve template
        if (c.getTemplateName() != null && !c.getTemplateName().isBlank()) {
            html += templateService.renderTemplate(c, request, pdfFont);
        } else if (c.getHtml() == null || c.getHtml().isBlank()) {
            String orig = c.getTemplateName();
            c.setTemplateName("default_metadata");
            html += templateService.renderTemplate(c, request, pdfFont);
            c.setTemplateName(orig);
        }

        // 2. Append custom HTML if provided
        if (c.getHtml() != null && !c.getHtml().isBlank()) {
            String customHtml = inputSanitizer.sanitizeHtml(c.getHtml());
            if (!html.isEmpty()) {
                html = html.contains("</body>")
                        ? html.replace("</body>", "<div>" + customHtml + "</div></body>")
                        : html + "<div>" + customHtml + "</div>";
            } else {
                html = customHtml.contains("<html") ? customHtml
                        : "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/></head>"
                        + "<body style=\"margin: 50px; font-family: " + fontFamily + "; color: #000;\">"
                        + customHtml + "</body></html>";
            }
        }

        // 3. Render and prepend/append
        byte[] htmlPageBytes = metadataFrontPageService.renderHtmlToPdf(html, pageSize);
        boolean appendToBack = "back".equalsIgnoreCase(c.getPagePosition());
        int prepended = 0, appended = 0;

        if (appendToBack) {
            currentPdfBytes = metadataFrontPageService.appendPdf(currentPdfBytes, htmlPageBytes);
            appended = 1;
            log.info("  [NEW_PAGE] Appended to back");
        } else {
            currentPdfBytes = metadataFrontPageService.prependPdf(currentPdfBytes, htmlPageBytes);
            prepended = 1;
            log.info("  [NEW_PAGE] Prepended to front");
        }

        return new NewPageResult(currentPdfBytes, prepended, appended);
    }

    // ─── Overlay position processing ────────────────────────────────────

    private byte[] processOverlayPosition(String posStr, DynamicStampRequest.Configuration c,
                                           JournalMetadataRequest request, PdfFontExtractor.FontInfo pdfFont,
                                           String fontFamily, Rectangle pageSize, byte[] currentPdfBytes,
                                           int prependedPages, int appendedPages) {
        log.info("  [{}] ads={}  text={}  html={}", posStr,
                Boolean.TRUE.equals(c.getAdsEnabled()) ? "enabled" : "disabled",
                c.getText() != null && !c.getText().isBlank() ? "yes" : "no",
                c.getHtml() != null && !c.getHtml().isBlank() ? "yes" : "no");

        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<div style=\"text-align: center;\">");

        appendLogo(htmlBuilder, c);
        appendText(htmlBuilder, c);
        appendRawHtml(htmlBuilder, c);
        appendDoi(htmlBuilder, c);
        appendDate(htmlBuilder, c);
        appendMetadataFields(htmlBuilder, c, request);
        appendAds(htmlBuilder, posStr, c, request);

        htmlBuilder.append("</div>");

        // Position-specific CSS and rotation
        float rotation = 0f;
        float sWidth = pageSize.getWidth();
        float sHeight = pageSize.getHeight();
        String cssPosition;

        switch (posStr) {
            case "HEADER" -> cssPosition = "position: absolute; top: 0; left: 0; right: 0; text-align: center;";
            case "FOOTER" -> cssPosition = "position: absolute; bottom: 0; left: 0; right: 0; text-align: center;";
            case "LEFT_MARGIN" -> {
                rotation = 90f;
                sWidth = pageSize.getHeight();
                sHeight = pageSize.getWidth();
                cssPosition = "position: absolute; top: 0; left: 0; right: 0; text-align: center;";
            }
            case "RIGHT_MARGIN" -> {
                rotation = 270f;
                sWidth = pageSize.getHeight();
                sHeight = pageSize.getWidth();
                cssPosition = "position: absolute; top: 0; left: 0; right: 0; text-align: center;";
            }
            case "CENTER" -> cssPosition = "position: absolute; top: 45%; left: 0; right: 0; text-align: center;";
            default -> cssPosition = "position: absolute; left: 0; right: 0;";
        }

        String fontFaceCss = buildFontFaceCss(pdfFont);
        String overlayHtml = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><style>"
                + fontFaceCss
                + "body{margin:0;padding:0;font-family:" + fontFamily + ";}</style></head><body>"
                + "<div style=\"" + cssPosition + "\">"
                + "<div style=\"display: inline-block; padding: 2px 4px;\">"
                + htmlBuilder
                + "</div></div></body></html>";

        String targetPages = computeTargetPages(currentPdfBytes, prependedPages, appendedPages);
        log.info("  [{}] Stamping pages {}", posStr, targetPages);

        StampRequest htmlReq = StampRequest.builder()
                .stampType(StampType.HTML)
                .position(StampPosition.CENTER)
                .opacity(1.0f)
                .rotation(rotation)
                .scale(1.0f)
                .pages(targetPages)
                .stampWidth(sWidth)
                .stampHeight(sHeight)
                .build();

        return stampService.applyStamp(currentPdfBytes, htmlReq, overlayHtml.getBytes(StandardCharsets.UTF_8));
    }

    // ─── HTML fragment builders ─────────────────────────────────────────

    private void appendLogo(StringBuilder sb, DynamicStampRequest.Configuration c) {
        if (c.getLogo() != null && !c.getLogo().isBlank()) {
            String mimeType = c.getLogoMimeType() != null ? c.getLogoMimeType() : "image/png";
            sb.append("<img src=\"data:").append(mimeType).append(";base64,").append(c.getLogo())
                    .append("\" style=\"max-width: 200px; display: block; margin: 0 auto; margin-bottom: 8px;\" />");
        }
    }

    private void appendText(StringBuilder sb, DynamicStampRequest.Configuration c) {
        if (c.getText() != null && !c.getText().isBlank()) {
            String safeText = inputSanitizer.sanitizeHtml(c.getText().replace("\n", "<br/>"));
            sb.append("<p style=\"font-size: 14px; margin: 4px 0; font-weight: bold;\">")
                    .append(safeText).append("</p>");
        }
    }

    private void appendRawHtml(StringBuilder sb, DynamicStampRequest.Configuration c) {
        if (c.getHtml() != null && !c.getHtml().isBlank()) {
            String sanitized = inputSanitizer.sanitizeHtml(c.getHtml());
            sb.append("<div style=\"margin: 8px 0;\">").append(sanitized).append("</div>");
        }
    }

    private void appendDoi(StringBuilder sb, DynamicStampRequest.Configuration c) {
        if (c.getDoi() != null && !c.getDoi().isBlank()) {
            String doiUrl = c.getDoi().startsWith("http") ? c.getDoi() : "https://doi.org/" + c.getDoi();
            sb.append("<p style=\"margin: 4px 0; font-size: 12px; color: black;\">doi: ")
                    .append("<a href=\"").append(doiUrl)
                    .append("\" style=\"color: blue; text-decoration: none;\">")
                    .append(doiUrl).append("</a></p>");
        }
    }

    private void appendDate(StringBuilder sb, DynamicStampRequest.Configuration c) {
        if (Boolean.TRUE.equals(c.getIncludeDate())) {
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
            sb.append("<p style=\"margin: 4px 0; font-size: 12px; color: #555;\">Date Generated: ")
                    .append(dateStr).append("</p>");
        }
    }

    private void appendMetadataFields(StringBuilder sb, DynamicStampRequest.Configuration c,
                                       JournalMetadataRequest request) {
        if (Boolean.TRUE.equals(c.getIncludeArticleTitle()) && isNotBlank(request.getArticleTitle())) {
            sb.append("<p style=\"margin: 4px 0; font-size: 12px; font-weight: bold;\">")
                    .append(request.getArticleTitle()).append("</p>");
        }
        if (Boolean.TRUE.equals(c.getIncludeAuthors()) && isNotBlank(request.getAuthors())) {
            sb.append("<p style=\"margin: 4px 0; font-size: 11px; color: #333;\">")
                    .append(request.getAuthors()).append("</p>");
        }
        if (Boolean.TRUE.equals(c.getIncludeDoi()) && isNotBlank(request.getDoiValue())) {
            String doiUrl = request.getDoiValue().startsWith("http") ? request.getDoiValue()
                    : "https://doi.org/" + request.getDoiValue();
            sb.append("<p style=\"margin: 4px 0; font-size: 11px;\">")
                    .append("<a href=\"").append(doiUrl)
                    .append("\" style=\"color: blue; text-decoration: none;\">")
                    .append(doiUrl).append("</a></p>");
        }
        if (Boolean.TRUE.equals(c.getIncludeCopyright()) && isNotBlank(request.getArticleCopyright())) {
            sb.append("<p style=\"margin: 4px 0; font-size: 10px; color: #555;\">")
                    .append(request.getArticleCopyright()).append("</p>");
        }
        if (Boolean.TRUE.equals(c.getIncludeIssn()) && isNotBlank(request.getArticleIssn())) {
            sb.append("<p style=\"margin: 4px 0; font-size: 10px; color: #555;\">ISSN: ")
                    .append(request.getArticleIssn()).append("</p>");
        }
        if (Boolean.TRUE.equals(c.getIncludeArticleId()) && isNotBlank(request.getArticleId())) {
            sb.append("<p style=\"margin: 4px 0; font-size: 10px; color: #555;\">Article ID: ")
                    .append(request.getArticleId()).append("</p>");
        }
        if (Boolean.TRUE.equals(c.getIncludeCurrentUser()) && isNotBlank(request.getDownloadedBy())) {
            sb.append("<p style=\"margin: 4px 0; font-size: 10px; color: #555;\">Downloaded By: ")
                    .append(request.getDownloadedBy()).append("</p>");
        }
    }

    private void appendAds(StringBuilder sb, String posStr, DynamicStampRequest.Configuration c,
                            JournalMetadataRequest request) {
        if (!Boolean.TRUE.equals(c.getAdsEnabled())) return;

        String adUrl = buildAdUrl(request.getPublisherId(), request.getJcode());
        AdResponse adResponse = adFetchService.fetchAds(adUrl);
        String extractedAdHtml = extractHeaderAdHtml(adResponse);

        if (extractedAdHtml != null) {
            log.info("  [{}] Ad injected ({} chars)", posStr, extractedAdHtml.length());
            extractedAdHtml = adStampService.processHtmlContent(extractedAdHtml, c.getLegacyDomain());
            sb.append("<div style=\"margin-top: 10px;\">").append(extractedAdHtml).append("</div>");
        } else {
            log.warn("  [{}] No 'header' ad found in response", posStr);
        }
    }

    // ─── Shared utilities ───────────────────────────────────────────────

    public String buildAdUrl(String publisherId, String jcode) {
        return properties.getAds().getBaseUrl()
                + "?publisherId=" + publisherId
                + "&jcode=" + jcode
                + "&sectionPath=" + properties.getAds().getSectionPath();
    }

    private String extractHeaderAdHtml(AdResponse adResponse) {
        if (adResponse == null || adResponse.getSection() == null) return null;
        for (var section : adResponse.getSection()) {
            if (section.getAdLocation() == null) continue;
            for (var location : section.getAdLocation()) {
                if ("header".equalsIgnoreCase(location.getPositionName()) && location.getAdData() != null) {
                    for (var ad : location.getAdData()) {
                        if (ad.getAdHtml() != null && !ad.getAdHtml().isEmpty()) {
                            return ad.getAdHtml();
                        }
                    }
                }
            }
        }
        return null;
    }

    private String buildFontFaceCss(PdfFontExtractor.FontInfo pdfFont) {
        if (pdfFont == null || !pdfFont.isUsableForStamping()) return "";
        String mimeType = "truetype".equals(pdfFont.getFormat()) ? "font/ttf" : "font/otf";
        String base64 = Base64.getEncoder().encodeToString(pdfFont.getFontBytes());
        return "@font-face{font-family:'" + pdfFont.getFontFamily()
                + "';src:url('data:" + mimeType + ";base64," + base64 + "');}";
    }

    private String computeTargetPages(byte[] pdfBytes, int prependedPages, int appendedPages) {
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfBytes)))) {
            int total = doc.getNumberOfPages();
            int first = prependedPages + 1;
            int last = total - appendedPages;
            if (first > last || (first == 1 && last == total)) return "ALL";
            if (first == last) return String.valueOf(first);
            return first + "-" + last;
        } catch (Exception e) {
            return "ALL";
        }
    }

    private String buildOutputFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) return "stamped.pdf";
        if (originalFilename.toLowerCase().endsWith(".pdf")) {
            return originalFilename.substring(0, originalFilename.length() - 4) + "_stamped.pdf";
        }
        return originalFilename + "_stamped.pdf";
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
