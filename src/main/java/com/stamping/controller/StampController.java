package com.stamping.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stamping.exception.StampingException;
import com.stamping.model.DynamicStampRequest;
import com.stamping.model.StampPosition;
import com.stamping.model.StampRequest;
import com.stamping.model.StampResponse;
import com.stamping.model.StampType;
import com.stamping.model.ad.AdResponse;
import com.stamping.service.AdFetchService;
import com.stamping.service.AdStampService;
import com.stamping.service.DemoConfigGeneratorService;
import com.stamping.service.DemoStampService;
import com.stamping.service.MetadataFrontPageService;
import com.stamping.service.PdfFontExtractor;
import com.stamping.service.StampService;
import com.stamping.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StampController {

    private final StampService stampService;
    private final AdStampService adStampService;
    private final MetadataFrontPageService metadataFrontPageService;
    private final AdFetchService adFetchService;
    private final TemplateService templateService;
    private final ObjectMapper objectMapper;

    // DEV-ONLY: remove before production (along with DevController + DemoConfigGeneratorService)
    private final DemoConfigGeneratorService demoConfigGeneratorService;
    private final DemoStampService demoStampService;
    private final PdfFontExtractor pdfFontExtractor;

    /**
     * Process a journal metadata request.
     * The request JSON must contain all stamping configuration inline (positions map),
     * along with pdfFilePath, publisherId, jcode, and metadata values.
     *
     * @param request the JSON request containing file path, positions config, and metadata
     * @return the stamped PDF file (or saves directly to disk if outputPath is provided)
     */
    @PostMapping(value = "/stamp/journal-metadata", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> processJournalMetadata(@RequestBody com.stamping.model.JournalMetadataRequest request) {
        java.io.File demoTempFile = null;
        try {
            request.applyDemoDefaults();

            // In demo mode, auto-fill positions from saved config and create a blank PDF if needed
            if (request.isDemoMode()) {
                if (request.getPositions() == null || request.getPositions().isEmpty()) {
                    if (request.getPublisherId() != null && !request.getPublisherId().isBlank()
                            && request.getJcode() != null && !request.getJcode().isBlank()) {
                        com.stamping.model.JournalMetadataRequest demoReq =
                                demoStampService.buildDemoRequest(request.getPublisherId(), request.getJcode());
                        request.setPositions(demoReq.getPositions());
                        log.info("  [DEMO] Auto-loaded positions from saved config for {}/{}",
                                request.getPublisherId(), request.getJcode());
                    }
                }
                if (request.getPdfFilePath() == null || request.getPdfFilePath().isBlank()) {
                    byte[] blankPdf = demoStampService.createBlankPdf();
                    demoTempFile = java.io.File.createTempFile("demo_blank_", ".pdf");
                    demoTempFile.deleteOnExit();
                    Files.write(demoTempFile.toPath(), blankPdf);
                    request.setPdfFilePath(demoTempFile.getAbsolutePath());
                    log.info("  [DEMO] Created blank placeholder PDF: {}", demoTempFile.getAbsolutePath());
                }
            }

            long startTime = System.currentTimeMillis();
            log.info("==========================================================");
            log.info("  STAMP REQUEST  pubId={}  jcode={}  env={}",
                    request.getPublisherId(), request.getJcode(),
                    request.getEnv() != null ? request.getEnv() : "default");
            log.info("  File: {}", request.getPdfFilePath());

            if (request.getPdfFilePath() == null || request.getPdfFilePath().isBlank()) {
                throw new StampingException("pdfFilePath is required");
            }
            if (request.getPublisherId() == null || request.getPublisherId().isBlank()) {
                throw new StampingException("publisherId is required");
            }
            if (request.getJcode() == null || request.getJcode().isBlank()) {
                throw new StampingException("jcode is required");
            }

            File inputFile = new File(request.getPdfFilePath());
            if (!inputFile.exists() || !inputFile.canRead()) {
                throw new StampingException("Cannot read input file: " + request.getPdfFilePath());
            }

            // Positions must be provided inline in the request
            if (request.getPositions() == null || request.getPositions().isEmpty()) {
                throw new StampingException("positions map is required in the request JSON");
            }
            java.util.Map<String, DynamicStampRequest.Configuration> positionsToProcess = request.getPositions();
            log.info("  Positions: {}", positionsToProcess.keySet());

            byte[] currentPdfBytes = Files.readAllBytes(Paths.get(request.getPdfFilePath()));

            // Extract page size once
            com.itextpdf.kernel.geom.Rectangle pageSize;
            try (com.itextpdf.kernel.pdf.PdfDocument tempOriginal = new com.itextpdf.kernel.pdf.PdfDocument(
                    new com.itextpdf.kernel.pdf.PdfReader(new java.io.ByteArrayInputStream(currentPdfBytes)))) {
                pageSize = tempOriginal.getPage(1).getPageSize();
                log.info("  PDF: {} pages, page size {}x{}pt",
                        tempOriginal.getNumberOfPages(),
                        Math.round(pageSize.getWidth()), Math.round(pageSize.getHeight()));
            }

            // Extract primary font from the input PDF for compliance
            PdfFontExtractor.FontInfo pdfFont = pdfFontExtractor.extractPrimaryFont(currentPdfBytes);
            if (pdfFont != null) {
                log.info("  Font: family='{}' baseFont='{}' format={} embedded={} subset={} {}",
                        pdfFont.getFontFamily(),
                        pdfFont.getBaseFontName(),
                        pdfFont.getFormat(),
                        pdfFont.isEmbedded(),
                        pdfFont.isSubset(),
                        pdfFont.isUsableForStamping()
                                ? "(@font-face will be injected, " + (pdfFont.getFontBytes().length / 1024) + " KB)"
                                : pdfFont.isEmbedded()
                                        ? "(subset -- font-family name only, no @font-face)"
                                        : "(name-only, no @font-face)");
            } else {
                log.info("  Font: none extracted -- using fallback Verdana/Arial/Helvetica");
            }
            String fontFamily = pdfFont != null
                    ? "'" + pdfFont.getFontFamily() + "', Verdana, Arial, Helvetica, sans-serif"
                    : "Verdana, Arial, Helvetica, sans-serif";
            log.info("  CSS font-family: {}", fontFamily);

            log.info("----------------------------------------------------------");

            boolean hasAddedNewPage = false;
            int prependedPages = 0;
            int appendedPages = 0;

            for (var entry : positionsToProcess.entrySet()) {
                String posStr = entry.getKey();
                DynamicStampRequest.Configuration c = entry.getValue();
                if (c == null) continue;

                boolean isNewPage = "NEW_PAGE".equalsIgnoreCase(posStr);

                // For NEW_PAGE (cover page) positions — generate a new prepended page
                if (isNewPage) {
                    log.info("  [NEW_PAGE] template={}  pagePosition={}  ads={}",
                            c.getTemplateName() != null ? c.getTemplateName() : "default_metadata",
                            c.getPagePosition() != null ? c.getPagePosition() : "front",
                            Boolean.TRUE.equals(c.getAdsEnabled()) ? "enabled" : "disabled");

                    String html = "";

                    // 1. Resolve Template if templateName is provided
                    if (c.getTemplateName() != null && !c.getTemplateName().isBlank()) {
                        html += templateService.renderTemplate(c, request, pdfFont);
                    } else if (c.getHtml() == null || c.getHtml().isBlank()) {
                        // Fallback to default_metadata if no templateName and no custom HTML is provided
                        // Temporarily set template name to default_metadata for this call
                        String origTemplateName = c.getTemplateName();
                        c.setTemplateName("default_metadata");
                        html += templateService.renderTemplate(c, request, pdfFont);
                        c.setTemplateName(origTemplateName);
                    }

                    // 2. Append custom layout HTML if provided
                    if (c.getHtml() != null && !c.getHtml().isBlank()) {
                        // If we already rendered a template, we just append the custom HTML at the end.
                        // If there was no template, we just use the custom HTML (wrapped in basic body if it doesn't have it).
                        String customHtml = c.getHtml();
                        if (!html.isEmpty()) {
                            // Inject custom HTML before the closing body tag
                            if (html.contains("</body>")) {
                                html = html.replace("</body>", "<div>" + customHtml + "</div></body>");
                            } else {
                                html += "<div>" + customHtml + "</div>";
                            }
                        } else {
                            if (!customHtml.contains("<html")) {
                                html = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/></head><body style=\"margin: 50px; font-family: " + fontFamily + "; color: #000;\">"
                                        + customHtml + "</body></html>";
                            } else {
                                html = customHtml;
                            }
                        }
                    }

                    // 3. Render HTML to PDF and prepend
                    byte[] htmlPageBytes = metadataFrontPageService.renderHtmlToPdf(html, pageSize);
                    boolean appendToBack = "back".equalsIgnoreCase(c.getPagePosition());
                    if (appendToBack) {
                        currentPdfBytes = metadataFrontPageService.appendPdf(currentPdfBytes, htmlPageBytes);
                        appendedPages++;
                        log.info("  [NEW_PAGE] Appended to back");
                    } else {
                        currentPdfBytes = metadataFrontPageService.prependPdf(currentPdfBytes, htmlPageBytes);
                        prependedPages++;
                        log.info("  [NEW_PAGE] Prepended to front");
                    }
                    hasAddedNewPage = true;
                    continue;
                }

                // For overlay positions (HEADER, FOOTER, LEFT_MARGIN, RIGHT_MARGIN, etc.)
                log.info("  [{}] ads={}  text={}  html={}",
                        posStr,
                        Boolean.TRUE.equals(c.getAdsEnabled()) ? "enabled" : "disabled",
                        c.getText() != null && !c.getText().isBlank() ? "yes" : "no",
                        c.getHtml() != null && !c.getHtml().isBlank() ? "yes" : "no");

                StringBuilder htmlBuilder = new StringBuilder();
                htmlBuilder.append("<div style=\"text-align: center;\">");

                // Logo
                if (c.getLogo() != null && !c.getLogo().isBlank()) {
                    String mimeType = c.getLogoMimeType() != null ? c.getLogoMimeType() : "image/png";
                    htmlBuilder.append("<img src=\"data:").append(mimeType).append(";base64,")
                            .append(c.getLogo())
                            .append("\" style=\"max-width: 200px; display: block; margin: 0 auto; margin-bottom: 8px;\" />");
                }

                // Text
                if (c.getText() != null && !c.getText().isBlank()) {
                    htmlBuilder.append("<p style=\"font-size: 14px; margin: 4px 0; font-weight: bold;\">")
                            .append(c.getText().replace("\n", "<br/>")).append("</p>");
                }

                // Raw HTML
                if (c.getHtml() != null && !c.getHtml().isBlank()) {
                    htmlBuilder.append("<div style=\"margin: 8px 0;\">")
                            .append(c.getHtml()).append("</div>");
                }

                // DOI config object
                if (c.getDoi() != null && !c.getDoi().isBlank()) {
                    String doiUrl = c.getDoi().startsWith("http") ? c.getDoi()
                            : "https://doi.org/" + c.getDoi();
                    htmlBuilder.append("<p style=\"margin: 4px 0; font-size: 12px; color: black;\">doi: ")
                            .append("<a href=\"").append(doiUrl)
                            .append("\" style=\"color: blue; text-decoration: none;\">")
                            .append(doiUrl).append("</a></p>");
                }

                // Date
                if (Boolean.TRUE.equals(c.getIncludeDate())) {
                    String dateStr = java.time.LocalDate.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy"));
                    htmlBuilder.append("<p style=\"margin: 4px 0; font-size: 12px; color: #555;\">Date Generated: ")
                            .append(dateStr).append("</p>");
                }

                // Metadata fields from the JournalMetadataRequest (populated by Drupal / test config)
                if (Boolean.TRUE.equals(c.getIncludeArticleTitle()) && request.getArticleTitle() != null && !request.getArticleTitle().isBlank()) {
                    htmlBuilder.append("<p style=\"margin: 4px 0; font-size: 12px; font-weight: bold;\">")
                            .append(request.getArticleTitle()).append("</p>");
                }
                if (Boolean.TRUE.equals(c.getIncludeAuthors()) && request.getAuthors() != null && !request.getAuthors().isBlank()) {
                    htmlBuilder.append("<p style=\"margin: 4px 0; font-size: 11px; color: #333;\">")
                            .append(request.getAuthors()).append("</p>");
                }
                if (Boolean.TRUE.equals(c.getIncludeDoi()) && request.getDoiValue() != null && !request.getDoiValue().isBlank()) {
                    String doiUrl = request.getDoiValue().startsWith("http") ? request.getDoiValue()
                            : "https://doi.org/" + request.getDoiValue();
                    htmlBuilder.append("<p style=\"margin: 4px 0; font-size: 11px;\">")
                            .append("<a href=\"").append(doiUrl)
                            .append("\" style=\"color: blue; text-decoration: none;\">")
                            .append(doiUrl).append("</a></p>");
                }
                if (Boolean.TRUE.equals(c.getIncludeCopyright()) && request.getArticleCopyright() != null && !request.getArticleCopyright().isBlank()) {
                    htmlBuilder.append("<p style=\"margin: 4px 0; font-size: 10px; color: #555;\">")
                            .append(request.getArticleCopyright()).append("</p>");
                }
                if (Boolean.TRUE.equals(c.getIncludeIssn()) && request.getArticleIssn() != null && !request.getArticleIssn().isBlank()) {
                    htmlBuilder.append("<p style=\"margin: 4px 0; font-size: 10px; color: #555;\">ISSN: ")
                            .append(request.getArticleIssn()).append("</p>");
                }
                if (Boolean.TRUE.equals(c.getIncludeArticleId()) && request.getArticleId() != null && !request.getArticleId().isBlank()) {
                    htmlBuilder.append("<p style=\"margin: 4px 0; font-size: 10px; color: #555;\">Article ID: ")
                            .append(request.getArticleId()).append("</p>");
                }
                if (Boolean.TRUE.equals(c.getIncludeCurrentUser()) && request.getDownloadedBy() != null && !request.getDownloadedBy().isBlank()) {
                    htmlBuilder.append("<p style=\"margin: 4px 0; font-size: 10px; color: #555;\">Downloaded By: ")
                            .append(request.getDownloadedBy()).append("</p>");
                }

                // Ad — for overlay positions, use "header" positioned ads
                if (Boolean.TRUE.equals(c.getAdsEnabled())) {
                    String adUrl = "https://bam-ads-presenter.highwire.org/api/ads?publisherId="
                            + request.getPublisherId() + "&jcode=" + request.getJcode() + "&sectionPath=xpdf";
                    AdResponse adResponse = adFetchService.fetchAds(adUrl);
                    String extractedAdHtml = null;
                    if (adResponse != null && adResponse.getSection() != null) {
                        for (var section : adResponse.getSection()) {
                            if (section.getAdLocation() != null) {
                                for (var location : section.getAdLocation()) {
                                    if ("header".equalsIgnoreCase(location.getPositionName())
                                            && location.getAdData() != null) {
                                        for (var ad : location.getAdData()) {
                                            if (ad.getAdHtml() != null && !ad.getAdHtml().isEmpty()) {
                                                extractedAdHtml = ad.getAdHtml();
                                                break;
                                            }
                                        }
                                    }
                                    if (extractedAdHtml != null) break;
                                }
                            }
                            if (extractedAdHtml != null) break;
                        }
                    } else {
                        log.warn("  [{}] Ad fetch returned no data (url={})", posStr, adUrl);
                    }
                    if (extractedAdHtml != null) {
                        log.info("  [{}] Ad injected ({} chars)", posStr, extractedAdHtml.length());
                        extractedAdHtml = adStampService.processHtmlContent(extractedAdHtml, c.getLegacyDomain());
                        htmlBuilder.append("<div style=\"margin-top: 10px;\">")
                                .append(extractedAdHtml).append("</div>");
                    } else {
                        log.warn("  [{}] No 'header' ad found in response", posStr);
                    }
                }

                htmlBuilder.append("</div>");
                String finalHtml = htmlBuilder.toString();

                // Position-specific CSS and rotation
                float rotation = 0f;
                float sWidth = pageSize.getWidth();
                float sHeight = pageSize.getHeight();
                String cssPosition = "position: absolute; left: 0; right: 0;";
                String innerAlign = "";

                if ("HEADER".equals(posStr)) {
                    cssPosition = "position: absolute; top: 0; left: 0; right: 0; text-align: center;";
                } else if ("FOOTER".equals(posStr)) {
                    cssPosition = "position: absolute; bottom: 0; left: 0; right: 0; text-align: center;";
                } else if ("LEFT_MARGIN".equals(posStr)) {
                    rotation = 90f;
                    sWidth = pageSize.getHeight();
                    sHeight = pageSize.getWidth();
                    cssPosition = "position: absolute; top: 0; left: 0; right: 0; text-align: center;";
                } else if ("RIGHT_MARGIN".equals(posStr)) {
                    rotation = 270f;
                    sWidth = pageSize.getHeight();
                    sHeight = pageSize.getWidth();
                    cssPosition = "position: absolute; top: 0; left: 0; right: 0; text-align: center;";
                } else if ("CENTER".equals(posStr)) {
                    cssPosition = "position: absolute; top: 45%; left: 0; right: 0; text-align: center;";
                }

                // Build font CSS for overlay (includes @font-face only for full non-subset fonts)
                String fontFaceCss = "";
                if (pdfFont != null && pdfFont.isUsableForStamping()) {
                    String mimeType = "truetype".equals(pdfFont.getFormat()) ? "font/ttf" : "font/otf";
                    String base64 = java.util.Base64.getEncoder().encodeToString(pdfFont.getFontBytes());
                    fontFaceCss = "@font-face{font-family:'" + pdfFont.getFontFamily()
                            + "';src:url('data:" + mimeType + ";base64," + base64 + "');}";
                }

                String overlayHtml = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><style>"
                        + fontFaceCss
                        + "body{margin:0;padding:0;font-family:" + fontFamily + ";}</style></head><body>"
                        + "<div style=\"" + cssPosition + "\">"
                        + "<div style=\"display: inline-block; padding: 2px 4px; "
                        + innerAlign + "\">"
                        + finalHtml
                        + "</div></div></body></html>";

                int currentTotalPages;
                try (com.itextpdf.kernel.pdf.PdfDocument tempDoc = new com.itextpdf.kernel.pdf.PdfDocument(
                        new com.itextpdf.kernel.pdf.PdfReader(new java.io.ByteArrayInputStream(currentPdfBytes)))) {
                    currentTotalPages = tempDoc.getNumberOfPages();
                }
                // Stamp only original content pages — skip prepended cover pages at the front
                // and appended pages at the back
                int firstOriginalPage = prependedPages + 1;
                int lastOriginalPage = currentTotalPages - appendedPages;
                String targetPages;
                if (firstOriginalPage > lastOriginalPage) {
                    targetPages = "ALL";
                } else if (firstOriginalPage == 1 && lastOriginalPage == currentTotalPages) {
                    targetPages = "ALL";
                } else if (firstOriginalPage == lastOriginalPage) {
                    targetPages = String.valueOf(firstOriginalPage);
                } else {
                    targetPages = firstOriginalPage + "-" + lastOriginalPage;
                }

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

                currentPdfBytes = stampService.applyStamp(currentPdfBytes, htmlReq,
                        overlayHtml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            String outputFilename = buildOutputFilename(inputFile.getName());

            // If an outputPath was provided, save it directly
            if (request.getOutputPath() != null && !request.getOutputPath().isBlank()) {
                File outputFile = new File(request.getOutputPath());
                File outputDir = outputFile.getParentFile();
                if (outputDir != null && !outputDir.exists()) {
                    outputDir.mkdirs();
                }
                Files.write(Paths.get(request.getOutputPath()), currentPdfBytes);
                log.info("  Saved to: {}", request.getOutputPath());
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("----------------------------------------------------------");
            log.info("  DONE  {}ms  output={}  size={} KB",
                    elapsed, outputFilename, currentPdfBytes.length / 1024);
            log.info("==========================================================");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + outputFilename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(currentPdfBytes.length)
                    .body(currentPdfBytes);

        } catch (StampingException e) {
            throw e;
        } catch (Exception e) {
            throw new StampingException("Failed to process journal metadata request: " + e.getMessage(), e);
        } finally {
            if (demoTempFile != null && demoTempFile.exists()) {
                demoTempFile.delete();
            }
        }
    }

    // ─── Demo PDF Download ─────────────────────────────────────────────

    /**
     * Generate and download a demo-stamped PDF for a saved configuration.
     * Uses demo placeholder metadata and a blank PDF to preview the stamping output.
     */
    @GetMapping(value = "/stamp/demo-pdf/{pubId}/{jcode}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadDemoPdf(@PathVariable String pubId, @PathVariable String jcode) {
        try {
            log.info("==========================================================");
            log.info("  DEMO PDF  pubId={}  jcode={}", pubId, jcode);

            // 1. Build a demo request with all positions from the saved config
            com.stamping.model.JournalMetadataRequest demoRequest = demoStampService.buildDemoRequest(pubId, jcode);

            // 2. Create a blank placeholder PDF
            byte[] blankPdf = demoStampService.createBlankPdf();

            // 3. Save blank PDF to a temp file (processJournalMetadata reads from disk)
            java.io.File tempFile = java.io.File.createTempFile("demo_blank_", ".pdf");
            tempFile.deleteOnExit();
            Files.write(tempFile.toPath(), blankPdf);
            demoRequest.setPdfFilePath(tempFile.getAbsolutePath());
            demoRequest.setOutputPath(null); // Don't save to disk

            // 4. Run the full stamping pipeline by delegating to processJournalMetadata
            ResponseEntity<byte[]> response = processJournalMetadata(demoRequest);

            // 5. Clean up temp file
            tempFile.delete();

            // 6. Return with a descriptive filename
            String filename = "demo_" + pubId + "_" + jcode + "_stamped.pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(response.getBody().length)
                    .body(response.getBody());

        } catch (StampingException e) {
            throw e;
        } catch (Exception e) {
            throw new StampingException("Failed to generate demo PDF: " + e.getMessage(), e);
        }
    }

    // ─── Frontend Config CRUD ───────────────────────────────────────────

    /**
     * List all saved frontend configurations.
     * Reads every config_*.json from the configs/ directory.
     */
    @GetMapping(value = "/configs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> listConfigs() {
        try {
            File configDir = new File("configs");
            java.util.List<Object> results = new java.util.ArrayList<>();
            if (configDir.exists() && configDir.isDirectory()) {
                File[] files = configDir.listFiles((dir, name) -> name.startsWith("config_") && name.endsWith(".json"));
                if (files != null) {
                    java.util.Arrays.sort(files);
                    for (File f : files) {
                        String content = Files.readString(f.toPath(), java.nio.charset.StandardCharsets.UTF_8);
                        results.add(objectMapper.readValue(content, Object.class));
                    }
                }
            }
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Failed to list configs", e);
            return ResponseEntity.internalServerError().body(StampResponse.builder()
                    .success(false).message("Failed to list configurations: " + e.getMessage()).build());
        }
    }

    /**
     * Get a single configuration by pubId and jcode.
     */
    @GetMapping(value = "/configs/{pubId}/{jcode}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getConfig(@PathVariable String pubId, @PathVariable String jcode) {
        try {
            File configFile = new File("configs", "config_" + pubId + "_" + jcode + ".json");
            if (!configFile.exists()) {
                return ResponseEntity.status(404).body(StampResponse.builder()
                        .success(false).message("No configuration found for pubId=" + pubId + ", jcode=" + jcode).build());
            }
            String content = Files.readString(configFile.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            return ResponseEntity.ok(objectMapper.readValue(content, Object.class));
        } catch (Exception e) {
            log.error("Failed to get config", e);
            return ResponseEntity.internalServerError().body(StampResponse.builder()
                    .success(false).message("Failed to get configuration: " + e.getMessage()).build());
        }
    }

    /**
     * Save (create or update) a frontend configuration.
     * Expects the raw frontend JSON with pubId, jcode, templateName, newPage, header, etc.
     * Saves to configs/config_{pubId}_{jcode}.json.
     */
    @PostMapping(value = "/configs", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StampResponse> saveConfig(@RequestBody java.util.Map<String, Object> rawConfig) {
        try {
            String pubId = (String) rawConfig.get("pubId");
            String jcode = (String) rawConfig.get("jcode");

            if (pubId == null || pubId.isBlank() || jcode == null || jcode.isBlank()) {
                throw new StampingException("pubId and jcode are required");
            }

            // Strip unused "value" field from adsBanner in each section
            for (String section : new String[]{"newPage", "header", "footer", "leftMargin", "rightMargin"}) {
                Object sectionObj = rawConfig.get(section);
                if (sectionObj instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> sectionMap = (java.util.Map<String, Object>) sectionObj;
                    Object adsBannerObj = sectionMap.get("adsBanner");
                    if (adsBannerObj instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> adsBanner = (java.util.Map<String, Object>) adsBannerObj;
                        adsBanner.remove("value");
                    }
                }
            }

            File configDir = new File("configs");
            if (!configDir.exists()) configDir.mkdirs();

            File outputFile = new File(configDir, "config_" + pubId + "_" + jcode + ".json");
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rawConfig);
            Files.writeString(outputFile.toPath(), json, java.nio.charset.StandardCharsets.UTF_8);

            log.info("Config saved: {}", outputFile.getAbsolutePath());

            // DEV-ONLY: auto-generate a demo test request file on every save/update
            demoConfigGeneratorService.generateDemoTestConfig(rawConfig);

            return ResponseEntity.ok(StampResponse.builder()
                    .success(true).message("Configuration saved successfully")
                    .outputFilePath(outputFile.getAbsolutePath()).build());
        } catch (Exception e) {
            log.error("Failed to save config", e);
            return ResponseEntity.internalServerError().body(StampResponse.builder()
                    .success(false).message("Failed to save configuration: " + e.getMessage()).build());
        }
    }

    /**
     * Archive (soft-delete) a configuration by moving it to the archive_configs directory.
     */
    @DeleteMapping(value = "/configs/{pubId}/{jcode}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StampResponse> archiveConfig(@PathVariable String pubId, @PathVariable String jcode) {
        try {
            File configFile = new File("configs", "config_" + pubId + "_" + jcode + ".json");
            if (!configFile.exists()) {
                return ResponseEntity.status(404).body(StampResponse.builder()
                        .success(false).message("Configuration not found in active directory").build());
            }

            File archiveDir = new File("archive_configs");
            if (!archiveDir.exists()) archiveDir.mkdirs();

            File archiveFile = new File(archiveDir, configFile.getName());
            
            // Move file
            Files.move(configFile.toPath(), archiveFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            return ResponseEntity.ok(StampResponse.builder().success(true).message("Configuration archived successfully").build());
        } catch (Exception e) {
            log.error("Failed to archive config", e);
            return ResponseEntity.internalServerError().body(StampResponse.builder()
                    .success(false).message("Failed to archive: " + e.getMessage()).build());
        }
    }

    /**
     * Restore an archived configuration by moving it back to the active configs directory.
     */
    @PutMapping(value = "/configs/{pubId}/{jcode}/restore", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StampResponse> restoreConfig(@PathVariable String pubId, @PathVariable String jcode) {
        try {
            File archiveFile = new File("archive_configs", "config_" + pubId + "_" + jcode + ".json");
            if (!archiveFile.exists()) {
                return ResponseEntity.status(404).body(StampResponse.builder()
                        .success(false).message("Configuration not found in archive directory").build());
            }

            File configDir = new File("configs");
            if (!configDir.exists()) configDir.mkdirs();

            File configFile = new File(configDir, archiveFile.getName());

            // Move file back
            Files.move(archiveFile.toPath(), configFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            return ResponseEntity.ok(StampResponse.builder().success(true).message("Configuration restored successfully").build());
        } catch (Exception e) {
            log.error("Failed to restore config", e);
            return ResponseEntity.internalServerError().body(StampResponse.builder()
                    .success(false).message("Failed to restore: " + e.getMessage()).build());
        }
    }

    private String buildOutputFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "stamped.pdf";
        }
        if (originalFilename.toLowerCase().endsWith(".pdf")) {
            return originalFilename.substring(0, originalFilename.length() - 4) + "_stamped.pdf";
        }
        return originalFilename + "_stamped.pdf";
    }
}
