package com.stamping.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.springframework.stereotype.Service;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.utils.PdfMerger;
import com.stamping.exception.StampingException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MetadataFrontPageService {

    public byte[] renderHtmlToPdf(String html, com.itextpdf.kernel.geom.Rectangle pageSize) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(os);
            PdfDocument pdfDoc = new PdfDocument(writer);
            pdfDoc.setDefaultPageSize(new com.itextpdf.kernel.geom.PageSize(pageSize));

            ConverterProperties props = new ConverterProperties();
            com.itextpdf.layout.font.FontProvider fontProvider =
                    new com.itextpdf.html2pdf.resolver.font.DefaultFontProvider(true, true, true);
            props.setFontProvider(fontProvider);
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

            merger.merge(appendDoc, 1, appendDoc.getNumberOfPages());
            merger.merge(originalDoc, 1, originalDoc.getNumberOfPages());

            appendDoc.close();
            originalDoc.close();
            resultDoc.close();

            return os.toByteArray();
        } catch (Exception e) {
            throw new StampingException("Failed to merge PDFs: " + e.getMessage(), e);
        }
    }

    public byte[] appendPdf(byte[] originalPdfBytes, byte[] newPageBytes) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfDocument resultDoc = new PdfDocument(new PdfWriter(os));
            PdfMerger merger = new PdfMerger(resultDoc);

            PdfDocument originalDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(originalPdfBytes)));
            PdfDocument newDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(newPageBytes)));

            merger.merge(originalDoc, 1, originalDoc.getNumberOfPages());
            merger.merge(newDoc, 1, newDoc.getNumberOfPages());

            originalDoc.close();
            newDoc.close();
            resultDoc.close();

            return os.toByteArray();
        } catch (Exception e) {
            throw new StampingException("Failed to append PDF page: " + e.getMessage(), e);
        }
    }
}
