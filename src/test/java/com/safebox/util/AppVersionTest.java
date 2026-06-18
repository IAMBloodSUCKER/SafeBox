package com.safebox.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppVersionTest {

    @Test
    void versionIsLoadedFromProperties() {
        assertFalse(AppVersion.get().isBlank());
        assertTrue(AppVersion.formatted().startsWith("v"));
        assertTrue(AppVersion.windowTitle("SafeBox").contains(AppVersion.get()));
    }
}
