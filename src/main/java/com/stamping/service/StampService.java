package com.stamping.service;

import com.stamping.exception.StampingException;
import com.stamping.model.StampRequest;
import com.stamping.model.StampType;
import com.stamping.service.stamper.Stamper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.stamping.model.StampPosition;

@Slf4j
@Service
public class StampService {

    private final Stamper textStamper;
    private final Stamper imageStamper;
    private final Stamper htmlStamper;
    private final Stamper pdfStamper;

    public StampService(@Qualifier("textStamper") Stamper textStamper,
            @Qualifier("imageStamper") Stamper imageStamper,
            @Qualifier("htmlStamper") Stamper htmlStamper,
            @Qualifier("pdfStamper") Stamper pdfStamper) {
        this.textStamper = textStamper;
        this.imageStamper = imageStamper;
        this.htmlStamper = htmlStamper;
        this.pdfStamper = pdfStamper;
    }

    /**
     * Apply a stamp to the given PDF based on the stamp type and configuration.
     *
     * @param pdfBytes     the source PDF file bytes
     * @param request      stamp configuration
     * @param stampContent the stamp file bytes (image or HTML), null for text
     *                     stamps
     * @return the stamped PDF as a byte array
     */
    public byte[] applyStamp(byte[] pdfBytes, StampRequest request, byte[] stampContent) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new StampingException("PDF file is required");
        }
        if (request.getStampType() == null) {
            throw new StampingException("Stamp type is required");
        }

        log.info("Applying {} stamp with position={}, opacity={}, rotation={}, pages={}",
                request.getStampType(), request.getPosition(), request.getOpacity(),
                request.getRotation(), request.getPages());

        Stamper stamper = getStamper(request.getStampType());
        long startTime = System.currentTimeMillis();
        byte[] result = stamper.stamp(pdfBytes, request, stampContent);

        log.info("Stamping completed in {}ms, output size: {} bytes",
                System.currentTimeMillis() - startTime, result.length);

        return result;
    }

    private Stamper getStamper(StampType type) {
        return switch (type) {
            case TEXT -> textStamper;
            case IMAGE -> imageStamper;
            case HTML -> htmlStamper;
            case PDF -> pdfStamper;
        };
    }
}
