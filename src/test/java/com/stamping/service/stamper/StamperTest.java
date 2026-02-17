package com.stamping.service.stamper;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.stamping.model.StampPosition;
import com.stamping.model.StampRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StamperTest {

    private TextStamper textStamper;
    private ImageStamper imageStamper;
    private byte[] emptyPdfBytes;

    @BeforeEach
    void setUp() throws Exception {
        textStamper = new TextStamper();
        imageStamper = new ImageStamper();

        // Create an empty PDF for testing
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfDocument pdfDoc = new PdfDocument(new PdfWriter(os));
            pdfDoc.addNewPage();
            pdfDoc.close();
            emptyPdfBytes = os.toByteArray();
        }
    }

    @Test
    void testTextStamping() throws Exception {
        StampRequest request = new StampRequest();
        request.setText("Hello World");
        request.setPosition(StampPosition.CENTER);
        request.setFontSize(12f);
        request.setPages("ALL");

        byte[] result = textStamper.stamp(emptyPdfBytes, request, null);
        assertNotNull(result);
        assertTrue(result.length > 0);

        // Optional: Save to file for manual inspection if needed
        // Files.write(new File("target/text_stamp_test.pdf").toPath(), result);
    }

    @Test
    void testImageStamping() throws Exception {
        // Create a dummy image (1x1 red pixel)
        // PNG header + simple data
        // Minimal valid PNG
        byte[] dummyImage = new byte[] {
                (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
                0x00, 0x00, 0x00, 0x0d, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53, (byte) 0xde,
                0x00, 0x00, 0x00, 0x0c, 0x49, 0x44, 0x41, 0x54,
                0x08, (byte) 0xd7, 0x63, (byte) 0xf8, (byte) 0xcf, (byte) 0xc0, 0x00, 0x00,
                0x03, 0x01, 0x01, 0x00, 0x18, (byte) 0xdd, (byte) 0x8d, (byte) 0xb0,
                0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4e, 0x44,
                (byte) 0xae, 0x42, 0x60, (byte) 0x82
        };

        StampRequest request = new StampRequest();
        request.setPosition(StampPosition.TOP_RIGHT);
        request.setScale(1.0f);
        request.setPages("1");

        byte[] result = imageStamper.stamp(emptyPdfBytes, request, dummyImage);
        assertNotNull(result);
        assertTrue(result.length > 0);

        // Files.write(new File("target/image_stamp_test.pdf").toPath(), result);
    }
}
