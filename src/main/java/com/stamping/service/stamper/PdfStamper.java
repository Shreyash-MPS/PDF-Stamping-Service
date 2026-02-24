package com.stamping.service.stamper;

import com.itextpdf.kernel.geom.AffineTransform;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.stamping.exception.StampingException;
import com.stamping.model.StampPosition;
import com.stamping.model.StampRequest;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Set;

/**
 * Stamps a PDF with another PDF (first page).
 */
@Component("pdfStamper")
public class PdfStamper implements Stamper {

    @Override
    public byte[] stamp(byte[] pdfBytes, StampRequest request, byte[] stampContent) {
        if (stampContent == null || stampContent.length == 0) {
            throw new StampingException("PDF stamp content is required for PDF stamp type");
        }

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {

            PdfDocument sourceDoc = new PdfDocument(
                    new PdfReader(new ByteArrayInputStream(pdfBytes)),
                    new PdfWriter(os));

            PdfDocument stampDoc = new PdfDocument(
                    new PdfReader(new ByteArrayInputStream(stampContent)));

            // Extract first page of stamp PDF
            PdfPage stampPage = stampDoc.getPage(1);
            Rectangle stampBox = stampPage.getMediaBox();
            float stampWidth = stampBox.getWidth();
            float stampHeight = stampBox.getHeight();

            // Allow overriding stamp dimensions via request if provided (though aspect
            // ratio might be an issue,
            // usually we scale based on original dimensions)
            // But let's stick to scaling for now.

            float scale = request.getScale();
            float scaledWidth = stampWidth * scale;
            float scaledHeight = stampHeight * scale;

            // Import stamp page as form XObject
            PdfFormXObject stampForm = stampPage.copyAsFormXObject(sourceDoc);

            // Determine target pages
            Set<Integer> targetPages = determineTargetPages(request, sourceDoc.getNumberOfPages());

            for (int pageIndex = 0; pageIndex < sourceDoc.getNumberOfPages(); pageIndex++) {
                if (!targetPages.contains(pageIndex)) {
                    continue;
                }

                PdfPage targetPage = sourceDoc.getPage(pageIndex + 1);
                Rectangle pageBox = targetPage.getMediaBox();

                // Calculate position
                float[] pos = calculatePosition(request, pageBox, scaledWidth, scaledHeight);

                // Draw
                PdfCanvas pdfCanvas = new PdfCanvas(targetPage);
                pdfCanvas.saveState();

                // Apply opacity
                if (request.getOpacity() < 1.0f) {
                    com.itextpdf.kernel.pdf.extgstate.PdfExtGState gs = new com.itextpdf.kernel.pdf.extgstate.PdfExtGState();
                    gs.setFillOpacity(request.getOpacity());
                    gs.setStrokeOpacity(request.getOpacity());
                    pdfCanvas.setExtGState(gs);
                }

                // Build transformation
                AffineTransform transform = new AffineTransform();
                transform.translate(pos[0], pos[1]);

                if (scale != 1.0f) {
                    transform.scale(scale, scale);
                }

                if (request.getRotation() != 0) {
                    double radians = Math.toRadians(request.getRotation());
                    // Rotate around center of the stamp
                    transform.rotate(radians, stampWidth / 2, stampHeight / 2);
                }

                float[] m = new float[6];
                transform.getMatrix(m);
                pdfCanvas.concatMatrix(m[0], m[1], m[2], m[3], m[4], m[5]);

                pdfCanvas.addXObjectAt(stampForm, 0, 0);
                pdfCanvas.restoreState();
            }

            stampDoc.close();
            sourceDoc.close();
            return os.toByteArray();

        } catch (Exception e) {
            throw new StampingException("Failed to apply PDF stamp: " + e.getMessage(), e);
        }
    }

    private float[] calculatePosition(StampRequest request, Rectangle pageBox,
            float stampWidth, float stampHeight) {
        float x, y;

        if (request.getPosition() == StampPosition.CUSTOM) {
            x = request.getX() != null ? request.getX() : 0;
            y = request.getY() != null ? request.getY() : 0;
        } else {
            float pageWidth = pageBox.getWidth();
            float pageHeight = pageBox.getHeight();
            float margin = 20f; // Default margin

            switch (request.getPosition()) {
                case TOP_LEFT:
                    x = margin;
                    y = pageHeight - margin - stampHeight;
                    break;
                case TOP_RIGHT:
                    x = pageWidth - margin - stampWidth;
                    y = pageHeight - margin - stampHeight;
                    break;
                case BOTTOM_LEFT:
                    x = margin;
                    y = margin;
                    break;
                case BOTTOM_RIGHT:
                    x = pageWidth - margin - stampWidth;
                    y = margin;
                    break;
                case CENTER:
                    x = (pageWidth - stampWidth) / 2;
                    y = (pageHeight - stampHeight) / 2;
                    break;
                case HEADER:
                    x = (pageWidth - stampWidth) / 2;
                    y = pageHeight - margin - stampHeight;
                    break;
                case FOOTER:
                    x = (pageWidth - stampWidth) / 2;
                    y = margin;
                    break;
                case LEFT_MARGIN:
                    x = margin;
                    y = (pageHeight - stampHeight) / 2;
                    break;
                case RIGHT_MARGIN:
                    x = pageWidth - margin - stampWidth;
                    y = (pageHeight - stampHeight) / 2;
                    break;
                default:
                    x = 0;
                    y = 0;
            }
        }

        return new float[] { x, y };
    }

    private Set<Integer> determineTargetPages(StampRequest request, int totalPages) {
        // Reuse logic from PageSelector logic or duplicate strictly for now if
        // PageSelector isn't public/static usable
        // (Assuming PageSelector logic is similar to HtmlStamper's private method or
        // using the utility class)
        // Checked file list: PageSelector.java exists. Let's try to use it if adaptable
        // or just copy logic from HtmlStamper for consistency/speed.
        // HtmlStamper uses a private method. Let's check PageSelector content first to
        // see if we can reuse it.
        // Just kidding, I'll duplicate the logic for safety as I didn't verify
        // PageSelector API fully in this turn.
        // Actually, previous view showed PageSelector.parsePages usage in TextStamper.
        // Let's use that if possible?
        // Let's implement the logic directly to be safe and avoid compilation errors if
        // PageSelector returns different type.

        String pagesSpec = request.getPages();
        if ("ALL".equalsIgnoreCase(pagesSpec) || pagesSpec == null) {
            Set<Integer> pages = new java.util.HashSet<>();
            for (int i = 0; i < totalPages; i++) {
                pages.add(i);
            }
            return pages;
        }

        Set<Integer> pages = new java.util.HashSet<>();
        for (String part : pagesSpec.split(",")) {
            part = part.trim();
            if (part.contains("-")) {
                String[] range = part.split("-");
                int start = Integer.parseInt(range[0].trim()) - 1;
                int end = Integer.parseInt(range[1].trim()) - 1;
                for (int i = start; i <= end && i < totalPages; i++) {
                    pages.add(i);
                }
            } else {
                try {
                    int pageNum = Integer.parseInt(part) - 1;
                    if (pageNum >= 0 && pageNum < totalPages) {
                        pages.add(pageNum);
                    }
                } catch (NumberFormatException e) {
                    // ignore invalid
                }
            }
        }
        return pages;
    }
}
