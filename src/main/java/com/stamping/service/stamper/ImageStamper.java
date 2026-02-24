package com.stamping.service.stamper;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.AffineTransform;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.stamping.exception.StampingException;
import com.stamping.model.StampPosition;
import com.stamping.model.StampRequest;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Set;

@Component("imageStamper")
public class ImageStamper implements Stamper {

    @Override
    public byte[] stamp(byte[] pdfBytes, StampRequest request, byte[] stampContent) {
        if (stampContent == null || stampContent.length == 0) {
            throw new StampingException("Image content is required for IMAGE stamp type");
        }

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfDocument pdfDoc = new PdfDocument(
                    new PdfReader(new ByteArrayInputStream(pdfBytes)),
                    new PdfWriter(os));

            Set<Integer> targetPages = PageSelector.parsePages(request.getPages(), pdfDoc.getNumberOfPages());

            // Create ImageData from bytes
            ImageData imageData = ImageDataFactory.create(stampContent);

            float scale = request.getScale() > 0 ? request.getScale() : 1.0f;
            float imgWidth = imageData.getWidth() * scale;
            float imgHeight = imageData.getHeight() * scale;

            for (int pageIndex = 1; pageIndex <= pdfDoc.getNumberOfPages(); pageIndex++) {
                if (!targetPages.contains(pageIndex - 1)) {
                    continue;
                }

                PdfPage page = pdfDoc.getPage(pageIndex);
                Rectangle pageSize = page.getPageSize();
                PdfCanvas pdfCanvas = new PdfCanvas(page);

                // Calculate position
                float[] pos = calculatePosition(request, pageSize, imgWidth, imgHeight);

                pdfCanvas.saveState();

                // Apply opacity
                if (request.getOpacity() < 1.0f) {
                    PdfExtGState gs = new PdfExtGState();
                    gs.setFillOpacity(request.getOpacity());
                    gs.setStrokeOpacity(request.getOpacity());
                    pdfCanvas.setExtGState(gs);
                }

                float x = pos[0];
                float y = pos[1];
                float origW = imageData.getWidth();
                float origH = imageData.getHeight();

                AffineTransform transform = new AffineTransform();
                transform.translate(x, y);
                transform.scale(scale, scale);

                if (request.getRotation() != 0) {
                    transform.rotate(Math.toRadians(request.getRotation()), origW / 2, origH / 2);
                }

                pdfCanvas.concatMatrix(transform);
                // Draw at native size (0,0 to origW, origH) - transform handles scaling and
                // position
                pdfCanvas.addImageAt(imageData, 0f, 0f, false);

                pdfCanvas.restoreState();
            }

            pdfDoc.close();
            return os.toByteArray();

        } catch (Exception e) {
            throw new StampingException("Failed to apply image stamp: " + e.getMessage(), e);
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
            case HEADER -> new float[] { (pageW - contentWidth) / 2, pageH - margin - contentHeight };
            case FOOTER -> new float[] { (pageW - contentWidth) / 2, margin };
            case LEFT_MARGIN -> new float[] { margin, (pageH - contentHeight) / 2 };
            case RIGHT_MARGIN -> new float[] { pageW - margin - contentWidth, (pageH - contentHeight) / 2 };
            case CUSTOM -> new float[] {
                    request.getX() != null ? request.getX() : 0f,
                    request.getY() != null ? request.getY() : 0f };
        };
    }
}
