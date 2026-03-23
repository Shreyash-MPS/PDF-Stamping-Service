package com.stamping.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.stamping.model.DynamicStampRequest;
import com.stamping.model.JournalMetadataRequest;
import com.stamping.model.ad.AdData;
import com.stamping.model.ad.AdLocation;
import com.stamping.model.ad.AdResponse;
import com.stamping.model.ad.Section;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TemplateService {

    private final AdFetchService adFetchService;
    private final AdStampService adStampService;
    private final Map<String, String> templates;

    public TemplateService(AdFetchService adFetchService, AdStampService adStampService) {
        this.adFetchService = adFetchService;
        this.adStampService = adStampService;
        templates = new HashMap<>();

        templates.put("journal_article", 
                "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head><meta charset=\"UTF-8\"/></head>\n" +
                "<body style=\"margin: 50px; font-family: Verdana, Arial, Helvetica, sans-serif; color: #000;\">\n" +
                "<table style=\"width: 100%; border-collapse: collapse;\">\n" +
                "  <tr>\n" +
                "    <td style=\"width: 30%; vertical-align: top; padding: 50px 40px 50px 50px;\">\n" +
                "      <div style=\"margin-bottom: 25px;\" class=\"logo-wrapper\">{{LOGO}}</div>\n" +
                "      <p class=\"date-block\" style=\"font-size: 15px; margin: 0; line-height: 1.3;\">This information is current as of {{DATE}}.</p>\n" +
                "    </td>\n" +
                "    <td style=\"vertical-align: top; padding: 50px 50px 50px 0;\">\n" +
                "      <h1 class=\"article-title-block\" style=\"font-size: 22px; font-weight: bold; margin: 0 0 16px 0; line-height: 1.2;\">{{ARTICLE_TITLE}}</h1>\n" +
                "      <p class=\"authors-block\" style=\"font-size: 16px; line-height: 1.4; margin: 0 0 25px 0;\">{{AUTHORS}}</p>\n" +
                "      <div style=\"font-size: 15px; line-height: 1.3;\">\n" +
                "        <p class=\"doi-block\" style=\"margin: 0 0 4px 0;\">doi: {{DOI}}</p>\n" +
                "        <div class=\"link-block\" style=\"margin: 0 0 4px 0;\"><a href=\"{{LINK_URL}}\" style=\"color: blue; text-decoration: none;\">{{LINK_TEXT}}</a></div>\n" +
                "      </div>\n" +
                "    </td>\n" +
                "  </tr>\n" +
                "</table>\n" +
                "</body>\n" +
                "</html>"
        );

        templates.put("genome_last_page", 
                "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head><meta charset=\"UTF-8\"/></head>\n" +
                "<body style=\"margin: 0; padding: 0; font-family: Verdana, Arial, Helvetica, sans-serif; color: #000; font-size: 13px;\">\n" +
                "  <div style=\"padding: 12px 40px; color: #c00; font-size: 11px;\">\n" +
                "    Downloaded from <a href=\"#\" style=\"color: #c00;\">genome.cshlp.org</a> on {{DATE}} - Published by <a href=\"#\" style=\"color: #c00;\">Cold Spring Harbor Laboratory Press</a>\n" +
                "  </div>\n" +
                "  <div style=\"padding: 20px 50px 10px 50px;\">\n" +
                "    <div style=\"margin-bottom: 20px;\" class=\"logo-wrapper\">{{LOGO}}</div>\n" +
                "    <h1 class=\"article-title-block\" style=\"font-size: 20px; font-weight: bold; margin: 0 0 10px 0; line-height: 1.3;\">{{ARTICLE_TITLE}}</h1>\n" +
                "    <p class=\"authors-block\" style=\"font-size: 13px; color: #444; margin: 0 0 10px 0;\">{{AUTHORS}}</p>\n" +
                "    <p style=\"font-size: 12px; color: #444; margin: 0 0 3px 0;\"><i>Genome Res.</i> published online {{DATE}}</p>\n" +
                "    <p class=\"doi-block\" style=\"font-size: 12px; margin: 0 0 15px 0;\">Access the most recent version at doi:<a href=\"#\" style=\"color: blue; text-decoration: none;\">{{DOI}}</a></p>\n" +
                "    <hr style=\"border: none; border-top: 1px solid #999; margin: 15px 0;\" />\n" +
                "    <table style=\"width: 100%; font-size: 12px; border-collapse: collapse;\">\n" +
                "      <tr><td style=\"padding: 8px 0; width: 130px; vertical-align: top; text-align: right; padding-right: 15px; font-weight: bold;\">P&lt;P</td>\n" +
                "          <td style=\"padding: 8px 0; vertical-align: top;\">Published online {{DATE}} in advance of the print journal.</td></tr>\n" +
                "      <tr><td style=\"padding: 8px 0; vertical-align: top; text-align: right; padding-right: 15px; font-weight: bold;\">Accepted<br/>Manuscript</td>\n" +
                "          <td style=\"padding: 8px 0; vertical-align: top;\">Peer-reviewed and accepted for publication but not copyedited or typeset; accepted manuscript is likely to differ from the final, published version.</td></tr>\n" +
                "      <tr><td style=\"padding: 8px 0; vertical-align: top; text-align: right; padding-right: 15px; font-weight: bold;\">Open Access</td>\n" +
                "          <td style=\"padding: 8px 0; vertical-align: top;\">Freely available online through the Genome Research Open Access option.</td></tr>\n" +
                "      <tr><td style=\"padding: 8px 0; vertical-align: top; text-align: right; padding-right: 15px; font-weight: bold;\">Creative<br/>Commons<br/>License</td>\n" +
                "          <td style=\"padding: 8px 0; vertical-align: top;\">This manuscript is Open Access. This article, published in <i>Genome Research</i>, is available under a Creative Commons License (Attribution-NonCommercial 4.0 International license), as described at <a href=\"http://creativecommons.org/licenses/by-nc/4.0/\" style=\"color: blue;\">http://creativecommons.org/licenses/by-nc/4.0/</a>.</td></tr>\n" +
                "      <tr><td style=\"padding: 8px 0; vertical-align: top; text-align: right; padding-right: 15px; font-weight: bold;\">Email Alerting<br/>Service</td>\n" +
                "          <td style=\"padding: 8px 0; vertical-align: top;\">Receive free email alerts when new articles cite this article - sign up in the box at the top right corner of the article or <a href=\"#\" style=\"color: blue; font-weight: bold;\">click here.</a></td></tr>\n" +
                "    </table>\n" +
                "    <hr style=\"border: none; border-top: 1px solid #999; margin: 15px 0;\" />\n" +
                "  </div>\n" +
                "  <div style=\"padding: 0 50px; text-align: center; margin-top: 30px;\">\n" +
                "    <div class=\"ad-banner-block\" style=\"margin-bottom: 20px;\">{{AD_BANNER}}</div>\n" +
                "  </div>\n" +
                "  <div style=\"padding: 0 50px; margin-top: 30px;\">\n" +
                "    <hr style=\"border: none; border-top: 1px solid #999; margin: 15px 0;\" />\n" +
                "    <p style=\"font-size: 12px; margin: 8px 0;\">To subscribe to <i>Genome Research</i> go to:<br/>\n" +
                "      <a href=\"https://genome.cshlp.org/subscriptions\" style=\"color: blue; font-weight: bold;\">https://genome.cshlp.org/subscriptions</a>\n" +
                "    </p>\n" +
                "    <p style=\"font-size: 12px; margin: 20px 0 0 0;\">Published by Cold Spring Harbor Laboratory Press</p>\n" +
                "  </div>\n" +
                "</body>\n" +
                "</html>"
        );

        templates.put("default_metadata", 
                "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head><meta charset=\"UTF-8\"/></head>\n" +
                "<body style=\"margin: 50px; font-family: Verdana, Arial, Helvetica, sans-serif; color: #000;\">\n" +
                "  <div style=\"text-align: center;\">\n" +
                "    <div style=\"margin-bottom: 25px;\" class=\"logo-wrapper\">{{LOGO}}</div>\n" +
                "    <h1 class=\"article-title-block\" style=\"font-size: 22px; font-weight: bold; margin: 0 0 16px 0; line-height: 1.2; text-align: center;\">{{ARTICLE_TITLE}}</h1>\n" +
                "    <p class=\"authors-block\" style=\"font-size: 16px; line-height: 1.4; margin: 0 0 25px 0; text-align: center;\">{{AUTHORS}}</p>\n" +
                "    <p class=\"doi-block\" style=\"margin: 0 0 4px 0; font-size: 15px; text-align: center;\">doi: <a href=\"{{DOI}}\" style=\"color: blue; text-decoration: none;\">{{DOI}}</a></p>\n" +
                "    <div class=\"link-block\" style=\"margin: 0 0 4px 0; font-size: 15px; text-align: center;\"><a href=\"{{LINK_URL}}\" style=\"color: blue; text-decoration: none;\">{{LINK_TEXT}}</a></div>\n" +
                "    <p style=\"margin: 4px 0; font-size: 13px; text-align: center;\">{{COPYRIGHT}}</p>\n" +
                "    <p style=\"margin: 4px 0; font-size: 13px; text-align: center;\">ISSN: {{ISSN}}</p>\n" +
                "    <p style=\"margin: 4px 0; font-size: 13px; text-align: center;\">Article ID: {{ARTICLE_ID}}</p>\n" +
                "    <p style=\"margin: 8px 0; font-size: 13px; color: #555; text-align: center;\">Date Generated: {{DATE}}</p>\n" +
                "    <p style=\"margin: 4px 0; font-size: 13px; color: #555; text-align: center;\">Downloaded By: {{USER}}</p>\n" +
                "  </div>\n" +
                "</body>\n" +
                "</html>"
        );

        templates.put("simple_header", 
                "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head><meta charset=\"UTF-8\"/></head>\n" +
                "<body style=\"margin: 40px; font-family: Verdana, Arial, Helvetica, sans-serif; color: #000;\">\n" +
                "  <div style=\"text-align: center;\">\n" +
                "    <div style=\"margin-bottom: 20px;\" class=\"logo-wrapper\">{{LOGO}}</div>\n" +
                "    <h1 class=\"article-title-block\" style=\"font-size: 22px; font-weight: bold; margin: 0 0 10px 0; line-height: 1.3; text-align: center;\">{{ARTICLE_TITLE}}</h1>\n" +
                "    <p class=\"authors-block\" style=\"font-size: 15px; color: #444; margin: 0 0 10px 0; text-align: center;\">{{AUTHORS}}</p>\n" +
                "    <p class=\"doi-block\" style=\"font-size: 13px; margin: 0 0 6px 0; text-align: center;\">doi: <a href=\"{{DOI}}\" style=\"color: blue; text-decoration: none;\">{{DOI}}</a></p>\n" +
                "    <div class=\"link-block\" style=\"font-size: 13px; margin: 0 0 6px 0; text-align: center;\"><a href=\"{{LINK_URL}}\" style=\"color: blue; text-decoration: none;\">{{LINK_TEXT}}</a></div>\n" +
                "    <p class=\"date-block\" style=\"font-size: 13px; color: #888; margin-top: 16px; text-align: center;\">{{DATE}}</p>\n" +
                "  </div>\n" +
                "</body>\n" +
                "</html>"
        );
        
        templates.put("custom_html",
                "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head><meta charset=\"UTF-8\"/></head>\n" +
                "<body style=\"margin: 40px; font-family: Verdana, Arial, Helvetica, sans-serif;\">\n" +
                "  <div>\n" +
                "    <!-- Write your custom HTML here -->\n" +
                "    <p>Custom content goes here</p>\n" +
                "  </div>\n" +
                "</body>\n" +
                "</html>"
        );
    }

    public String renderTemplate(DynamicStampRequest.Configuration config, JournalMetadataRequest request) {
        return renderTemplate(config, request, null);
    }

    public String renderTemplate(DynamicStampRequest.Configuration config, JournalMetadataRequest request,
                                 PdfFontExtractor.FontInfo fontInfo) {
        String templateName = config.getTemplateName() != null ? config.getTemplateName() : "default_metadata";
        String template = templates.getOrDefault(templateName, templates.get("default_metadata"));

        // Inject extracted PDF font into the template
        if (fontInfo != null) {
            PdfFontExtractor fontExtractor = new PdfFontExtractor();
            template = fontExtractor.injectFontIntoHtml(template, fontInfo);
        }
        
        // Date
        if (Boolean.TRUE.equals(config.getIncludeDate())) {
            String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
            template = template.replace("{{DATE}}", dateStr);
        } else {
            template = template.replace("{{DATE}}", "");
            template = template.replaceAll("<[^>]*class=\"date-block\"[^>]*>[\\s\\S]*?</[^>]+>", "");
        }

        // Logo
        String logoBase64 = config.getLogo();
        if (logoBase64 != null && !logoBase64.isBlank()) {
            String mime = config.getLogoMimeType() != null ? config.getLogoMimeType() : "image/png";
            template = template.replace("{{LOGO}}", "<img src=\"data:" + mime + ";base64," + logoBase64 + "\" style=\"max-width: 200px; display: block; margin-bottom: 16px;\" />");
        } else if (request.isDemoMode()) {
            // In demo mode, inject the HighWire logo SVG when no custom logo is provided
            template = template.replace("{{LOGO}}", DemoStampService.getHighWireLogoSvg());
        } else {
            template = template.replace("{{LOGO}}", "");
        }

        // Title
        if (Boolean.TRUE.equals(config.getIncludeArticleTitle()) && request.getArticleTitle() != null && !request.getArticleTitle().isBlank()) {
            template = template.replace("{{ARTICLE_TITLE}}", request.getArticleTitle());
        } else {
            template = template.replace("{{ARTICLE_TITLE}}", "");
            template = template.replaceAll("<[^>]*class=\"article-title-block\"[^>]*>[\\s\\S]*?</[^>]+>", "");
        }

        // Authors
        if (Boolean.TRUE.equals(config.getIncludeAuthors()) && request.getAuthors() != null && !request.getAuthors().isBlank()) {
            template = template.replace("{{AUTHORS}}", request.getAuthors());
        } else {
            template = template.replace("{{AUTHORS}}", "");
            template = template.replaceAll("<[^>]*class=\"authors-block\"[^>]*>[\\s\\S]*?</[^>]+>", "");
        }

        // DOI
        if (Boolean.TRUE.equals(config.getIncludeDoi()) && request.getDoiValue() != null && !request.getDoiValue().isBlank()) {
            String doiUrl = request.getDoiValue().startsWith("http") ? request.getDoiValue() : "https://doi.org/" + request.getDoiValue();
            template = template.replace("{{DOI}}", doiUrl);
        } else {
            template = template.replace("{{DOI}}", "");
            template = template.replaceAll("<[^>]*class=\"doi-block\"[^>]*>[\\s\\S]*?</[^>]+>", "");
        }
        
        // Link
        String linkUrl = config.getLinkUrl();
        String linkText = config.getLinkText();
        if (linkUrl != null && !linkUrl.isBlank() && linkText != null && !linkText.isBlank()) {
            template = template.replace("{{LINK_URL}}", linkUrl);
            template = template.replace("{{LINK_TEXT}}", linkText);
        } else {
            template = template.replaceAll("<[^>]*class=\"link-block\"[^>]*>[\\s\\S]*?</[^>]+>", "");
        }

        // Additional Metadata Fields
        template = template.replace("{{COPYRIGHT}}", request.getArticleCopyright() != null ? request.getArticleCopyright() : "");
        template = template.replace("{{ISSN}}", request.getArticleIssn() != null ? request.getArticleIssn() : "");
        template = template.replace("{{ARTICLE_ID}}", request.getArticleId() != null ? request.getArticleId() : "");
        if (Boolean.TRUE.equals(config.getIncludeCurrentUser()) && request.getDownloadedBy() != null && !request.getDownloadedBy().isBlank()) {
            template = template.replace("{{USER}}", request.getDownloadedBy());
        } else {
            template = template.replace("{{USER}}", "");
        }

        // Ad Banner — fetch and inject if ads are enabled and template has the placeholder
        // For NEW_PAGE templates, only use ads with position "pdf ad one"
        if (template.contains("{{AD_BANNER}}")) {
            String adHtml = "";
            if (Boolean.TRUE.equals(config.getAdsEnabled())
                    && request.getPublisherId() != null && !request.getPublisherId().isBlank()
                    && request.getJcode() != null && !request.getJcode().isBlank()) {
                String adUrl = "https://bam-ads-presenter.highwire.org/api/ads?publisherId="
                        + request.getPublisherId() + "&jcode=" + request.getJcode() + "&sectionPath=xpdf";
                AdResponse adResponse = adFetchService.fetchAds(adUrl);
                if (adResponse != null && adResponse.getSection() != null) {
                    outer:
                    for (Section sec : adResponse.getSection()) {
                        if (sec.getAdLocation() != null) {
                            for (AdLocation location : sec.getAdLocation()) {
                                // Filter to "pdf ad one" position only for NEW_PAGE template rendering
                                if ("pdf ad one".equalsIgnoreCase(location.getPositionName())
                                        && location.getAdData() != null) {
                                    for (AdData ad : location.getAdData()) {
                                        if (ad.getAdHtml() != null && !ad.getAdHtml().isEmpty()) {
                                            adHtml = adStampService.processHtmlContent(ad.getAdHtml(), config.getLegacyDomain());
                                            break outer;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (adHtml.isEmpty()) {
                        log.warn("Ad response received but no 'pdf ad one' position found for pubId={}, jcode={}",
                                request.getPublisherId(), request.getJcode());
                    }
                } else {
                    log.warn("No ad response or empty sections for template ad banner (pubId={}, jcode={})",
                            request.getPublisherId(), request.getJcode());
                }
            }
            template = template.replace("{{AD_BANNER}}", adHtml);
            if (adHtml.isEmpty()) {
                template = template.replaceAll("<[^>]*class=\"ad-banner-block\"[^>]*>[\\s\\S]*?</[^>]+>", "");
            }
        }

        return template;
    }
}
