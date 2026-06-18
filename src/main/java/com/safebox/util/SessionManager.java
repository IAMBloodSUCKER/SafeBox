package com.safebox.util;

import com.safebox.service.PasswordManager;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Manages an unlocked vault session with idle timeout.
 */
public class SessionManager {

    public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);

    private final Duration idleTimeout;
    private SecretKey encryptionKey;
    private char[] masterPasswordCopy;
    private final AtomicLong lastActivityMillis = new AtomicLong(System.currentTimeMillis());
    private Consumer<Void> onLockCallback;
    private Thread watchdogThread;
    private volatile boolean running;

    public SessionManager() {
        this(DEFAULT_TIMEOUT);
    }

    public SessionManager(Duration idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    /**
     * Starts a session with the derived encryption key.
     *
     * @param encryptionKey session key
     * @param masterPassword master password (wiped on lock)
     */
    public synchronized void start(SecretKey encryptionKey, char[] masterPassword) {
        this.encryptionKey = encryptionKey;
        this.masterPasswordCopy = masterPassword == null ? null : Arrays.copyOf(masterPassword, masterPassword.length);
        touch();
        startWatchdog();
    }

    /**
     * Returns the active encryption key if session is unlocked.
     *
     * @return session key
     */
    public synchronized SecretKey getEncryptionKey() {
        if (encryptionKey == null) {
            throw new IllegalStateException(I18n.get("error.session.locked"));
        }
        return encryptionKey;
    }

    /**
     * Returns whether the vault is currently unlocked.
     *
     * @return true if unlocked
     */
    public synchronized boolean isUnlocked() {
        return encryptionKey != null;
    }

    /**
     * Registers a callback invoked when the session locks.
     *
     * @param callback lock handler
     */
    public void setOnLock(Consumer<Void> callback) {
        this.onLockCallback = callback;
    }

    /**
     * Updates the last activity timestamp (resets idle timer).
     */
    public void touch() {
        lastActivityMillis.set(System.currentTimeMillis());
    }

    /**
     * Returns remaining idle time before auto-lock.
     *
     * @return remaining duration
     */
    public Duration remainingTime() {
        long elapsed = System.currentTimeMillis() - lastActivityMillis.get();
        long remaining = idleTimeout.toMillis() - elapsed;
        return Duration.ofMillis(Math.max(0, remaining));
    }

    /**
     * Replaces the session key and master password after a password change.
     *
     * @param newKey            new encryption key
     * @param newMasterPassword new master password
     */
    public synchronized void rotateKey(SecretKey newKey, char[] newMasterPassword) {
        if (encryptionKey != null) {
            byte[] keyBytes = encryptionKey.getEncoded();
            if (keyBytes != null) {
                Arrays.fill(keyBytes, (byte) 0);
            }
        }
        PasswordManager.wipe(masterPasswordCopy);
        this.encryptionKey = newKey;
        this.masterPasswordCopy = newMasterPassword == null
                ? null
                : Arrays.copyOf(newMasterPassword, newMasterPassword.length);
        touch();
    }

    /**
     * Locks the session and wipes sensitive data from memory.
     */
    public synchronized void lock() {
        stopWatchdog();
        if (encryptionKey != null) {
            byte[] keyBytes = encryptionKey.getEncoded();
            if (keyBytes != null) {
                Arrays.fill(keyBytes, (byte) 0);
            }
            encryptionKey = null;
        }
        PasswordManager.wipe(masterPasswordCopy);
        masterPasswordCopy = null;
        if (onLockCallback != null) {
            onLockCallback.accept(null);
        }
    }

    private void startWatchdog() {
        stopWatchdog();
        running = true;
        watchdogThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (remainingTime().isZero()) {
                    lock();
                    return;
                }
            }
        }, "safebox-session-watchdog");
        watchdogThread.setDaemon(true);
        watchdogThread.start();
    }

    private void stopWatchdog() {
        running = false;
        if (watchdogThread != null) {
            watchdogThread.interrupt();
            watchdogThread = null;
        }
    }
}
