package com.stamping.service.stamper;

import com.stamping.model.StampRequest;

/**
 * Strategy interface for applying different types of stamps to a PDF.
 */
public interface Stamper {

    /**
     * Apply a stamp to the given PDF.
     *
     * @param pdfBytes     the source PDF as a byte array
     * @param request      stamp configuration (position, opacity, rotation, pages, etc.)
     * @param stampContent the stamp payload — image bytes for IMAGE, HTML bytes for HTML, null for TEXT
     * @return the stamped PDF as a byte array
     */
    byte[] stamp(byte[] pdfBytes, StampRequest request, byte[] stampContent);
}
