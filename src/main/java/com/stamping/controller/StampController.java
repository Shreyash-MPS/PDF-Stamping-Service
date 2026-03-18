package com.stamping.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stamping.exception.StampingException;
import com.stamping.model.DynamicStampRequest;
import com.stamping.model.FilePathStampRequest;
import com.stamping.model.StampPosition;
import com.stamping.model.StampRequest;
import com.stamping.model.StampResponse;
import com.stamping.model.StampType;
import com.stamping.model.AdJsonRequest;
import com.stamping.model.MetadataFrontPageRequest;
import com.stamping.model.ad.AdResponse;
import com.stamping.service.AdFetchService;
import com.stamping.service.AdStampService;
import com.stamping.service.MetadataFrontPageService;
import com.stamping.service.StampService;
import com.stamping.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * Stamp a PDF with text, image, or HTML content.
     *
     * @param file      the source PDF file
     * @param stamp     the stamp file (image or HTML), required for IMAGE and HTML
     *                  types
     * @param stampType the type of stamp: TEXT, IMAGE, or HTML
     * @param position  stamp position: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT,
     *                  BOTTOM_RIGHT, CENTER, CUSTOM
     * @param x         custom X coordinate (when position = CUSTOM)
     * @param y         custom Y coordinate (when position = CUSTOM)
     * @param opacity   opacity from 0.0 to 1.0 (default: 1.0)
     * @param rotation  rotation angle in degrees (default: 0)
     * @param scale     scale factor for image/HTML stamps (default: 1.0)
     * @param pages     page selection: ALL, FIRST, LAST, or comma-separated like
     *                  "1,3,5-7" (default: ALL)
     * @param text      text content (required for TEXT stamp type)
     * @param fontSize  font size in points (default: 14)
     * @param fontColor font color as hex string e.g. "#FF0000" (default: "#000000")
     * @return the stamped PDF file
     */
    @PostMapping(value = "/stamp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> stampPdf(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "stamp", required = false) MultipartFile stamp,
            @RequestParam("stampType") StampType stampType,
            @RequestParam(value = "position", defaultValue = "CENTER") StampPosition position,
            @RequestParam(value = "x", required = false) Float x,
            @RequestParam(value = "y", required = false) Float y,
            @RequestParam(value = "opacity", defaultValue = "1.0") float opacity,
            @RequestParam(value = "rotation", defaultValue = "0") float rotation,
            @RequestParam(value = "scale", defaultValue = "1.0") float scale,
            @RequestParam(value = "pages", defaultValue = "ALL") String pages,
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "fontSize", defaultValue = "14") float fontSize,
            @RequestParam(value = "fontColor", defaultValue = "#000000") String fontColor,
            @RequestParam(value = "stampWidth", required = false) Float stampWidth,
            @RequestParam(value = "stampHeight", required = false) Float stampHeight) {

        try {
            log.info("Received stamp request: type={}, file={}, stampFile={}",
                    stampType, file.getOriginalFilename(),
                    stamp != null ? stamp.getOriginalFilename() : "none");

            // Validate inputs
            if (file.isEmpty()) {
                throw new StampingException("PDF file is empty");
            }

            // Build the request
            StampRequest request = StampRequest.builder()
                    .stampType(stampType)
                    .position(position)
                    .x(x)
                    .y(y)
                    .opacity(opacity)
                    .rotation(rotation)
                    .scale(scale)
                    .pages(pages)
                    .text(text)
                    .fontSize(fontSize)
                    .fontColor(fontColor)
                    .stampWidth(stampWidth)
                    .stampHeight(stampHeight)
                    .build();

            // Get stamp content bytes
            byte[] stampContent = null;
            if (stamp != null && !stamp.isEmpty()) {
                stampContent = stamp.getBytes();
            }

            // Apply stamp
            byte[] stampedPdf = stampService.applyStamp(file.getBytes(), request, stampContent);

            // Build response
            String outputFilename = buildOutputFilename(file.getOriginalFilename());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + outputFilename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(stampedPdf.length)
                    .body(stampedPdf);

        } catch (StampingException e) {
            throw e;
        } catch (Exception e) {
            throw new StampingException("Failed to process stamp request: " + e.getMessage(), e);
        }
    }

    /**
     * Dynamically stamp a PDF based on the JSON configuration from the frontend.
     *
     * @param file      the source PDF file
     * @param configStr the JSON configuration string
     * @param imageFile optional image file for logo
     * @return the stamped PDF file
     */
    @PostMapping(value = "/stamp/dynamic", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> stampPdfDynamic(
            @RequestPart("file") MultipartFile file,
            @RequestPart("config") String configStr,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {

        try {
            log.info("Received dynamic stamp request for file: {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                throw new StampingException("PDF file is empty");
            }

            DynamicStampRequest config = objectMapper.readValue(configStr, DynamicStampRequest.class);

            byte[] currentPdfBytes = file.getBytes();

            // Build list of position configs to process
            java.util.Map<String, DynamicStampRequest.Configuration> positionsToProcess = new java.util.LinkedHashMap<>();

            if (config.getPositions() != null && !config.getPositions().isEmpty()) {
                positionsToProcess.putAll(config.getPositions());
            } else if (config.getConfiguration() != null) {
                DynamicStampRequest.Configuration c = config.getConfiguration();
                positionsToProcess.put("HEADER", c);
            } else {
                throw new StampingException("No configuration or positions provided");
            }

            // Extract page size once
            com.itextpdf.kernel.geom.Rectangle pageSize;
            try (com.itextpdf.kernel.pdf.PdfDocument tempOriginal = new com.itextpdf.kernel.pdf.PdfDocument(
                    new com.itextpdf.kernel.pdf.PdfReader(new java.io.ByteArrayInputStream(currentPdfBytes)))) {
                pageSize = tempOriginal.getPage(1).getPageSize();
            }

            // Process each position
            boolean hasAddedNewPage = false;
            for (var entry : positionsToProcess.entrySet()) {
                String posStr = entry.getKey();
                DynamicStampRequest.Configuration c = entry.getValue();
                if (c == null)
                    continue;

                // Build dynamic HTML for this position
                boolean isNewPage = "NEW_PAGE".equalsIgnoreCase(posStr) || (config.getStrategy() != null && "new_page".equalsIgnoreCase(config.getStrategy()));
                StringBuilder htmlBuilder = new StringBuilder();
                htmlBuilder.append("<div style=\"text-align: ").append(isNewPage ? "left" : "center").append(";\">");

                // 1. Logo
                if (c.getLogo() != null && !c.getLogo().isBlank()) {
                    if (imageFile != null && !imageFile.isEmpty()) {
                        byte[] imageBytes = imageFile.getBytes();
                        String base64Image = java.util.Base64.getEncoder().encodeToString(imageBytes);
                        String mimeType = "image/png";
                        String originalFilename = imageFile.getOriginalFilename();
                        if (originalFilename != null && originalFilename.toLowerCase().endsWith(".jpg")) {
                            mimeType = "image/jpeg";
                        }
                        htmlBuilder.append("<img src=\"data:").append(mimeType).append(";base64,").append(base64Image)
                                .append("\" style=\"max-width: 200px; display: block; margin: 0 auto; margin-bottom: 8px;\" />");
                    } else {
                        String mimeType = c.getLogoMimeType() != null ? c.getLogoMimeType() : "image/png";
                        htmlBuilder.append("<img src=\"data:").append(mimeType).append(";base64,")
                                .append(c.getLogo())
                                .append("\" style=\"max-width: 200px; display: block; margin: 0 auto; margin-bottom: 8px;\" />");
                    }
                }

                // 2. Custom Text
                if (c.getText() != null && !c.getText().isBlank()) {
                    htmlBuilder.append("<p style=\"font-size: 14px; margin: 4px 0; font-weight: bold;\">")
                            .append(c.getText().replace("\n", "<br/>")).append("</p>");
                }

                // 3. Raw HTML
                if (c.getHtml() != null && !c.getHtml().isBlank()) {
                    htmlBuilder.append("<div style=\"margin: 8px 0;\">")
                            .append(c.getHtml()).append("</div>");
                }

                // 4. DOI
                if (c.getDoi() != null && !c.getDoi().isBlank()) {
                    String expectedDoiUrl = c.getDoi().startsWith("http") ? c.getDoi()
                            : "https://doi.org/" + c.getDoi();
                    htmlBuilder.append("<p style=\"margin: 4px 0; font-size: 12px; color: black;\">doi: ")
                            .append("<a href=\"").append(expectedDoiUrl)
                            .append("\" style=\"color: blue; text-decoration: none;\">")
                            .append(expectedDoiUrl).append("</a></p>");
                } else if (config.getJcode() != null && !config.getJcode().isBlank()) {
                    String expectedDoiUrl = "https://doi.org/" + config.getJcode();
                    htmlBuilder.append("<p style=\"margin: 4px 0; font-size: 12px; color: black;\">doi: ")
                            .append("<a href=\"").append(expectedDoiUrl)
                            .append("\" style=\"color: blue; text-decoration: none;\">")
                            .append(expectedDoiUrl).append("</a></p>");
                }

                // 5. Date
                if (c.isIncludeDate()) {
                    String dateStr = java.time.LocalDate.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy"));
                    htmlBuilder.append("<p style=\"margin: 4px 0; font-size: 12px; color: #555;\">Date Generated: ")
                            .append(dateStr).append("</p>");
                }

                // 6. Ad Embedding
                if (c.getAd() != null && !c.getAd().isBlank()) {
                    AdResponse adResponse = adFetchService.fetchAds(c.getAd());
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
                                    if (extractedAdHtml != null)
                                        break;
                                }
                            }
                            if (extractedAdHtml != null)
                                break;
                        }
                    }
                    if (extractedAdHtml != null) {
                        extractedAdHtml = adStampService.processHtmlContent(extractedAdHtml);
                        htmlBuilder.append("<div style=\"margin-top: 10px;\">")
                                .append(extractedAdHtml).append("</div>");
                    }
                }

                htmlBuilder.append("</div>");
                String finalHtml = htmlBuilder.toString();

                // Decide strategy: per-position NEW_PAGE or global strategy fallback
                boolean shouldAddNewPage = "NEW_PAGE".equalsIgnoreCase(posStr)
                        || "new_page".equalsIgnoreCase(config.getStrategy());

                if (shouldAddNewPage) {
                    String fullHtml = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/></head>" +
                            "<body style=\"margin: 50px;\">" + finalHtml + "</body></html>";
                    byte[] htmlPageBytes = metadataFrontPageService.renderHtmlToPdf(fullHtml, pageSize);
                    currentPdfBytes = metadataFrontPageService.prependPdf(currentPdfBytes, htmlPageBytes);
                    hasAddedNewPage = true;
                } else {
                    float rotation = 0f;
                    float sWidth = pageSize.getWidth();
                    float sHeight = pageSize.getHeight();
                    String cssPosition = "position: absolute; left: 0; right: 0;";
                    String innerAlign = "";

                    if ("HEADER".equals(posStr) || "TOP_CENTER".equals(posStr) || "TOP".equals(posStr)) {
                        cssPosition = "position: absolute; top: 0; left: 0; right: 0; text-align: center;";
                    } else if ("FOOTER".equals(posStr) || "BOTTOM_CENTER".equals(posStr) || "BOTTOM".equals(posStr)) {
                        cssPosition = "position: absolute; bottom: 0; left: 0; right: 0; text-align: center;";
                    } else if ("TOP_LEFT".equals(posStr)) {
                        cssPosition = "position: absolute; top: 2px; left: 2px; text-align: left;";
                    } else if ("TOP_RIGHT".equals(posStr)) {
                        cssPosition = "position: absolute; top: 2px; right: 2px; text-align: right;";
                    } else if ("BOTTOM_LEFT".equals(posStr)) {
                        cssPosition = "position: absolute; bottom: 2px; left: 2px; text-align: left;";
                    } else if ("BOTTOM_RIGHT".equals(posStr)) {
                        cssPosition = "position: absolute; bottom: 2px; right: 2px; text-align: right;";
                    } else if ("CENTER".equals(posStr)) {
                        cssPosition = "position: absolute; top: 45%; left: 0; right: 0; text-align: center;";
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
                    }

                    String overlayHtml = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><style>body{margin:0;padding:0;}</style></head><body>"
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
                    String targetPages = hasAddedNewPage ? "2-" + currentTotalPages : "ALL";

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
            }

            String outputFilename = buildOutputFilename(file.getOriginalFilename());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + outputFilename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(currentPdfBytes.length)
                    .body(currentPdfBytes);

        } catch (StampingException e) {
            throw e;
        } catch (Exception e) {
            throw new StampingException("Failed to process dynamic stamp request: " + e.getMessage(), e);
        }
    }

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
        try {
            log.info("Received process-journal request for file: {}", request.getPdfFilePath());

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

            byte[] currentPdfBytes = Files.readAllBytes(Paths.get(request.getPdfFilePath()));

            // Extract page size once
            com.itextpdf.kernel.geom.Rectangle pageSize;
            try (com.itextpdf.kernel.pdf.PdfDocument tempOriginal = new com.itextpdf.kernel.pdf.PdfDocument(
                    new com.itextpdf.kernel.pdf.PdfReader(new java.io.ByteArrayInputStream(currentPdfBytes)))) {
                pageSize = tempOriginal.getPage(1).getPageSize();
            }

            // Standard font family for all stamping output
            String fontFamily = "Verdana, Arial, Helvetica, sans-serif";

            boolean hasAddedNewPage = false;

            for (var entry : positionsToProcess.entrySet()) {
                String posStr = entry.getKey();
                DynamicStampRequest.Configuration c = entry.getValue();
                if (c == null) continue;

                boolean isNewPage = "NEW_PAGE".equalsIgnoreCase(posStr);

                // For NEW_PAGE (cover page) positions — generate a new prepended page
                if (isNewPage) {
                    String html = "";

                    // 1. Resolve Template if templateName is provided
                    if (c.getTemplateName() != null && !c.getTemplateName().isBlank()) {
                        html += templateService.renderTemplate(c, request);
                    } else if (c.getHtml() == null || c.getHtml().isBlank()) {
                        // Fallback to default_metadata if no templateName and no custom HTML is provided
                        // Temporarily set template name to default_metadata for this call
                        String origTemplateName = c.getTemplateName();
                        c.setTemplateName("default_metadata");
                        html += templateService.renderTemplate(c, request);
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
                    currentPdfBytes = metadataFrontPageService.prependPdf(currentPdfBytes, htmlPageBytes);
                    hasAddedNewPage = true;
                    continue;
                }

                // For overlay positions (HEADER, FOOTER, LEFT_MARGIN, RIGHT_MARGIN, etc.)
                // All data comes from the saved config — Drupal doesn't touch these
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
                if (c.isIncludeDate()) {
                    String dateStr = java.time.LocalDate.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy"));
                    htmlBuilder.append("<p style=\"margin: 4px 0; font-size: 12px; color: #555;\">Date Generated: ")
                            .append(dateStr).append("</p>");
                }

                // Ad
                if (c.getAd() != null && !c.getAd().isBlank()) {
                    AdResponse adResponse = adFetchService.fetchAds(c.getAd());
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
                    }
                    if (extractedAdHtml != null) {
                        extractedAdHtml = adStampService.processHtmlContent(extractedAdHtml);
                        htmlBuilder.append("<div style=\"margin-top: 10px;\">")
                                .append(extractedAdHtml).append("</div>");
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

                String overlayHtml = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><style>body{margin:0;padding:0;font-family:" + fontFamily + ";}</style></head><body>"
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
                String targetPages = hasAddedNewPage ? "2-" + currentTotalPages : "ALL";

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
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + outputFilename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(currentPdfBytes.length)
                    .body(currentPdfBytes);

        } catch (StampingException e) {
            throw e;
        } catch (Exception e) {
            throw new StampingException("Failed to process journal metadata request: " + e.getMessage(), e);
        }
    }

    /**
     * Stamp a PDF using file paths (JSON-based endpoint).
     * Reads input PDF from disk, applies stamp, and saves output to specified path.
     *
     * @param request JSON request containing file paths and stamp configuration
     * @return JSON response with operation status and output file path
     */
    @PostMapping(value = "/stamp/file-path", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StampResponse> stampPdfWithFilePath(@RequestBody FilePathStampRequest request) {
        try {
            log.info("Received file-path stamp request: type={}, input={}, output={}",
                    request.getStampType(), request.getInputFilePath(), request.getOutputFilePath());

            // Validate input file path
            if (request.getInputFilePath() == null || request.getInputFilePath().isBlank()) {
                throw new StampingException("Input file path is required");
            }
            if (request.getOutputFilePath() == null || request.getOutputFilePath().isBlank()) {
                throw new StampingException("Output file path is required");
            }

            File inputFile = new File(request.getInputFilePath());
            if (!inputFile.exists()) {
                throw new StampingException("Input file not found: " + request.getInputFilePath());
            }
            if (!inputFile.canRead()) {
                throw new StampingException("Cannot read input file: " + request.getInputFilePath());
            }

            // Read input PDF
            byte[] pdfBytes = Files.readAllBytes(Paths.get(request.getInputFilePath()));

            // Build stamp request
            StampRequest stampRequest = StampRequest.builder()
                    .stampType(request.getStampType())
                    .position(request.getPosition())
                    .x(request.getX())
                    .y(request.getY())
                    .opacity(request.getOpacity())
                    .rotation(request.getRotation())
                    .scale(request.getScale())
                    .pages(request.getPages())
                    .text(request.getText())
                    .fontSize(request.getFontSize())
                    .fontColor(request.getFontColor())
                    .stampWidth(request.getStampWidth())
                    .stampHeight(request.getStampHeight())
                    .build();

            // Read stamp content if needed
            byte[] stampContent = null;
            if (request.getStampFilePath() != null && !request.getStampFilePath().isBlank()) {
                File stampFile = new File(request.getStampFilePath());
                if (!stampFile.exists()) {
                    throw new StampingException("Stamp file not found: " + request.getStampFilePath());
                }
                stampContent = Files.readAllBytes(Paths.get(request.getStampFilePath()));
            }

            // Apply stamp
            byte[] stampedPdf = stampService.applyStamp(pdfBytes, stampRequest, stampContent);

            // Ensure output directory exists
            File outputFile = new File(request.getOutputFilePath());
            File outputDir = outputFile.getParentFile();
            if (outputDir != null && !outputDir.exists()) {
                outputDir.mkdirs();
            }

            // Write output file
            Files.write(Paths.get(request.getOutputFilePath()), stampedPdf);

            // Build response
            StampResponse response = StampResponse.builder()
                    .success(true)
                    .message("PDF stamped successfully")
                    .outputFilePath(request.getOutputFilePath())
                    .fileSizeBytes(stampedPdf.length)
                    .build();

            log.info("Successfully stamped PDF: {} ({} bytes)", request.getOutputFilePath(), stampedPdf.length);

            return ResponseEntity.ok(response);

        } catch (StampingException e) {
            log.error("Stamping failed: {}", e.getMessage());
            StampResponse response = StampResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Unexpected error during stamping", e);
            StampResponse response = StampResponse.builder()
                    .success(false)
                    .message("Failed to process stamp request: " + e.getMessage())
                    .build();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Note: The /stamp/ad endpoint functionality has been migrated to
    // /stamp/adJson.
    // Preserving the method signature below as deprecated or removing entirely
    // based on usage.
    // Removed to favor the unified AdStampService pipeline.

    /**
     * Stamp a PDF with an ad fetched from a JSON URL.
     * Maps ads based on JSON positionName configurations ("header" or "pdf ad
     * one").
     * Reads the source file from disk.
     *
     * @param request the JSON request containing input path and ad configuration
     * @return the stamped/prepended PDF file (or a JSON response if outputPath is
     *         provided)
     */
    @PostMapping(value = "/stamp/adJson", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> stampPdfWithAdJson(@RequestBody AdJsonRequest request) {
        try {
            log.info("Received adJson stamp request: inputPath={}, url={}, adType={}",
                    request.getInputPath(), request.getAdJsonUrl(), request.getAdType());

            if (request.getInputPath() == null || request.getInputPath().isBlank()) {
                throw new StampingException("Input file path is required");
            }
            if (request.getAdJsonUrl() == null || request.getAdJsonUrl().isBlank()) {
                throw new StampingException("Ad JSON URL is required");
            }

            File inputFile = new File(request.getInputPath());
            if (!inputFile.exists() || !inputFile.canRead()) {
                throw new StampingException("Cannot read input file: " + request.getInputPath());
            }

            byte[] pdfBytes = Files.readAllBytes(Paths.get(request.getInputPath()));
            String adType = request.getAdType() != null ? request.getAdType() : "all";

            byte[] stampedPdf = adStampService.processAdJson(pdfBytes, request.getAdJsonUrl(), adType);

            if (request.getOutputPath() != null && !request.getOutputPath().isBlank()) {
                // Ensure output directory exists
                File outputFile = new File(request.getOutputPath());
                File outputDir = outputFile.getParentFile();
                if (outputDir != null && !outputDir.exists()) {
                    outputDir.mkdirs();
                }
                Files.write(Paths.get(request.getOutputPath()), stampedPdf);

                StampResponse response = StampResponse.builder()
                        .success(true)
                        .message("PDF stamped successfully")
                        .outputFilePath(request.getOutputPath())
                        .fileSizeBytes(stampedPdf.length)
                        .build();
                return ResponseEntity.ok(response);
            } else {
                String outputFilename = inputFile.getName();
                outputFilename = buildOutputFilename(outputFilename);

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + outputFilename + "\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .contentLength(stampedPdf.length)
                        .body(stampedPdf);
            }

        } catch (StampingException e) {
            throw e;
        } catch (Exception e) {
            throw new StampingException("Failed to process adJson stamp request: " + e.getMessage(), e);
        }
    }

    /**
     * Prepend a metadata front page (logo, journal name, doi, authors, date).
     * Reads the source file from disk.
     *
     * @param request the JSON request containing input path and metadata parameters
     * @return the prepended PDF file (or a JSON response if outputPath is provided)
     */
    @PostMapping(value = "/stamp/metadata-page", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> prependMetadataPage(@RequestBody MetadataFrontPageRequest request) {
        try {
            log.info("Received metadata-page request: inputPath={}, title={}",
                    request.getInputPath(), request.getArticleTitle());

            if (request.getInputPath() == null || request.getInputPath().isBlank()) {
                throw new StampingException("Input file path is required");
            }

            File inputFile = new File(request.getInputPath());
            if (!inputFile.exists() || !inputFile.canRead()) {
                throw new StampingException("Cannot read input file: " + request.getInputPath());
            }

            byte[] pdfBytes = Files.readAllBytes(Paths.get(request.getInputPath()));

            byte[] stampedPdf = metadataFrontPageService.prependMetadataPage(pdfBytes, request);

            if (request.getOutputPath() != null && !request.getOutputPath().isBlank()) {
                // Ensure output directory exists
                File outputFile = new File(request.getOutputPath());
                File outputDir = outputFile.getParentFile();
                if (outputDir != null && !outputDir.exists()) {
                    outputDir.mkdirs();
                }
                Files.write(Paths.get(request.getOutputPath()), stampedPdf);

                StampResponse response = StampResponse.builder()
                        .success(true)
                        .message("Metadata page prepended successfully")
                        .outputFilePath(request.getOutputPath())
                        .fileSizeBytes(stampedPdf.length)
                        .build();
                return ResponseEntity.ok(response);
            } else {
                String outputFilename = inputFile.getName();
                outputFilename = buildOutputFilename(outputFilename);

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + outputFilename + "\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .contentLength(stampedPdf.length)
                        .body(stampedPdf);
            }

        } catch (StampingException e) {
            throw e;
        } catch (Exception e) {
            throw new StampingException("Failed to process metadata-page request: " + e.getMessage(), e);
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

            File configDir = new File("configs");
            if (!configDir.exists()) configDir.mkdirs();

            File outputFile = new File(configDir, "config_" + pubId + "_" + jcode + ".json");
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rawConfig);
            Files.writeString(outputFile.toPath(), json, java.nio.charset.StandardCharsets.UTF_8);

            log.info("Config saved: {}", outputFile.getAbsolutePath());

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
     * Archive (soft-delete) a configuration.
     */
    @DeleteMapping(value = "/configs/{pubId}/{jcode}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StampResponse> archiveConfig(@PathVariable String pubId, @PathVariable String jcode) {
        try {
            File configFile = new File("configs", "config_" + pubId + "_" + jcode + ".json");
            if (!configFile.exists()) {
                return ResponseEntity.status(404).body(StampResponse.builder()
                        .success(false).message("Configuration not found").build());
            }
            String content = Files.readString(configFile.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> config = objectMapper.readValue(content, java.util.Map.class);
            config.put("archived", true);
            Files.writeString(configFile.toPath(),
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config),
                    java.nio.charset.StandardCharsets.UTF_8);
            return ResponseEntity.ok(StampResponse.builder().success(true).message("Configuration archived").build());
        } catch (Exception e) {
            log.error("Failed to archive config", e);
            return ResponseEntity.internalServerError().body(StampResponse.builder()
                    .success(false).message("Failed to archive: " + e.getMessage()).build());
        }
    }

    /**
     * Restore an archived configuration.
     */
    @PutMapping(value = "/configs/{pubId}/{jcode}/restore", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StampResponse> restoreConfig(@PathVariable String pubId, @PathVariable String jcode) {
        try {
            File configFile = new File("configs", "config_" + pubId + "_" + jcode + ".json");
            if (!configFile.exists()) {
                return ResponseEntity.status(404).body(StampResponse.builder()
                        .success(false).message("Configuration not found").build());
            }
            String content = Files.readString(configFile.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> config = objectMapper.readValue(content, java.util.Map.class);
            config.put("archived", false);
            Files.writeString(configFile.toPath(),
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(config),
                    java.nio.charset.StandardCharsets.UTF_8);
            return ResponseEntity.ok(StampResponse.builder().success(true).message("Configuration restored").build());
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
