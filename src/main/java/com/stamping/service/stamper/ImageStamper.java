package com.stamping.service.stamper;

import com.stamping.exception.StampingException;
import com.stamping.model.StampPosition;
import com.stamping.model.StampRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Set;

@Component("imageStamper")
public class ImageStamper implements Stamper {

    @Override
    public byte[] stamp(byte[] pdfBytes, StampRequest request, byte[] stampContent) {
        if (stampContent == null || stampContent.length == 0) {
            throw new StampingException("Image content is required for IMAGE stamp type");
        }

        try (PDDocument document = PDDocument.load(pdfBytes)) {
            Set<Integer> targetPages = PageSelector.parsePages(request.getPages(), document.getNumberOfPages());
            PDImageXObject image = PDImageXObject.createFromByteArray(document, stampContent, "stamp");

            float scale = request.getScale();
            float imgWidth = image.getWidth() * scale;
            float imgHeight = image.getHeight() * scale;

            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                if (!targetPages.contains(pageIndex))
                    continue;

                PDPage page = document.getPage(pageIndex);
                PDRectangle mediaBox = page.getMediaBox();

                float[] pos = calculatePosition(request, mediaBox, imgWidth, imgHeight);

                try (PDPageContentStream cs = new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                    // Set opacity
                    if (request.getOpacity() < 1.0f) {
                        PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
                        gs.setNonStrokingAlphaConstant(request.getOpacity());
                        gs.setStrokingAlphaConstant(request.getOpacity());
                        cs.setGraphicsStateParameters(gs);
                    }

                    // Apply rotation if needed
                    if (request.getRotation() != 0) {
                        cs.saveGraphicsState();
                        float centerX = pos[0] + imgWidth / 2;
                        float centerY = pos[1] + imgHeight / 2;
                        double radians = Math.toRadians(request.getRotation());
                        float cos = (float) Math.cos(radians);
                        float sin = (float) Math.sin(radians);

                        // Translate to center, rotate, translate back
                        cs.transform(org.apache.pdfbox.util.Matrix.getTranslateInstance(centerX, centerY));
                        cs.transform(org.apache.pdfbox.util.Matrix.getRotateInstance(radians, 0, 0));
                        cs.transform(org.apache.pdfbox.util.Matrix.getTranslateInstance(-imgWidth / 2, -imgHeight / 2));
                        cs.drawImage(image, 0, 0, imgWidth, imgHeight);
                        cs.restoreGraphicsState();
                    } else {
                        cs.drawImage(image, pos[0], pos[1], imgWidth, imgHeight);
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();

        } catch (StampingException e) {
            throw e;
        } catch (Exception e) {
            throw new StampingException("Failed to apply image stamp: " + e.getMessage(), e);
        }
    }

    private float[] calculatePosition(StampRequest request, PDRectangle mediaBox,
            float contentWidth, float contentHeight) {
        float margin = 20f;
        StampPosition position = request.getPosition() != null ? request.getPosition() : StampPosition.CENTER;

        return switch (position) {
            case TOP_LEFT -> new float[] { margin, mediaBox.getHeight() - margin - contentHeight };
            case TOP_RIGHT -> new float[] { mediaBox.getWidth() - margin - contentWidth,
                    mediaBox.getHeight() - margin - contentHeight };
            case BOTTOM_LEFT -> new float[] { margin, margin };
            case BOTTOM_RIGHT -> new float[] { mediaBox.getWidth() - margin - contentWidth, margin };
            case CENTER -> new float[] { (mediaBox.getWidth() - contentWidth) / 2,
                    (mediaBox.getHeight() - contentHeight) / 2 };
            case CUSTOM -> new float[] {
                    request.getX() != null ? request.getX() : 0f,
                    request.getY() != null ? request.getY() : 0f };
        };
    }
}
