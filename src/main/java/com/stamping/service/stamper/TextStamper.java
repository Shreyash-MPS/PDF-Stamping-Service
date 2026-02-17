package com.stamping.service.stamper;

import com.stamping.exception.StampingException;
import com.stamping.model.StampPosition;
import com.stamping.model.StampRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Set;

@Component("textStamper")
public class TextStamper implements Stamper {

    @Override
    public byte[] stamp(byte[] pdfBytes, StampRequest request, byte[] stampContent) {
        if (request.getText() == null || request.getText().isBlank()) {
            throw new StampingException("Text content is required for TEXT stamp type");
        }

        try (PDDocument document = PDDocument.load(pdfBytes)) {
            Set<Integer> targetPages = PageSelector.parsePages(request.getPages(), document.getNumberOfPages());
            PDType1Font font = PDType1Font.HELVETICA_BOLD;
            float fontSize = request.getFontSize();
            java.awt.Color color = parseColor(request.getFontColor());

            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                if (!targetPages.contains(pageIndex))
                    continue;

                PDPage page = document.getPage(pageIndex);
                PDRectangle mediaBox = page.getMediaBox();

                // Calculate text wrapping
                float maxWidth = request.getStampWidth() != null
                        ? request.getStampWidth()
                        : mediaBox.getWidth() - 40f; // Default margin consideration

                List<String> lines = wrapText(request.getText(), font, fontSize, maxWidth);

                // Calculate dimensions
                float maxLineWidth = 0;
                for (String line : lines) {
                    maxLineWidth = Math.max(maxLineWidth, font.getStringWidth(line) / 1000 * fontSize);
                }
                float leading = 1.2f * fontSize;
                float textHeight = lines.size() * leading; // Approximate height

                // Calculate position (bottom-left of the text block)
                float[] pos = calculatePosition(request, mediaBox, maxLineWidth, textHeight);

                try (PDPageContentStream cs = new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                    // Set opacity
                    if (request.getOpacity() < 1.0f) {
                        PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
                        gs.setNonStrokingAlphaConstant(request.getOpacity());
                        gs.setStrokingAlphaConstant(request.getOpacity());
                        cs.setGraphicsStateParameters(gs);
                    }

                    cs.beginText();
                    cs.setFont(font, fontSize);
                    cs.setNonStrokingColor(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f);
                    cs.setLeading(leading);

                    // Apply rotation if needed
                    // We calculate the center of the text block for rotation
                    float centerX = pos[0] + maxLineWidth / 2;
                    float centerY = pos[1] + textHeight / 2;

                    // Start position for the FIRST line (top-left of text block, adjusted for
                    // baseline)
                    // pos[1] is bottom y. Top y is pos[1] + textHeight.
                    // First line baseline is roughly top y - fontSize (or simply handled by moving
                    // to top and usingnewLine)

                    // Actually, simpler approach:
                    // matrix translate to (pos[0], pos[1] + textHeight - leading) -> Top line
                    // baseline
                    // Then showText, newLine...

                    float startX = pos[0];
                    float startY = pos[1] + textHeight - leading; // Move to top line baseline

                    // Adjust centering for each line if needed?
                    // Current request is just wrapping, let's assume left alignment within the
                    // block for now due to complexity
                    // or center alignment if Position is CENTER?
                    // The block is positioned. Text inside the block: let's left align for now.

                    if (request.getRotation() != 0) {
                        double radians = Math.toRadians(request.getRotation());
                        cs.setTextMatrix(org.apache.pdfbox.util.Matrix.getRotateInstance(radians, centerX, centerY));
                        // After rotating around center, we need to offset to the start of the text
                        // relative to center
                        // Relative start: -width/2, +height/2 - leading
                        cs.newLineAtOffset(-maxLineWidth / 2, textHeight / 2 - leading);
                    } else {
                        cs.newLineAtOffset(startX, startY);
                    }

                    for (String line : lines) {
                        cs.showText(line);
                        cs.newLine();
                    }

                    cs.endText();
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();

        } catch (StampingException e) {
            throw e;
        } catch (Exception e) {
            throw new StampingException("Failed to apply text stamp: " + e.getMessage(), e);
        }
    }

    private java.util.List<String> wrapText(String text, PDType1Font font, float fontSize, float maxWidth)
            throws java.io.IOException {
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() > 0) {
                // Check if adding space + word exceeds width
                String potential = currentLine + " " + word;
                float width = font.getStringWidth(potential) / 1000 * fontSize;
                if (width > maxWidth) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine.append(" ").append(word);
                }
            } else {
                currentLine.append(word);
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
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

    private java.awt.Color parseColor(String hex) {
        if (hex == null || hex.isBlank()) {
            return java.awt.Color.BLACK;
        }
        hex = hex.startsWith("#") ? hex.substring(1) : hex;
        return new java.awt.Color(Integer.parseInt(hex, 16));
    }
}
