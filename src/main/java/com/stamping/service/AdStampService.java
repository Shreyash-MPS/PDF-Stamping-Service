package com.stamping.service;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.utils.PdfMerger;
import com.stamping.exception.StampingException;
import com.stamping.model.StampPosition;
import com.stamping.model.StampRequest;
import com.stamping.model.StampType;
import com.stamping.service.stamper.Stamper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Slf4j
@Service
public class AdStampService {

    private final AdFetchService adFetchService;
    private final Stamper htmlStamper;

    public AdStampService(AdFetchService adFetchService, @Qualifier("htmlStamper") Stamper htmlStamper) {
        this.adFetchService = adFetchService;
        this.htmlStamper = htmlStamper;
    }

    /**
     * Processes ad JSON and applies ads based on positionName.
     * 
     * @param pdfBytes  Original PDF document bytes
     * @param adJsonUrl URL to fetch ad configurations from
     * @param adType    Which ad types to process ("header", "pdf ad one", or "all")
     * @return PDF bytes with ads applied
     */
    public byte[] processAdJson(byte[] pdfBytes, String adJsonUrl, String adType) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new StampingException("PDF file is required");
        }

        log.info("Fetching ads from URL for processAdJson: {}", adJsonUrl);
        var adResponse = adFetchService.fetchAds(adJsonUrl);

        byte[] currentPdfBytes = pdfBytes;

        String pdfAdOneHtml = null;

        if (adResponse.getSection() != null) {
            for (var section : adResponse.getSection()) {
                if (section.getAdLocation() != null) {
                    for (var location : section.getAdLocation()) {

                        // 1. Check for "header" ad (In-Page Stamping)
                        if (("all".equalsIgnoreCase(adType) || "header".equalsIgnoreCase(adType))
                                && "header".equalsIgnoreCase(location.getPositionName())
                                && location.getAdData() != null) {

                            for (var ad : location.getAdData()) {
                                String htmlContent = ad.getAdHtml();
                                if (htmlContent != null && !htmlContent.isEmpty()) {
                                    htmlContent = processHtmlContent(htmlContent);

                                    log.info("Applying header ad stamp: id={}", ad.getAdId());

                                    StampRequest request = new StampRequest();
                                    request.setStampType(StampType.HTML);
                                    request.setPosition(StampPosition.TOP_RIGHT); // Ensure standard header placement

                                    currentPdfBytes = htmlStamper.stamp(currentPdfBytes, request,
                                            htmlContent.getBytes());
                                }
                            }
                        }

                        // 2. Check for "pdf ad one" ad (Whole Page Prepend)
                        if (("all".equalsIgnoreCase(adType) || "pdf ad one".equalsIgnoreCase(adType))
                                && "pdf ad one".equalsIgnoreCase(location.getPositionName())
                                && location.getAdData() != null) {

                            for (var ad : location.getAdData()) {
                                if (ad.getAdHtml() != null && !ad.getAdHtml().isEmpty()) {
                                    pdfAdOneHtml = ad.getAdHtml();
                                    break; // Take the first valid HTML string
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Process the "pdf ad one" page if found.
        if (pdfAdOneHtml != null && !pdfAdOneHtml.isEmpty()) {
            pdfAdOneHtml = processHtmlContent(pdfAdOneHtml);
            pdfAdOneHtml = ensureHtml(pdfAdOneHtml);

            try {
                // Get original page size
                com.itextpdf.kernel.geom.Rectangle pageSize;
                try (PdfDocument tempOriginal = new PdfDocument(
                        new PdfReader(new ByteArrayInputStream(currentPdfBytes)))) {
                    pageSize = tempOriginal.getPage(1).getPageSize();
                }

                // Convert HTML to PDF using target page size
                byte[] htmlPdfBytes = renderHtmlToPdf(pdfAdOneHtml, pageSize);

                // Prepend HTML PDF to current state of PDF
                currentPdfBytes = prependPdf(currentPdfBytes, htmlPdfBytes);

            } catch (Exception e) {
                throw new StampingException("Failed to prepend pdf ad one page: " + e.getMessage(), e);
            }
        } else if (("all".equalsIgnoreCase(adType) || "pdf ad one".equalsIgnoreCase(adType))) {
            log.info("No 'pdf ad one' HTML content found in request, skipping page prepend.");
        }

        return currentPdfBytes;
    }

    private String processHtmlContent(String htmlContent) {
        // Fix relative paths for images and links
        String baseUrl = "https://hwmaint.genome.cshlp.org/adsystem/";
        htmlContent = htmlContent.replace("src=\"/", "src=\"" + baseUrl);
        htmlContent = htmlContent.replace("href=\"/", "href=\"" + baseUrl);
        return htmlContent;
    }

    private byte[] renderHtmlToPdf(String html, com.itextpdf.kernel.geom.Rectangle pageSize) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(os);
            PdfDocument pdfDoc = new PdfDocument(writer);
            pdfDoc.setDefaultPageSize(new com.itextpdf.kernel.geom.PageSize(pageSize));

            ConverterProperties props = new ConverterProperties();
            com.itextpdf.layout.Document document = HtmlConverter.convertToDocument(html, pdfDoc, props);
            document.close();

            return os.toByteArray();
        } catch (Exception e) {
            throw new StampingException("Failed to render HTML to PDF: " + e.getMessage(), e);
        }
    }

    private byte[] prependPdf(byte[] originalPdfBytes, byte[] appendPdfBytes) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfDocument resultDoc = new PdfDocument(new PdfWriter(os));
            PdfMerger merger = new PdfMerger(resultDoc);

            PdfDocument appendDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(appendPdfBytes)));
            PdfDocument originalDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(originalPdfBytes)));

            // Merge the append document (ad page) first
            merger.merge(appendDoc, 1, appendDoc.getNumberOfPages());

            // Merge the original document next
            merger.merge(originalDoc, 1, originalDoc.getNumberOfPages());

            appendDoc.close();
            originalDoc.close();
            resultDoc.close();

            return os.toByteArray();
        } catch (Exception e) {
            throw new StampingException("Failed to merge PDFs: " + e.getMessage(), e);
        }
    }

    private String ensureHtml(String html) {
        String trimmed = html.trim();
        if (trimmed.toLowerCase().startsWith("<!doctype") || trimmed.toLowerCase().startsWith("<html")) {
            return html;
        }
        return "<!DOCTYPE html>\n<html>\n<head><meta charset=\"UTF-8\"/></head>\n" +
                "<body>\n" + html + "\n</body>\n</html>";
    }
}
