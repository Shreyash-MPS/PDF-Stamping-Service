package com.stamping.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
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

    // HighWire logo as inline SVG for demo PDF stamping
    private static final String HIGHWIRE_LOGO_SVG =
            "<svg width=\"160\" height=\"56\" xmlns=\"http://www.w3.org/2000/svg\">"
            + "<g fill=\"#931430\" fill-rule=\"evenodd\">"
            + "<path d=\"M37.584 5.132l1.132-3.288c.95.337 1.874.735 2.764 1.19l-1.537 3.112c-.765-.388-1.546-.728-2.36-1.014M33.603 4.077L34.156.64c.995.164 1.97.392 2.924.685l-.993 3.33c-.806-.248-1.636-.442-2.484-.578M35.274 11.58L36.4 8.293c.787.267 1.6.606 2.357.993l-1.547 3.113c-.622-.322-1.272-.58-1.936-.817M29.675 10.818l.003-3.5c.835-.014 1.707.033 2.546.147l-.452 3.47c-.69-.094-1.387-.117-2.097-.117M32.422 10.972l.54-3.453c.843.12 1.69.3 2.5.542l-.98 3.365c-.672-.2-1.364-.34-2.06-.455M29.664 14.884l.007-3.48c.706 0 1.41.048 2.097.137l-.45 3.454c-.542-.073-1.092-.11-1.653-.11M33.816 15.617l1.128-3.288c.666.232 1.313.514 1.937.832l-1.542 3.114c-.49-.253-.996-.476-1.522-.66M31.727 15.066l.55-3.433c.695.116 1.385.273 2.053.477l-.99 3.337c-.527-.164-1.067-.29-1.613-.38M45.518 10.093l2.287-2.6c.75.677 1.454 1.4 2.105 2.166l-2.607 2.276c-.553-.656-1.15-1.272-1.785-1.843M41.586 7.028l1.72-3.016c.865.513 1.7 1.075 2.49 1.69L43.71 8.466c-.674-.524-1.385-1-2.122-1.437M41.132 14.81l2.29-2.595c.633.575 1.232 1.19 1.785 1.844l-2.6 2.267c-.458-.534-.953-1.046-1.475-1.518M38.237 12.834l1.718-3.015c.74.433 1.445.914 2.12 1.44l-2.09 2.76c-.556-.432-1.137-.825-1.748-1.186M38.042 18.113l2.294-2.596c.52.474 1.017.982 1.47 1.52l-2.608 2.272c-.355-.422-.74-.82-1.156-1.197M35.98 16.6l1.72-3.02c.61.36 1.194.756 1.75 1.187l-2.1 2.762c-.43-.338-.888-.65-1.37-.93M53.07 31.26l3.4.512c-.144.995-.352 1.988-.633 2.97l-3.303-.954c.23-.833.41-1.677.537-2.528M51.37 18.888l3.185-1.31c.378.94.687 1.9.943 2.89l-3.33.874c-.213-.84-.482-1.66-.8-2.454M15.467 49.36l-1.838 2.934c-.85-.545-1.655-1.15-2.414-1.796l2.2-2.673c.645.552 1.33 1.064 2.05 1.534M8.513 9.91L5.815 7.753c.618-.79 1.29-1.54 2.008-2.248L10.218 8c-.61.602-1.18 1.24-1.705 1.91M33.526 52.256l.576 3.432c-1 .172-1.992.273-2.982.312l-.13-3.482c.84-.03 1.686-.118 2.536-.262M45.682 46.12c.642-.55 1.244-1.14 1.804-1.758l2.536 2.343c-.652.726-1.36 1.427-2.122 2.077-.015.008-.027.018-.037.03l-2.216-2.665c.012-.006.02-.02.035-.026M3.78 32.468l-3.365.704C.21 32.177.072 31.178 0 30.167l3.43-.253c.06.858.178 1.712.35 2.554M48.777 13.976l2.8-2.024c.583.828 1.103 1.69 1.575 2.588l-3.03 1.637c-.404-.763-.85-1.5-1.345-2.2M52.92 24.847l3.413-.447c.13 1.002.19 2.016.187 3.034l-3.437-.01c.005-.867-.05-1.726-.162-2.577M51.41 38.02l3.122 1.464c-.42.908-.903 1.795-1.446 2.663l-2.904-1.868c.458-.73.875-1.485 1.228-2.26M41.33 49.467l1.674 3.042c-.886.494-1.794.92-2.713 1.29l-1.265-3.23c.775-.313 1.55-.676 2.304-1.103M24.63 52.458l-.65 3.423c-.987-.192-1.955-.448-2.9-.775l1.083-3.294c.803.267 1.623.488 2.466.648M7.883 42.483l-2.808 2.01c-.578-.834-1.098-1.7-1.558-2.594l3.043-1.62c.393.773.833 1.5 1.323 2.203M3.59 21.067l-3.353-.773c.224-.982.514-1.954.865-2.902l3.22 1.21c-.293.805-.543 1.63-.732 2.465M18.014 45.423l-1.838 2.938c-.72-.46-1.404-.975-2.052-1.527l2.204-2.672c.535.45 1.096.877 1.686 1.263M8.017 31.693l-3.37.705c-.166-.844-.29-1.696-.347-2.558l3.433-.246c.046.703.146 1.404.284 2.1M11.59 12.43l-2.696-2.16c.525-.667 1.096-1.305 1.71-1.908l2.393 2.498c-.506.497-.974 1.02-1.408 1.57M45.752 21.024l3.188-1.307c.316.8.582 1.62.797 2.455l-3.33.872c-.175-.686-.394-1.362-.655-2.02M42.024 41.87c.533-.453 1.02-.94 1.48-1.445l2.54 2.346c-.562.616-1.156 1.207-1.807 1.762-.008.005-.02.02-.028.025L41.99 41.9c.01-.01.02-.025.034-.03M47.222 30.24l3.4.502c-.12.847-.3 1.697-.54 2.528l-3.305-.956c.195-.682.344-1.377.445-2.074M32.804 47.202l.58 3.43c-.845.146-1.692.236-2.537.266l-.124-3.48c.69-.023 1.383-.094 2.08-.216M43.713 17.584l2.8-2.023c.492.703.936 1.44 1.34 2.2l-3.036 1.634c-.33-.626-.697-1.23-1.104-1.81M47.01 25.368l3.412-.444c.108.85.162 1.71.157 2.577l-3.437-.006c.005-.715-.04-1.422-.132-2.126M46.214 35.343l3.12 1.46c-.354.775-.765 1.532-1.23 2.262L45.2 37.203c.38-.605.72-1.224 1.014-1.86M38.818 44.662l1.675 3.042c-.75.42-1.524.784-2.306 1.097l-1.267-3.23c.64-.266 1.282-.56 1.898-.908M25.656 47.637l-.645 3.413c-.845-.163-1.664-.375-2.465-.648l1.08-3.303c.663.223 1.343.398 2.03.537M11.563 39.994l-2.812 2.012c-.488-.707-.928-1.44-1.32-2.2l3.045-1.615c.32.622.684 1.233 1.085 1.814M7.657 22.08l-3.352-.777c.158-.685.36-1.364.607-2.027l3.222 1.206c-.19.522-.353 1.052-.474 1.594"
            + "M19.896 19.66l-2.693-2.16c.43-.554.902-1.076 1.41-1.57l2.39 2.498c-.395.384-.763.8-1.107 1.232M17.433 30.12l-3.372.703c-.14-.695-.236-1.4-.282-2.098l3.427-.26c.04.56.114 1.108.228 1.654M37.874 37.257c.42-.356.8-.732 1.16-1.127l2.54 2.345c-.458.507-.946.993-1.48 1.448-.01.007-.02.014-.035.022l-2.215-2.663c.007-.007.02-.015.03-.025M41.32 22.805l3.184-1.302c.26.655.48 1.326.66 2.015l-3.33.88c-.135-.547-.31-1.08-.515-1.593M31.836 40.12l.58 3.43c-.7.117-1.398.192-2.088.225l-.13-3.48c.546-.024 1.09-.08 1.638-.174M42.14 29.423l3.404.507c-.106.697-.25 1.393-.447 2.078l-3.31-.95c.157-.542.273-1.088.353-1.635M22.997 38.312l-1.837 2.94c-.593-.38-1.155-.802-1.69-1.254l2.205-2.672c.416.357.857.688 1.322.986M39.876 20.202l2.796-2.024c.405.578.77 1.182 1.1 1.808l-3.03 1.64c-.26-.493-.55-.972-.866-1.424M42.153 25.94l3.405-.447c.094.705.143 1.413.137 2.126l-3.444-.01c.01-.564-.027-1.12-.097-1.67M41.126 32.93l3.112 1.462c-.29.64-.626 1.26-1.01 1.862l-2.9-1.862c.3-.472.563-.962.798-1.46M35.86 38.697l1.676 3.04c-.62.345-1.256.646-1.895.903l-1.27-3.233c.504-.205 1.002-.44 1.49-.71M27.36 40.068l-.65 3.416c-.688-.132-1.366-.308-2.026-.537l1.08-3.3c.52.177 1.052.315 1.596.42M19.454 34.875l-2.812 2.012c-.4-.586-.763-1.187-1.086-1.816l3.044-1.616c.254.495.54.966.854 1.42M17.493 24.717l-3.355-.773c.158-.685.36-1.364.607-2.027l3.222 1.206c-.19.522-.353 1.052-.474 1.594M29.815 3.735l.003-3.48c1.014 0 2.014.063 3 .198l-.457 3.45c-.834-.112-1.685-.166-2.545-.168\"/>"
            + "</g>"
            + "<g transform=\"translate(62, 18)\" fill=\"#1A1918\" fill-rule=\"evenodd\">"
            + "<path d=\"M12.574 8.382H2.422v7.022H.21V.488H2.42v5.974h10.152V.488h2.213v14.916h-2.213V8.382M17.67.363h2.09v2.295h-2.09V.363zm0 4.09h2.09v10.95h-2.09V4.454zM32.1 6.064h-.04c-.813-1.252-2.465-1.86-4.26-1.86-2.904 0-5.974 1.588-5.974 5.663 0 4.05 3.07 5.642 5.994 5.642 1.42 0 3.07-.337 4.24-2.03h.04v1.59c0 1.795-1.105 3.302-4.07 3.302-1.983 0-3.26-.355-3.636-1.778h-2.09C22.745 19.622 25.46 20 27.82 20c4.492 0 6.37-1.59 6.37-5.515V4.455H32.1v1.61zm0 3.825c0 2.254-1.5 3.865-4.03 3.865-2.548 0-4.03-1.61-4.03-3.866 0-2.256 1.482-3.93 4.03-3.93 2.53 0 4.03 1.674 4.03 3.93zM36.614.363h2.09v5.744h.04c.983-1.528 2.715-2.027 4.283-2.027 2.633 0 4.594 1.128 4.594 3.866v7.458h-2.086V8.36c0-1.605-1.004-2.525-3.24-2.525-1.898 0-3.59 1.107-3.59 3.26v6.31h-2.09V.362M60.26 3.036h-.045l-4.113 12.368H53.66L48.75.488h2.402l3.82 12.118h.044L59.006.488h2.63l4.095 12.118h.043L69.578.488h2.293l-5.054 14.916h-2.422L60.26 3.036M72.997.363h2.09v2.295h-2.09V.363zm0 4.09h2.09v10.95h-2.09V4.454zM77.638 4.454h2.09v2.008h.04c.98-1.735 2.382-2.382 3.76-2.382.52 0 .796.022 1.19.123v2.26c-.522-.127-.918-.21-1.485-.21-2.066 0-3.505 1.213-3.505 3.594v5.557h-2.09V4.454M97.94 10.553v-.267c0-4.535-3.05-6.206-5.935-6.206-4.257 0-6.348 2.696-6.348 5.85 0 3.155 2.09 5.85 6.348 5.85 2.155 0 4.594-1.07 5.644-3.7H95.43c-.708 1.546-2.34 1.947-3.53 1.947-1.86 0-3.864-1.215-4.03-3.474H97.94zM87.934 8.928c.313-1.963 2.046-3.093 3.97-3.093 1.962 0 3.505 1.107 3.822 3.093h-7.792z\"/>"
            + "</g>"
            + "</svg>";

    /**
     * Builds a JournalMetadataRequest with demo default values from a saved config.
     * Any field enabled in the config gets populated with a demo placeholder value.
     */
    @SuppressWarnings("unchecked")
    public JournalMetadataRequest buildDemoRequest(String pubId, String jcode) {
        File configFile = new File(CONFIGS_DIR, "config_" + pubId + "_" + jcode + ".json");
        if (!configFile.exists()) {
            // No saved config — fall back to the default demo request
            log.info("No saved config for {}/{}, using default demo request", pubId, jcode);
            return buildDefaultDemoRequest(pubId, jcode);
        }

        try {
            String content = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
            Map<String, Object> rawConfig = objectMapper.readValue(content, Map.class);

            Map<String, DynamicStampRequest.Configuration> positions =
                    demoConfigGeneratorService.buildDemoPositions(rawConfig);

            return JournalMetadataRequest.builder()
                    .publisherId(pubId)
                    .jcode(jcode)
                    .env("demo")
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
     * Builds a default demo request using the journal_article template with the
     * HighWire logo and all metadata fields enabled. Used when no saved config exists
     * or for a generic demo preview.
     */
    public JournalMetadataRequest buildDefaultDemoRequest(String pubId, String jcode) {
        Map<String, DynamicStampRequest.Configuration> positions = new LinkedHashMap<>();

        // NEW_PAGE: journal_article template, front page, HighWire logo, all metadata on
        positions.put("NEW_PAGE", DynamicStampRequest.Configuration.builder()
                .templateName("journal_article")
                .pagePosition("front")
                .includeArticleTitle(true)
                .includeAuthors(true)
                .includeDoi(true)
                .includeDate(true)
                .includeCopyright(true)
                .includeIssn(true)
                .includeArticleId(true)
                .includeCurrentUser(true)
                .linkUrl("https://www.highwirepress.com")
                .linkText("Access Full Text Online")
                .build());

        // FOOTER with copyright
        positions.put("FOOTER", DynamicStampRequest.Configuration.builder()
                .text("© 2026 HighWire Press, Inc. All rights reserved.")
                .includeCopyright(true)
                .build());

        return JournalMetadataRequest.builder()
                .publisherId(pubId != null ? pubId : "demoPub")
                .jcode(jcode != null ? jcode : "demoJcode")
                .env("demo")
                .articleTitle(DEMO_ARTICLE_TITLE)
                .authors(DEMO_AUTHORS)
                .doiValue(DEMO_DOI)
                .articleCopyright(DEMO_COPYRIGHT)
                .articleIssn(DEMO_ISSN)
                .articleId(DEMO_ARTICLE_ID)
                .downloadedBy(DEMO_DOWNLOADED_BY)
                .positions(positions)
                .build();
    }

    /**
     * Returns the HighWire logo as an inline SVG string for embedding in HTML templates.
     */
    public static String getHighWireLogoSvg() {
        return HIGHWIRE_LOGO_SVG;
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
