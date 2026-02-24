package com.stamping.service;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.utils.PdfMerger;
import com.stamping.exception.StampingException;
import com.stamping.model.MetadataFrontPageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class MetadataFrontPageService {

    public byte[] prependMetadataPage(byte[] pdfBytes, MetadataFrontPageRequest request) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new StampingException("PDF file is required");
        }

        try {
            // Build the HTML for the front page
            String htmlContent = buildHtml(request);

            // Get original page size to match the generated page
            com.itextpdf.kernel.geom.Rectangle pageSize;
            try (PdfDocument tempOriginal = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfBytes)))) {
                pageSize = tempOriginal.getPage(1).getPageSize();
            }

            // Convert HTML to PDF
            byte[] pagePdfBytes = renderHtmlToPdf(htmlContent, pageSize);

            // Prepend new page to original PDF
            return prependPdf(pdfBytes, pagePdfBytes);

        } catch (Exception e) {
            throw new StampingException("Failed to prepend metadata page: " + e.getMessage(), e);
        }
    }

    private String buildHtml(MetadataFrontPageRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/></head>");
        sb.append("<body style=\"margin: 50px; font-family: 'Times New Roman', Times, serif;\">");

        sb.append("<table style=\"width: 100%; border-collapse: collapse; table-layout: fixed;\">");

        // Row 1: Logo (Left) and Title (Right)
        sb.append("<tr>");
        // Left column top (Logo)
        sb.append("<td style=\"width: 25%; vertical-align: top; padding-right: 20px;\">");
        if (request.getLogoUrl() != null && !request.getLogoUrl().isBlank()) {
            sb.append("<img src=\"").append(request.getLogoUrl()).append("\" style=\"max-width: 100%;\" />");
        } else if (request.getLogoText() != null && !request.getLogoText().isBlank()) {
            sb.append(
                    "<h1 style=\"color: #002D62; font-family: Arial, sans-serif; font-size: 64px; margin-top: 0; font-weight: bold;\">")
                    .append(request.getLogoText()).append("</h1>");
        }
        sb.append("</td>");

        // Right column top (Title)
        sb.append("<td style=\"width: 75%; vertical-align: top;\">");
        if (request.getArticleTitle() != null && !request.getArticleTitle().isBlank()) {
            // Replace newlines with <br> to respect manual line breaks
            String titleHtml = request.getArticleTitle().replace("\n", "<br/>");
            sb.append("<h2 style=\"font-size: 24px; font-weight: bold; margin-top: 5px; line-height: 1.2;\">")
                    .append(titleHtml).append("</h2>");
        }
        sb.append("</td>");
        sb.append("</tr>");

        // Row 2: Date (Left) and Authors / DOI (Right)
        sb.append("<tr>");
        // Left column middle (Date)
        sb.append("<td style=\"width: 25%; vertical-align: top; padding-right: 20px; padding-top: 40px;\">");
        if (request.isAddCurrentDate()) {
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
            sb.append("<p style=\"font-size: 16px;\">This information is current as<br/>of ")
                    .append(dateStr).append(".</p>");
        }
        sb.append("</td>");

        // Right column middle (Authors / DOI / Citation)
        sb.append("<td style=\"width: 75%; vertical-align: top; padding-top: 40px;\">");
        if (request.getAuthors() != null && !request.getAuthors().isBlank()) {
            sb.append("<p style=\"font-size: 18px; line-height: 1.5; margin-top: 0;\">")
                    .append(request.getAuthors()).append("</p>");
        }

        // Add padding between authors and citation info
        sb.append("<div style=\"margin-top: 40px; font-size: 16px;\">");

        if (request.getCitationText() != null && !request.getCitationText().isBlank()) {
            sb.append("<p style=\"margin-bottom: 5px;\"><i>")
                    .append(request.getCitationText()).append("</i></p>");
        }

        if (request.isAddDoi() && request.getDoi() != null && !request.getDoi().isBlank()) {
            String doiUrl = request.getDoi().startsWith("http") ? request.getDoi()
                    : "https://doi.org/" + request.getDoi();
            sb.append("<p style=\"margin: 0; color: blue;\">doi: ")
                    .append("<a href=\"").append(doiUrl).append("\" style=\"color: blue; text-decoration: none;\">")
                    .append(doiUrl).append("</a></p>");
        }

        if (request.getAdditionalLink() != null && !request.getAdditionalLink().isBlank()) {
            sb.append("<p style=\"margin: 0; color: blue;\">")
                    .append("<a href=\"").append(request.getAdditionalLink())
                    .append("\" style=\"color: blue; text-decoration: none;\">")
                    .append(request.getAdditionalLink()).append("</a></p>");
        }

        sb.append("</div>");
        sb.append("</td>");
        sb.append("</tr>");

        sb.append("</table>");
        sb.append("</body></html>");

        return sb.toString();
    }

    public byte[] renderHtmlToPdf(String html, com.itextpdf.kernel.geom.Rectangle pageSize) {
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

    public byte[] prependPdf(byte[] originalPdfBytes, byte[] appendPdfBytes) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfDocument resultDoc = new PdfDocument(new PdfWriter(os));
            PdfMerger merger = new PdfMerger(resultDoc);

            PdfDocument appendDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(appendPdfBytes)));
            PdfDocument originalDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(originalPdfBytes)));

            // Merge the append document (front page) first
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
}
