package com.stamping.service;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

import com.stamping.config.StampingProperties;
import com.stamping.exception.StampingException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Centralizes input validation: file path traversal prevention,
 * identifier sanitization, and HTML sanitization via OWASP.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InputSanitizer {

    private final StampingProperties properties;

    private static final Pattern SAFE_ID = Pattern.compile("^[a-zA-Z0-9_\\-]+$");

    /**
     * Relaxed HTML policy — allows typical stamp content (images, links, basic formatting)
     * but strips script tags, event handlers, and other dangerous constructs.
     */
    private static final PolicyFactory HTML_POLICY = new HtmlPolicyBuilder()
            .allowElements("div", "span", "p", "h1", "h2", "h3", "h4", "h5", "h6",
                    "a", "img", "br", "hr", "table", "tr", "td", "th", "thead", "tbody",
                    "ul", "ol", "li", "b", "i", "em", "strong", "u", "small", "sub", "sup",
                    "svg", "g", "path", "rect", "circle", "text", "tspan", "defs", "use")
            .allowAttributes("style", "class", "id", "width", "height").globally()
            .allowAttributes("href").onElements("a")
            .allowAttributes("src", "alt").onElements("img")
            .allowAttributes("target", "rel").onElements("a")
            // SVG attributes
            .allowAttributes("viewBox", "xmlns", "fill", "fill-rule", "d", "transform",
                    "stroke", "stroke-width", "cx", "cy", "r", "x", "y", "dx", "dy",
                    "font-size", "text-anchor", "font-family", "font-weight")
            .globally()
            .allowUrlProtocols("http", "https", "data")
            .toFactory();

    /**
     * Validates that a file path does not escape the allowed base directory.
     * If no base path is configured, only checks for obvious traversal patterns.
     */
    public void validateFilePath(String path) {
        if (path == null || path.isBlank()) {
            throw new StampingException("File path is required");
        }

        // Block obvious traversal patterns regardless of config
        if (path.contains("..")) {
            throw new StampingException("Invalid file path: directory traversal not allowed");
        }

        String allowedBase = properties.getAllowedPdfBasePath();
        if (allowedBase != null && !allowedBase.isBlank()) {
            try {
                String canonical = new File(path).getCanonicalPath();
                String allowedCanonical = new File(allowedBase).getCanonicalPath();
                if (!canonical.startsWith(allowedCanonical)) {
                    throw new StampingException("Access denied: file path outside allowed directory");
                }
            } catch (IOException e) {
                throw new StampingException("Invalid file path: " + e.getMessage());
            }
        }
    }

    /**
     * Validates that a publisher ID or jcode contains only safe characters.
     * Prevents path injection via config file naming.
     */
    public void validateIdentifier(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new StampingException(fieldName + " is required");
        }
        if (!SAFE_ID.matcher(value).matches()) {
            throw new StampingException(fieldName + " contains invalid characters (only alphanumeric, hyphens, underscores allowed)");
        }
    }

    /**
     * Sanitizes user-provided HTML content using OWASP policy.
     * Strips scripts, event handlers, and other dangerous constructs
     * while preserving layout-relevant tags and styles.
     */
    public String sanitizeHtml(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        return HTML_POLICY.sanitize(html);
    }
}
