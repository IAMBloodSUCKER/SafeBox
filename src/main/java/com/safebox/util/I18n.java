package com.safebox.util;

import java.io.IOException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Application UI strings with English/Russian locales.
 */
public final class I18n {

    private static final Locale LOCALE_EN = Locale.forLanguageTag("en");
    private static final Locale LOCALE_RU = Locale.forLanguageTag("ru");
    private static final String BUNDLE_BASE = "i18n.messages";

    private static Locale currentLocale = LOCALE_EN;
    private static ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE, currentLocale);
    private static final List<Runnable> listeners = new ArrayList<>();

    private I18n() {
    }

    /**
     * Loads saved locale preference and initializes bundles.
     */
    public static void init() {
        currentLocale = loadSavedLocale();
        bundle = ResourceBundle.getBundle(BUNDLE_BASE, currentLocale);
    }

    /**
     * Returns a localized string for the key.
     *
     * @param key message key
     * @return localized text
     */
    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * Returns a formatted localized string.
     *
     * @param key  message key
     * @param args format arguments
     * @return localized text
     */
    public static String get(String key, Object... args) {
        return MessageFormat.format(get(key), args);
    }

    /**
     * Returns the active locale.
     *
     * @return current locale
     */
    public static Locale getLocale() {
        return currentLocale;
    }

    /**
     * Returns true if Russian is active.
     *
     * @return true for Russian locale
     */
    public static boolean isRussian() {
        return LOCALE_RU.getLanguage().equals(currentLocale.getLanguage());
    }

    /**
     * Switches between English and Russian.
     */
    public static void toggleLocale() {
        setLocale(isRussian() ? LOCALE_EN : LOCALE_RU);
    }

    /**
     * Sets locale and notifies listeners.
     *
     * @param locale target locale
     */
    public static void setLocale(Locale locale) {
        currentLocale = locale;
        bundle = ResourceBundle.getBundle(BUNDLE_BASE, currentLocale);
        saveLocale(locale);
        listeners.forEach(Runnable::run);
    }

    /**
     * Registers a callback invoked when locale changes.
     *
     * @param listener refresh handler
     */
    public static void addListener(Runnable listener) {
        listeners.add(listener);
    }

    /**
     * Removes a locale change listener.
     *
     * @param listener refresh handler
     */
    public static void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private static Locale loadSavedLocale() {
        try {
            if (Files.exists(AppPaths.localeFile())) {
                String code = Files.readString(AppPaths.localeFile()).trim();
                if ("ru".equalsIgnoreCase(code)) {
                    return LOCALE_RU;
                }
            }
        } catch (IOException ignored) {
            // default English
        }
        return LOCALE_EN;
    }

    private static void saveLocale(Locale locale) {
        try {
            Files.createDirectories(AppPaths.dataDir());
            Files.writeString(AppPaths.localeFile(), locale.getLanguage());
        } catch (IOException ignored) {
            // non-critical
        }
    }
}
