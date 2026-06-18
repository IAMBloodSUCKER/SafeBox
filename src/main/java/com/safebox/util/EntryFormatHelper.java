package com.safebox.util;

import com.safebox.model.PasswordEntry;

import java.util.List;

/**
 * Formats password entries for compact dialog messages.
 */
public final class EntryFormatHelper {

    private static final int DIALOG_FIELD_MAX = 48;

    private EntryFormatHelper() {
    }

    /**
     * Builds a delete confirmation message for a single entry.
     */
    public static String deleteSingleMessage(PasswordEntry entry) {
        return I18n.get("main.delete.site", truncate(entry.getSite()))
                + "\n"
                + I18n.get("main.delete.login", truncate(entry.getLogin()))
                + "\n\n"
                + I18n.get("main.delete.warning");
    }

    /**
     * Builds a delete confirmation message for multiple entries.
     */
    public static String deleteSelectedMessage(List<PasswordEntry> entries) {
        StringBuilder message = new StringBuilder();
        int previewCount = Math.min(3, entries.size());
        for (int i = 0; i < previewCount; i++) {
            PasswordEntry entry = entries.get(i);
            message.append("• ")
                    .append(truncate(entry.getSite()))
                    .append(" / ")
                    .append(truncate(entry.getLogin()))
                    .append('\n');
        }
        if (entries.size() > previewCount) {
            message.append(I18n.get("main.delete.andMore", entries.size() - previewCount))
                    .append('\n');
        }
        message.append('\n').append(I18n.get("main.delete.warning"));
        return message.toString().strip();
    }

    private static String truncate(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        String oneLine = value.replace('\n', ' ').replace('\r', ' ').strip();
        while (oneLine.contains("  ")) {
            oneLine = oneLine.replace("  ", " ");
        }
        if (oneLine.length() <= DIALOG_FIELD_MAX) {
            return oneLine;
        }
        return oneLine.substring(0, DIALOG_FIELD_MAX - 3) + "...";
    }
}
