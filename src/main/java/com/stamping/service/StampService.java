package com.stamping.service;

import com.stamping.exception.StampingException;
import com.stamping.model.StampRequest;
import com.stamping.service.stamper.Stamper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StampService {

    private final Stamper htmlStamper;

    public StampService(@Qualifier("htmlStamper") Stamper htmlStamper) {
        this.htmlStamper = htmlStamper;
    }

    /**
     * Apply an HTML stamp to the given PDF.
     *
     * @param pdfBytes     the source PDF file bytes
     * @param request      stamp configuration (position, opacity, rotation, pages, dimensions)
     * @param stampContent the HTML content bytes
     * @return the stamped PDF as a byte array
     */
    public byte[] applyStamp(byte[] pdfBytes, StampRequest request, byte[] stampContent) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new StampingException("PDF file is required");
        }

        log.debug("Applying HTML stamp: position={}, rotation={}, pages={}",
                request.getPosition(), request.getRotation(), request.getPages());

        long startTime = System.currentTimeMillis();
        byte[] result = htmlStamper.stamp(pdfBytes, request, stampContent);

        log.debug("Stamping completed in {}ms, output size: {} bytes",
                System.currentTimeMillis() - startTime, result.length);

        return result;
    }
}
