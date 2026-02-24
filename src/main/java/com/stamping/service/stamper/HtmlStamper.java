package com.stamping.service.stamper;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.geom.AffineTransform;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfString;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.annot.PdfAnnotation;
import com.itextpdf.kernel.pdf.annot.PdfLinkAnnotation;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.stamping.exception.StampingException;
import com.stamping.model.StampPosition;
import com.stamping.model.StampRequest;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * Stamps a PDF with rendered HTML content using iText 7 pdfHTML.
 * HTML is first rendered to a temp PDF (which creates native link annotations),
 * then the content and annotations are transferred to the target PDF.
 */
@Component("htmlStamper")
public class HtmlStamper implements Stamper {

    @Override
    public byte[] stamp(byte[] pdfBytes, StampRequest request, byte[] stampContent) {
        if (stampContent == null || stampContent.length == 0) {
            throw new StampingException("HTML content is required for HTML stamp type");
        }

        try {
            String html = new String(stampContent, StandardCharsets.UTF_8);
            html = ensureHtml(html);

            // Step 1: Render HTML to PDF (iText creates native link annotations)
            byte[] htmlPdfBytes = renderHtmlToPdf(html, request);

            // Step 2: Overlay HTML PDF onto source PDF with proper annotation transfer
            return overlayHtmlPdf(pdfBytes, htmlPdfBytes, request);

        } catch (StampingException e) {
            throw e;
        } catch (Exception e) {
            throw new StampingException("Failed to apply HTML stamp: " + e.getMessage(), e);
        }
    }

    private byte[] renderHtmlToPdf(String html, StampRequest request) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ConverterProperties props = new ConverterProperties();

            if (request.getStampWidth() != null && request.getStampHeight() != null) {
                com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(os);
                com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(writer);
                pdfDoc.setDefaultPageSize(
                        new com.itextpdf.kernel.geom.PageSize(request.getStampWidth(), request.getStampHeight()));
                com.itextpdf.layout.Document document = HtmlConverter.convertToDocument(html, pdfDoc, props);
                document.close();
                return os.toByteArray();
            } else {
                HtmlConverter.convertToPdf(html, os, props);
                return os.toByteArray();
            }
        } catch (Exception e) {
            throw new StampingException("Failed to render HTML to PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Overlay HTML PDF onto source PDF, transferring both visual content and
     * annotations.
     */
    private byte[] overlayHtmlPdf(byte[] sourcePdfBytes, byte[] htmlPdfBytes, StampRequest request) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {

            PdfDocument sourceDoc = new PdfDocument(
                    new PdfReader(new ByteArrayInputStream(sourcePdfBytes)),
                    new PdfWriter(os));

            PdfDocument htmlDoc = new PdfDocument(
                    new PdfReader(new ByteArrayInputStream(htmlPdfBytes)));

            PdfPage htmlPage = htmlDoc.getPage(1);
            Rectangle htmlBox = htmlPage.getMediaBox();
            float htmlWidth = htmlBox.getWidth();
            float htmlHeight = htmlBox.getHeight();

            float scale = request.getScale();
            float scaledWidth = htmlWidth * scale;
            float scaledHeight = htmlHeight * scale;

            // Import HTML page as form XObject (visual content only)
            PdfFormXObject htmlForm = htmlPage.copyAsFormXObject(sourceDoc);

            // Get annotations from HTML page
            List<PdfAnnotation> htmlAnnotations = htmlPage.getAnnotations();

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

                // Draw visual content
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
                    transform.rotate(radians, htmlWidth / 2, htmlHeight / 2);
                }

                float[] m = new float[6];
                transform.getMatrix(m);
                pdfCanvas.concatMatrix(m[0], m[1], m[2], m[3], m[4], m[5]);

                pdfCanvas.addXObjectAt(htmlForm, 0, 0);
                pdfCanvas.restoreState();

                // Transfer link annotations with proper positioning
                for (PdfAnnotation annotation : htmlAnnotations) {
                    if (annotation instanceof PdfLinkAnnotation linkAnnot) {
                        Rectangle rect = linkAnnot.getRectangle().toRectangle();

                        // Scale the rectangle
                        // 1. Calculate unrotated coordinates relative to the stamp
                        float x = rect.getX() * scale;
                        float y = rect.getY() * scale;
                        float w = rect.getWidth() * scale;
                        float h = rect.getHeight() * scale;

                        // 2. Define the 4 corners of the unrotated rectangle
                        // (relative to the stamp's origin at 0,0)
                        float[][] corners = {
                                { x, y },
                                { x + w, y },
                                { x, y + h },
                                { x + w, y + h }
                        };

                        // 3. Rotate each corner
                        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
                        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

                        if (request.getRotation() != 0) {
                            double radians = Math.toRadians(request.getRotation());
                            float cos = (float) Math.cos(radians);
                            float sin = (float) Math.sin(radians);

                            // Center of rotation (center of the stamp)
                            float cx = scaledWidth / 2;
                            float cy = scaledHeight / 2;

                            for (float[] corner : corners) {
                                // Translate to origin (relative to center)
                                float dx = corner[0] - cx;
                                float dy = corner[1] - cy;

                                // Rotate
                                float rotX = dx * cos - dy * sin + cx;
                                float rotY = dx * sin + dy * cos + cy;

                                // Update bounds
                                minX = Math.min(minX, rotX);
                                minY = Math.min(minY, rotY);
                                maxX = Math.max(maxX, rotX);
                                maxY = Math.max(maxY, rotY);
                            }
                        } else {
                            minX = x;
                            minY = y;
                            maxX = x + w;
                            maxY = y + h;
                        }

                        // 4. Translate to final page position
                        minX += pos[0];
                        minY += pos[1];
                        maxX += pos[0];
                        maxY += pos[1];

                        // Extract URL from the source annotation
                        String url = extractUrl(linkAnnot);
                        if (url != null) {
                            // Create fresh link annotation in target document
                            Rectangle newRect = new Rectangle(minX, minY, maxX - minX, maxY - minY);
                            PdfLinkAnnotation newLink = new PdfLinkAnnotation(newRect);

                            if (request.getRotation() != 0) {
                                newLink.put(PdfName.Rotate,
                                        new com.itextpdf.kernel.pdf.PdfNumber((int) request.getRotation()));
                            }

                            newLink.setAction(PdfAction.createURI(url));

                            // Change the annotation border/highlight color to white to hide the default
                            // browser link box
                            newLink.setColor(com.itextpdf.kernel.colors.ColorConstants.WHITE);

                            // Set border width to 0 to make it invisible
                            newLink.setBorder(new com.itextpdf.kernel.pdf.PdfArray(new float[] { 0, 0, 0 }));

                            targetPage.addAnnotation(newLink);
                        }
                    }
                }
            }

            htmlDoc.close();
            sourceDoc.close();
            return os.toByteArray();

        } catch (Exception e) {
            throw new StampingException("Failed to stamp PDF with HTML: " + e.getMessage(), e);
        }
    }

    /**
     * Extract URL string from a link annotation's action dictionary
     */
    private String extractUrl(PdfLinkAnnotation linkAnnot) {
        try {
            PdfDictionary actionDict = linkAnnot.getPdfObject().getAsDictionary(PdfName.A);
            if (actionDict != null) {
                PdfString uriStr = actionDict.getAsString(PdfName.URI);
                if (uriStr != null) {
                    return uriStr.getValue();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    /**
     * Calculate position based on StampPosition and custom x/y
     */
    private float[] calculatePosition(StampRequest request, Rectangle pageBox,
            float stampWidth, float stampHeight) {
        float x, y;

        if (request.getPosition() == StampPosition.CUSTOM) {
            x = request.getX();
            y = request.getY();
        } else {
            float pageWidth = pageBox.getWidth();
            float pageHeight = pageBox.getHeight();

            switch (request.getPosition()) {
                case TOP_LEFT:
                    x = 0;
                    y = pageHeight - stampHeight;
                    break;
                case TOP_RIGHT:
                    x = pageWidth - stampWidth;
                    y = pageHeight - stampHeight;
                    break;
                case BOTTOM_LEFT:
                    x = 0;
                    y = 0;
                    break;
                case BOTTOM_RIGHT:
                    x = pageWidth - stampWidth;
                    y = 0;
                    break;
                case CENTER:
                    x = (pageWidth - stampWidth) / 2;
                    y = (pageHeight - stampHeight) / 2;
                    break;
                case HEADER:
                    x = (pageWidth - stampWidth) / 2;
                    y = pageHeight - stampHeight;
                    break;
                case FOOTER:
                    x = (pageWidth - stampWidth) / 2;
                    y = 0;
                    break;
                case LEFT_MARGIN:
                    x = 0;
                    y = (pageHeight - stampHeight) / 2;
                    break;
                case RIGHT_MARGIN:
                    x = pageWidth - stampWidth;
                    y = (pageHeight - stampHeight) / 2;
                    break;
                default:
                    x = 0;
                    y = 0;
            }
        }

        return new float[] { x, y };
    }

    /**
     * Determine which pages to stamp
     */
    private Set<Integer> determineTargetPages(StampRequest request, int totalPages) {
        String pagesSpec = request.getPages();
        if ("ALL".equalsIgnoreCase(pagesSpec)) {
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
                int pageNum = Integer.parseInt(part) - 1;
                if (pageNum >= 0 && pageNum < totalPages) {
                    pages.add(pageNum);
                }
            }
        }
        return pages;
    }

    /**
     * Ensure HTML has proper structure
     */
    private String ensureHtml(String html) {
        html = html.trim();
        if (html.toLowerCase().startsWith("<!doctype") || html.toLowerCase().startsWith("<html")) {
            return html;
        }
        return "<!DOCTYPE html>\n<html>\n<head><meta charset=\"UTF-8\"/></head>\n" +
                "<body>\n" + html + "\n</body>\n</html>";
    }
}
