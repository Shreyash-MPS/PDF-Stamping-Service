package com.stamping.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.stamping.model.MetadataFrontPageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class MetadataFrontPageServiceTest {

    private MetadataFrontPageService metadataFrontPageService;

    @BeforeEach
    void setUp() {
        metadataFrontPageService = new MetadataFrontPageService();
    }

    private byte[] createMinimalPdf() throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
            pdfDoc.addNewPage();
            pdfDoc.close();
            return baos.toByteArray();
        }
    }

    @Test
    void testPrependMetadataPage_Success() throws Exception {
        byte[] inputPdf = createMinimalPdf();

        MetadataFrontPageRequest request = MetadataFrontPageRequest.builder()
                .logoText("AJNR")
                .articleTitle("Consensus Statement")
                .authors("John Doe, Jane Doe")
                .addCurrentDate(true)
                .addDoi(true)
                .doi("10.1234/test")
                .citationText("J Testing 2026")
                .build();

        // Check input length
        PdfDocument originalDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(inputPdf)));
        int originalPages = originalDoc.getNumberOfPages(); // Should be 1
        originalDoc.close();

        // Execute
        byte[] result = metadataFrontPageService.prependMetadataPage(inputPdf, request);

        // Verify output length
        PdfDocument resultDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(result)));
        int resultPages = resultDoc.getNumberOfPages(); // Should be original + metadata page (1 + 1 = 2)
        resultDoc.close();

        assertEquals(originalPages + 1, resultPages, "Result PDF should have one extra metadata page");
        assertTrue(result.length > inputPdf.length, "Output PDF byte size should be larger");
    }
}
