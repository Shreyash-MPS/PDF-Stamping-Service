package com.stamping.service.stamper;

import com.stamping.exception.StampingException;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility to parse page selection expressions like "ALL", "FIRST", "LAST",
 * "1,3,5-7".
 * Returns a set of 0-based page indices.
 */
public final class PageSelector {

    private PageSelector() {
    }

    /**
     * Parse a page selection expression into a set of 0-based page indices.
     *
     * @param expression the page expression (e.g. "ALL", "FIRST", "LAST",
     *                   "1,3,5-7")
     * @param totalPages total number of pages in the document
     * @return set of 0-based page indices
     */
    public static Set<Integer> parsePages(String expression, int totalPages) {
        if (expression == null || expression.isBlank() || expression.equalsIgnoreCase("ALL")) {
            Set<Integer> all = new HashSet<>();
            for (int i = 0; i < totalPages; i++)
                all.add(i);
            return all;
        }

        if (expression.equalsIgnoreCase("FIRST")) {
            return Set.of(0);
        }

        if (expression.equalsIgnoreCase("LAST")) {
            return Set.of(totalPages - 1);
        }

        // Parse comma-separated ranges like "1,3,5-7"
        Set<Integer> pages = new HashSet<>();
        String[] parts = expression.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.contains("-")) {
                String[] range = part.split("-");
                if (range.length != 2) {
                    throw new StampingException("Invalid page range: " + part);
                }
                int start = Integer.parseInt(range[0].trim()) - 1; // convert to 0-based
                int end = Integer.parseInt(range[1].trim()) - 1;
                if (start < 0 || end >= totalPages || start > end) {
                    throw new StampingException(
                            "Invalid page range: " + part + " (document has " + totalPages + " pages)");
                }
                for (int i = start; i <= end; i++)
                    pages.add(i);
            } else {
                int page = Integer.parseInt(part) - 1; // convert to 0-based
                if (page < 0 || page >= totalPages) {
                    throw new StampingException(
                            "Invalid page number: " + (page + 1) + " (document has " + totalPages + " pages)");
                }
                pages.add(page);
            }
        }
        return pages;
    }
}
