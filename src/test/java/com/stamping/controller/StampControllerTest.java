package com.stamping.controller;

import com.stamping.model.AdJsonRequest;
import com.stamping.service.AdStampService;
import com.stamping.service.StampService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class StampControllerTest {

    @Mock
    private StampService stampService;

    @Mock
    private AdStampService adStampService;

    @InjectMocks
    private StampController stampController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testStampPdfWithAdJson_Success() throws Exception {
        // Create a temporary file to act as the input PDF
        File tempInputFile = File.createTempFile("test_input", ".pdf");
        Files.write(tempInputFile.toPath(), "dummy content".getBytes());
        tempInputFile.deleteOnExit();

        String adJsonUrl = "http://example.com/ads.json";
        byte[] expectedStampedPdf = "stamped content".getBytes();
        String adType = "all";

        AdJsonRequest request = AdJsonRequest.builder()
                .inputPath(tempInputFile.getAbsolutePath())
                .adJsonUrl(adJsonUrl)
                .adType(adType)
                .build();

        when(adStampService.processAdJson(any(byte[].class), eq(adJsonUrl), eq(adType)))
                .thenReturn(expectedStampedPdf);

        ResponseEntity<?> response = stampController.stampPdfWithAdJson(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(adStampService).processAdJson(any(byte[].class), eq(adJsonUrl), eq(adType));
    }
}
