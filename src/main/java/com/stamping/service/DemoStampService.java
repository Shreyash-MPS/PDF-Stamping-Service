package com.stamping.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stamping.exception.StampingException;
import com.stamping.model.DynamicStampRequest;
import com.stamping.model.JournalMetadataRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Generates a demo-stamped PDF from a saved frontend config.
 * Creates a blank placeholder PDF, then applies all configured stamps
 * using demo/default metadata values so users can preview the final output.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemoStampService {

    private final ObjectMapper objectMapper;
    private final DemoConfigGeneratorService demoConfigGeneratorService;

    private static final String CONFIGS_DIR = "configs";

    // Default demo metadata values
    private static final String DEMO_ARTICLE_TITLE = "Sample Article Title for Demo Preview";
    private static final String DEMO_AUTHORS = "Author One, Author Two, Author Three";
    private static final String DEMO_DOI = "10.xxxx/sample.doi.2026";
    private static final String DEMO_COPYRIGHT = "© 2026 Demo Publisher. All rights reserved.";
    private static final String DEMO_ISSN = "1234-5678";
    private static final String DEMO_ARTICLE_ID = "ART-DEMO-001";
    private static final String DEMO_DOWNLOADED_BY = "demo.user@example.com";

    /**
     * Builds a JournalMetadataRequest with demo default values from a saved config.
     * Any field enabled in the config gets populated with a demo placeholder value.
     */
    @SuppressWarnings("unchecked")
    public JournalMetadataRequest buildDemoRequest(String pubId, String jcode) {
        File configFile = new File(CONFIGS_DIR, "config_" + pubId + "_" + jcode + ".json");
        if (!configFile.exists()) {
            throw new StampingException("No saved config found for pubId=" + pubId + ", jcode=" + jcode);
        }

        try {
            String content = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
            Map<String, Object> rawConfig = objectMapper.readValue(content, Map.class);

            Map<String, DynamicStampRequest.Configuration> positions =
                    demoConfigGeneratorService.buildDemoPositions(rawConfig);

            return JournalMetadataRequest.builder()
                    .publisherId(pubId)
                    .jcode(jcode)
                    .articleTitle(DEMO_ARTICLE_TITLE)
                    .authors(DEMO_AUTHORS)
                    .doiValue(DEMO_DOI)
                    .articleCopyright(DEMO_COPYRIGHT.replace("Demo Publisher", pubId))
                    .articleIssn(DEMO_ISSN)
                    .articleId("ART-" + jcode.toUpperCase() + "-001")
                    .downloadedBy(DEMO_DOWNLOADED_BY)
                    .positions(positions)
                    .build();
        } catch (StampingException e) {
            throw e;
        } catch (Exception e) {
            throw new StampingException("Failed to build demo request: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a minimal blank PDF (single A4 page with a centered placeholder text).
     */
    public byte[] createBlankPdf() {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(os);
            com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(writer);
            com.itextpdf.kernel.geom.PageSize pageSize = com.itextpdf.kernel.geom.PageSize.A4;
            pdfDoc.setDefaultPageSize(pageSize);

            com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdfDoc);

            // Add some vertical spacing to center the text
            document.add(new com.itextpdf.layout.element.Paragraph("\n\n\n\n\n\n\n\n\n\n\n\n\n\n"));
            document.add(new com.itextpdf.layout.element.Paragraph("DEMO PDF — Original Content Would Appear Here")
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                    .setFontSize(16)
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY));
            document.add(new com.itextpdf.layout.element.Paragraph("This is a placeholder page used for demo stamping preview.")
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                    .setFontSize(11)
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY));

            document.close();
            return os.toByteArray();
        } catch (Exception e) {
            throw new StampingException("Failed to create blank demo PDF: " + e.getMessage(), e);
        }
    }
}
