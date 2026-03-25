package com.stamping.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stamping.service.DemoStampService;

class StampControllerTest {

    @Mock private DemoStampService demoStampService;

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
