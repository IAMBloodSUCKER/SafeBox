package com.safebox.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Application version read from {@code version.properties} at build time.
 */
public final class AppVersion {

    private static final String VERSION = loadVersion();

    private AppVersion() {
    }

    /**
     * Returns the semantic version (for example {@code 1.0.0}).
     */
    public static String get() {
        return VERSION;
    }

    /**
     * Returns the version with a leading {@code v}.
     */
    public static String formatted() {
        return "v" + VERSION;
    }

    /**
     * Builds a window title such as {@code SafeBox v1.0.0}.
     */
    public static String windowTitle(String appName) {
        return appName + " " + formatted();
    }

    private static String loadVersion() {
        Properties properties = new Properties();
        try (InputStream input = AppVersion.class.getResourceAsStream("/version.properties")) {
            if (input != null) {
                properties.load(input);
                String version = properties.getProperty("version");
                if (version != null && !version.isBlank() && !version.contains("${")) {
                    return version.trim();
                }
            }
        } catch (IOException ignored) {
            // Fall back to dev build marker.
        }
        return "dev";
    }
}
