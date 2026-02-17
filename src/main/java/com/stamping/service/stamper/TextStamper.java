package com.stamping.service.stamper;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.layout.LayoutArea;
import com.itextpdf.layout.layout.LayoutContext;
import com.itextpdf.layout.layout.LayoutResult;
import com.itextpdf.layout.renderer.IRenderer;
import com.stamping.exception.StampingException;
import com.stamping.model.StampPosition;
import com.stamping.model.StampRequest;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

@Component("textStamper")
public class TextStamper implements Stamper {

    @Override
    public byte[] stamp(byte[] pdfBytes, StampRequest request, byte[] stampContent) {
        if (request.getText() == null || request.getText().isBlank()) {
            throw new StampingException("Text content is required for TEXT stamp type");
        }

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfDocument pdfDoc = new PdfDocument(
                    new PdfReader(new ByteArrayInputStream(pdfBytes)),
                    new PdfWriter(os));

            Set<Integer> targetPages = PageSelector.parsePages(request.getPages(), pdfDoc.getNumberOfPages());
            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            float fontSize = request.getFontSize();
            DeviceRgb color = parseColor(request.getFontColor());

            for (int pageIndex = 1; pageIndex <= pdfDoc.getNumberOfPages(); pageIndex++) {
                if (!targetPages.contains(pageIndex - 1)) {
                    continue;
                }

                PdfPage page = pdfDoc.getPage(pageIndex);
                Rectangle pageSize = page.getPageSize();
                PdfCanvas pdfCanvas = new PdfCanvas(page);

                // Create paragraph
                Paragraph p = new Paragraph(request.getText())
                        .setFont(font)
                        .setFontSize(fontSize)
                        .setFontColor(color);

                // Determine maximum width
                float maxWidth = request.getStampWidth() != null
                        ? request.getStampWidth()
                        : pageSize.getWidth() - 40f;

                // Measure content
                float contentHeight = measureParagraphHeight(p, maxWidth, pdfCanvas, pageSize);

                // Calculate position
                float[] pos = calculatePosition(request, pageSize, maxWidth, contentHeight);
                float x = pos[0];
                float y = pos[1];

                // Render
                pdfCanvas.saveState();

                // Apply opacity
                if (request.getOpacity() < 1.0f) {
                    PdfExtGState gs = new PdfExtGState();
                    gs.setFillOpacity(request.getOpacity());
                    gs.setStrokeOpacity(request.getOpacity());
                    pdfCanvas.setExtGState(gs);
                }

                // Handle rotation
                if (request.getRotation() != 0) {
                    double radians = Math.toRadians(request.getRotation());
                    // Rotate around center of the text block
                    float cx = x + maxWidth / 2;
                    float cy = y + contentHeight / 2;
                    pdfCanvas.concatMatrix(
                            Math.cos(radians), Math.sin(radians),
                            -Math.sin(radians), Math.cos(radians),
                            cx - (cx * Math.cos(radians) - cy * Math.sin(radians)),
                            cy - (cx * Math.sin(radians) + cy * Math.cos(radians)));
                }

                // Draw text in the calculated box
                // Note: The box's x,y is the bottom-left corner.
                Rectangle finalBox = new Rectangle(x, y, maxWidth, contentHeight);
                Canvas canvas = new Canvas(pdfCanvas, finalBox);
                canvas.add(p);
                canvas.close();

                pdfCanvas.restoreState();
            }

            pdfDoc.close();
            return os.toByteArray();

        } catch (IOException e) {
            throw new StampingException("Failed to decode font or read PDF", e);
        } catch (Exception e) {
            throw new StampingException("Failed to apply text stamp: " + e.getMessage(), e);
        }
    }

    private float measureParagraphHeight(Paragraph p, float width, PdfCanvas pdfCanvas, Rectangle pageSize) {
        // Create a dummy container to measure the paragraph
        // We use a large height to allow full wrapping
        Rectangle layoutBox = new Rectangle(0, 0, width, pageSize.getHeight());
        try (Canvas dummyCanvas = new Canvas(pdfCanvas, layoutBox)) {
            IRenderer renderer = p.createRendererSubTree().setParent(dummyCanvas.getRenderer());
            LayoutResult result = renderer.layout(new LayoutContext(new LayoutArea(1, layoutBox)));
            return result.getOccupiedArea().getBBox().getHeight();
        }
    }

    private float[] calculatePosition(StampRequest request, Rectangle pageSize, float contentWidth,
            float contentHeight) {
        float margin = 20f;
        StampPosition position = request.getPosition() != null ? request.getPosition() : StampPosition.CENTER;

        float pageW = pageSize.getWidth();
        float pageH = pageSize.getHeight();

        return switch (position) {
            case TOP_LEFT -> new float[] { margin, pageH - margin - contentHeight };
            case TOP_RIGHT -> new float[] { pageW - margin - contentWidth, pageH - margin - contentHeight };
            case BOTTOM_LEFT -> new float[] { margin, margin };
            case BOTTOM_RIGHT -> new float[] { pageW - margin - contentWidth, margin };
            case CENTER -> new float[] { (pageW - contentWidth) / 2, (pageH - contentHeight) / 2 };
            case CUSTOM -> new float[] {
                    request.getX() != null ? request.getX() : 0f,
                    request.getY() != null ? request.getY() : 0f };
        };
    }

    private DeviceRgb parseColor(String hex) {
        if (hex == null || hex.isBlank()) {
            return new DeviceRgb(0, 0, 0); // Black
        }
        hex = hex.startsWith("#") ? hex.substring(1) : hex;
        try {
            int rgb = Integer.parseInt(hex, 16);
            return new DeviceRgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        } catch (NumberFormatException e) {
            return new DeviceRgb(0, 0, 0);
        }
    }
}
