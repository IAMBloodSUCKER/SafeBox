package com.safebox.util;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Application data directory paths.
 */
public final class AppPaths {

    private static final String APP_DIR_NAME = ".safebox";

    private AppPaths() {
    }

    /**
     * Returns the application data directory (~/.safebox).
     *
     * @return data directory path
     */
    public static Path dataDir() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, APP_DIR_NAME);
    }

    /**
     * Returns the SQLite database file path.
     *
     * @return database path
     */
    public static Path databaseFile() {
        return dataDir().resolve("safebox.db");
    }

    /**
     * Returns the PBKDF2 salt file path.
     *
     * @return salt file path
     */
    public static Path saltFile() {
        return dataDir().resolve("salt.bin");
    }

    /**
     * Returns the theme preference file path.
     *
     * @return theme file path
     */
    public static Path themeFile() {
        return dataDir().resolve("theme.txt");
    }

    /**
     * Returns the locale preference file path.
     *
     * @return locale file path
     */
    public static Path localeFile() {
        return dataDir().resolve("locale.txt");
    }
}
