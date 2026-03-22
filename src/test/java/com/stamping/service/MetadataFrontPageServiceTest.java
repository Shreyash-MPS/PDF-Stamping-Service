package com.stamping.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;

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
    void testRenderHtmlToPdf_ProducesValidPdf() {
        String html = "<html><body><p>Test content</p></body></html>";
        byte[] result = metadataFrontPageService.renderHtmlToPdf(html, PageSize.A4);

        assertNotNull(result);
        assertTrue(result.length > 0, "Rendered PDF should not be empty");
    }

    @Test
    void testPrependPdf_AddsExtraPage() throws Exception {
        byte[] originalPdf = createMinimalPdf();
        byte[] prependPdf = createMinimalPdf();

        byte[] result = metadataFrontPageService.prependPdf(originalPdf, prependPdf);

        PdfDocument resultDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(result)));
        int resultPages = resultDoc.getNumberOfPages();
        resultDoc.close();

        assertEquals(2, resultPages, "Merged PDF should have 2 pages");
        assertTrue(result.length > originalPdf.length, "Output should be larger than input");
    }
}
