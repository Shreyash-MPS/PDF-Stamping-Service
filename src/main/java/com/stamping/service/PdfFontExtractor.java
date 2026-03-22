package com.stamping.service;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfStream;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts embedded font information from a PDF document.
 * Used to ensure stamped content uses the same font family as the original PDF,
 * maintaining PDF compliance and visual consistency.
 */
@Slf4j
@Service
public class PdfFontExtractor {

    /**
     * Result of font extraction from a PDF.
     */
    public static class FontInfo {
        private final String fontFamily;   // CSS-friendly font family name (e.g. "Times New Roman")
        private final String baseFontName; // Raw PDF BaseFont name (e.g. "TimesNewRomanPSMT")
        private final byte[] fontBytes;    // Embedded font program bytes (null if not embedded)
        private final String format;       // "truetype" or "opentype"
        private final boolean subset;      // true if font is a subset (XXXXXX+ prefix) — incomplete glyph set

        public FontInfo(String fontFamily, String baseFontName, byte[] fontBytes, String format, boolean subset) {
            this.fontFamily = fontFamily;
            this.baseFontName = baseFontName;
            this.fontBytes = fontBytes;
            this.format = format;
            this.subset = subset;
        }

        public String getFontFamily() { return fontFamily; }
        public String getBaseFontName() { return baseFontName; }
        public byte[] getFontBytes() { return fontBytes; }
        public String getFormat() { return format; }
        public boolean isSubset() { return subset; }
        public boolean isEmbedded() { return fontBytes != null && fontBytes.length > 0; }
        /** True only if the font is embedded AND has a full glyph set (not a subset) */
        public boolean isUsableForStamping() { return isEmbedded() && !subset; }
    }

    /**
     * Extracts the primary font from the first page of the PDF.
     * Returns the first non-standard font found, preferring embedded fonts.
     *
     * @param pdfBytes the PDF document bytes
     * @return FontInfo with font details, or null if no usable font found
     */
    public FontInfo extractPrimaryFont(byte[] pdfBytes) {
        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfBytes)))) {
            int totalPages = pdfDoc.getNumberOfPages();
            int pagesToScan = Math.min(totalPages, 3);

            Map<String, FontInfo> allFonts = new LinkedHashMap<>();
            FontInfo bestUsable = null;   // embedded + non-subset (can inject via @font-face)
            FontInfo firstEmbedded = null; // any embedded (even subset — use family name only)

            for (int pageNum = 1; pageNum <= pagesToScan; pageNum++) {
                PdfDictionary resources = pdfDoc.getPage(pageNum).getResources().getPdfObject();
                PdfDictionary fontDict = resources.getAsDictionary(PdfName.Font);
                if (fontDict == null) {
                    log.debug("  Page {}: no /Font dictionary in resources", pageNum);
                    continue;
                }

                for (PdfName key : fontDict.keySet()) {
                    PdfDictionary font = fontDict.getAsDictionary(key);
                    if (font == null) continue;

                    PdfName baseFont = font.getAsName(PdfName.BaseFont);
                    if (baseFont == null) continue;

                    String baseFontName = baseFont.getValue();

                    boolean isStandard = isStandardFont(baseFontName);
                    PdfName subtype = font.getAsName(PdfName.Subtype);
                    String subtypeStr = subtype != null ? subtype.getValue() : "unknown";

                    if (isStandard) {
                        log.debug("  Page {} font {}: {} [{}] (standard, skipped)",
                                pageNum, key.getValue(), baseFontName, subtypeStr);
                        continue;
                    }

                    if (allFonts.containsKey(baseFontName)) continue;

                    byte[] embeddedBytes = extractEmbeddedFontBytes(font);
                    String format = detectFormat(font);
                    String fontFamily = toFontFamily(baseFontName);
                    boolean isSubset = isSubsetFont(baseFontName);

                    FontInfo info = new FontInfo(fontFamily, baseFontName, embeddedBytes, format, isSubset);
                    allFonts.put(baseFontName, info);

                    log.info("  Page {} font {}: {} -> '{}' [{}] embedded={} subset={} {}",
                            pageNum, key.getValue(), baseFontName, fontFamily, format,
                            info.isEmbedded(), isSubset,
                            info.isEmbedded() ? "(" + (embeddedBytes.length / 1024) + " KB)" : "");

                    if (firstEmbedded == null && info.isEmbedded()) {
                        firstEmbedded = info;
                    }
                    if (bestUsable == null && info.isUsableForStamping()) {
                        bestUsable = info;
                    }
                }
            }

            // Priority: non-subset embedded > any embedded > first non-standard name
            if (bestUsable != null) {
                log.info("  Selected font (full, embedded): {} (family: '{}')",
                        bestUsable.getBaseFontName(), bestUsable.getFontFamily());
                return bestUsable;
            }

            if (firstEmbedded != null) {
                log.info("  Selected font (subset, name-only for CSS): {} (family: '{}')",
                        firstEmbedded.getBaseFontName(), firstEmbedded.getFontFamily());
                return firstEmbedded;
            }

            if (!allFonts.isEmpty()) {
                FontInfo first = allFonts.values().iterator().next();
                log.info("  No embedded fonts found. Using name-only: {} (family: '{}')",
                        first.getBaseFontName(), first.getFontFamily());
                return first;
            }

            log.info("  No non-standard fonts found in first {} pages", pagesToScan);
            return null;
        } catch (Exception e) {
            log.warn("Failed to extract fonts from PDF: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generates a CSS @font-face + font-family override snippet to inject into HTML
     * before rendering. If the font is embedded, includes a base64 data URI src.
     * If not embedded, just returns the font-family name for CSS use.
     *
     * @param fontInfo extracted font info
     * @return CSS style block string, or empty string if fontInfo is null
     */
    public String buildFontCss(FontInfo fontInfo) {
        if (fontInfo == null) return "";

        StringBuilder css = new StringBuilder();
        css.append("<style>\n");

        // Only inject @font-face if the font is fully embedded (not a subset)
        // Subset fonts only contain glyphs used in the original PDF — they'll fail
        // when iText tries to render new text that needs missing glyphs
        if (fontInfo.isUsableForStamping()) {
            String mimeType = "truetype".equals(fontInfo.getFormat()) ? "font/ttf" : "font/otf";
            String base64 = Base64.getEncoder().encodeToString(fontInfo.getFontBytes());
            css.append("@font-face {\n");
            css.append("  font-family: '").append(fontInfo.getFontFamily()).append("';\n");
            css.append("  src: url('data:").append(mimeType).append(";base64,").append(base64).append("');\n");
            css.append("}\n");
        }

        css.append("body { font-family: '").append(fontInfo.getFontFamily()).append("', Verdana, Arial, Helvetica, sans-serif; }\n");
        css.append("</style>\n");

        return css.toString();
    }

    /**
     * Injects font CSS into an HTML string's <head> section.
     */
    public String injectFontIntoHtml(String html, FontInfo fontInfo) {
        if (fontInfo == null || html == null) return html;

        String fontCss = buildFontCss(fontInfo);
        if (fontCss.isEmpty()) return html;

        // Inject after <head> or after <meta charset>
        if (html.contains("</head>")) {
            return html.replace("</head>", fontCss + "</head>");
        } else if (html.contains("<body")) {
            return html.replace("<body", fontCss + "<body");
        }
        return fontCss + html;
    }

    // ─── Private helpers ────────────────────────────────────────────────

    private byte[] extractEmbeddedFontBytes(PdfDictionary fontDict) {
        // Check FontDescriptor → FontFile / FontFile2 / FontFile3
        PdfDictionary descriptor = fontDict.getAsDictionary(PdfName.FontDescriptor);
        if (descriptor == null) {
            // Type0 (CIDFont) — check DescendantFonts
            var descendants = fontDict.getAsArray(PdfName.DescendantFonts);
            if (descendants != null && descendants.size() > 0) {
                PdfDictionary cidFont = descendants.getAsDictionary(0);
                if (cidFont != null) {
                    descriptor = cidFont.getAsDictionary(PdfName.FontDescriptor);
                }
            }
        }
        if (descriptor == null) return null;

        // FontFile2 = TrueType, FontFile3 = OpenType/CFF, FontFile = Type1
        PdfStream stream = descriptor.getAsStream(PdfName.FontFile2);
        if (stream == null) stream = descriptor.getAsStream(PdfName.FontFile3);
        if (stream == null) stream = descriptor.getAsStream(new PdfName("FontFile"));
        if (stream == null) return null;

        return stream.getBytes();
    }

    private String detectFormat(PdfDictionary fontDict) {
        PdfDictionary descriptor = fontDict.getAsDictionary(PdfName.FontDescriptor);
        if (descriptor == null) return "truetype";

        if (descriptor.getAsStream(PdfName.FontFile2) != null) return "truetype";
        if (descriptor.getAsStream(PdfName.FontFile3) != null) return "opentype";
        return "truetype";
    }

    /**
     * Converts a PDF BaseFont name to a CSS-friendly font family.
     * E.g. "TimesNewRomanPSMT" → "Times New Roman"
     *      "ArialMT" → "Arial"
     *      "BCDFGH+Helvetica-Bold" → "Helvetica"
     */
    private String toFontFamily(String baseFontName) {
        // Strip subset prefix (e.g. "BCDFGH+")
        if (baseFontName.length() > 7 && baseFontName.charAt(6) == '+') {
            baseFontName = baseFontName.substring(7);
        }
        // Strip style suffixes
        String name = baseFontName
                .replace("PSMT", "")
                .replace("PSMTBold", "")
                .replace("MT", "")
                .replaceAll("[-,](Bold|Italic|BoldItalic|Regular|Light|Medium|Semibold|ExtraBold)$", "")
                .replaceAll("[-,](Bold|Italic|BoldItalic|Regular)$", "");

        // Insert spaces before capitals: "TimesNewRoman" → "Times New Roman"
        name = name.replaceAll("([a-z])([A-Z])", "$1 $2");

        return name.trim();
    }

    private boolean isStandardFont(String baseFontName) {
        // PDF standard 14 fonts — never embedded, always available
        return switch (baseFontName) {
            case "Courier", "Courier-Bold", "Courier-Oblique", "Courier-BoldOblique",
                 "Helvetica", "Helvetica-Bold", "Helvetica-Oblique", "Helvetica-BoldOblique",
                 "Times-Roman", "Times-Bold", "Times-Italic", "Times-BoldItalic",
                 "Symbol", "ZapfDingbats" -> true;
            default -> false;
        };
    }

    /**
     * Detects if a font is a subset. Subset fonts have a 6-letter prefix followed by '+'.
     * E.g. "FZQQVU+CMR17" is a subset, "TimesNewRomanPSMT" is not.
     * Subset fonts only contain glyphs used in the original document — they can't render
     * arbitrary new text, so we must NOT inject them via @font-face.
     */
    private boolean isSubsetFont(String baseFontName) {
        if (baseFontName.length() > 7 && baseFontName.charAt(6) == '+') {
            String prefix = baseFontName.substring(0, 6);
            // Subset prefix is always 6 uppercase letters
            return prefix.chars().allMatch(c -> c >= 'A' && c <= 'Z');
        }
        return false;
    }
}
