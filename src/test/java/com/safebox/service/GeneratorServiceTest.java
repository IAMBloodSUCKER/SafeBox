package com.safebox.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratorServiceTest {

    private final GeneratorService generatorService = new GeneratorService();

    @Test
    void generate_respectsLength() {
        String password = generatorService.generate(20, true, true, true, true);
        assertEquals(20, password.length());
    }

    @Test
    void generate_usesSelectedCharsets() {
        String password = generatorService.generate(32, true, false, false, false);
        assertTrue(password.chars().allMatch(c -> c >= 'A' && c <= 'Z'));
    }

    @Test
    void generate_rejectsInvalidLength() {
        assertThrows(IllegalArgumentException.class,
                () -> generatorService.generate(4, true, true, true, true));
    }

    @Test
    void generate_requiresAtLeastOneCharset() {
        assertThrows(IllegalArgumentException.class,
                () -> generatorService.generate(12, false, false, false, false));
    }
}
