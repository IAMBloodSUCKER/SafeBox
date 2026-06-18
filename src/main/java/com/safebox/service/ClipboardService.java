package com.safebox.service;

import javafx.application.Platform;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Clipboard copy with automatic clearing after a timeout.
 */
public class ClipboardService {

    private static final long CLEAR_DELAY_MS = 30_000;

    private final AtomicReference<Timer> clearTimer = new AtomicReference<>();

    /**
     * Copies text to the system clipboard and schedules auto-clear.
     *
     * @param text text to copy
     */
    public void copy(String text) {
        if (text == null) {
            return;
        }
        Platform.runLater(() -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            Clipboard.getSystemClipboard().setContent(content);
        });
        scheduleClear();
    }

    /**
     * Clears the clipboard immediately.
     */
    public void clearNow() {
        cancelScheduledClear();
        Platform.runLater(() -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            if (clipboard.hasString()) {
                ClipboardContent content = new ClipboardContent();
                content.putString("");
                clipboard.setContent(content);
            }
        });
    }

    private void scheduleClear() {
        cancelScheduledClear();
        Timer timer = new Timer(true);
        clearTimer.set(timer);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                clearNow();
            }
        }, CLEAR_DELAY_MS);
    }

    private void cancelScheduledClear() {
        Timer existing = clearTimer.getAndSet(null);
        if (existing != null) {
            existing.cancel();
        }
    }
}
