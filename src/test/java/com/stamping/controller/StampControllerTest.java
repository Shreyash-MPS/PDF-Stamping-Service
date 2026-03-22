package com.stamping.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stamping.service.AdFetchService;
import com.stamping.service.AdStampService;
import com.stamping.service.DemoConfigGeneratorService;
import com.stamping.service.DemoStampService;
import com.stamping.service.MetadataFrontPageService;
import com.stamping.service.PdfFontExtractor;
import com.stamping.service.StampService;
import com.stamping.service.TemplateService;

class StampControllerTest {

    @Mock private StampService stampService;
    @Mock private AdStampService adStampService;
    @Mock private MetadataFrontPageService metadataFrontPageService;
    @Mock private AdFetchService adFetchService;
    @Mock private TemplateService templateService;
    @Mock private DemoConfigGeneratorService demoConfigGeneratorService;
    @Mock private DemoStampService demoStampService;
    @Mock private PdfFontExtractor pdfFontExtractor;

    @InjectMocks
    private StampController stampController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        try {
            var field = StampController.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(stampController, objectMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testControllerInitialization() {
        assertNotNull(stampController);
    }
}
