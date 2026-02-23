package com.stamping.controller;

import com.stamping.exception.StampingException;
import com.stamping.model.FilePathStampRequest;
import com.stamping.model.StampPosition;
import com.stamping.model.StampRequest;
import com.stamping.model.StampResponse;
import com.stamping.model.StampType;
import com.stamping.model.AdJsonRequest;
import com.stamping.model.MetadataFrontPageRequest;
import com.stamping.service.AdStampService;
import com.stamping.service.MetadataFrontPageService;
import com.stamping.service.StampService;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StampController {

    private final StampService stampService;
    private final AdStampService adStampService;
    private final MetadataFrontPageService metadataFrontPageService;

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
