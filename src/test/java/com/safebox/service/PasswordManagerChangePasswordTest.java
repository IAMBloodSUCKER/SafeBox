package com.safebox.service;

import com.safebox.model.PasswordEntry;
import com.safebox.repository.DatabaseConnection;
import com.safebox.repository.PasswordRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.crypto.SecretKey;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordManagerChangePasswordTest {

    @TempDir
    Path tempDir;

    private PasswordManager passwordManager;
    private DatabaseConnection databaseConnection;
    private SecretKey key;
    private char[] master = "old-password".toCharArray();

    @BeforeEach
    void setUp() {
        System.setProperty("user.home", tempDir.toString());
        CryptoService cryptoService = new CryptoService();
        databaseConnection = new DatabaseConnection();
        PasswordRepository passwordRepository = new PasswordRepository(databaseConnection, cryptoService);
        passwordManager = new PasswordManager(databaseConnection, passwordRepository, cryptoService);
        key = passwordManager.setupVault(master);
        passwordManager.saveEntry(new PasswordEntry("site.com", "user", "secret", "note"), key);
    }

    @AfterEach
    void tearDown() {
        databaseConnection.close();
        System.clearProperty("user.home");
        PasswordManager.wipe(master);
    }

    @Test
    void changeMasterPassword_reencryptsEntries() {
        char[] current = "old-password".toCharArray();
        char[] updated = "x".toCharArray();

        SecretKey newKey = passwordManager.changeMasterPassword(current, updated, key);

        List<PasswordEntry> withNew = passwordManager.listEntries(newKey, "");
        assertEquals(1, withNew.size());
        assertEquals("secret", withNew.get(0).getPassword());

        char[] unlock = "x".toCharArray();
        SecretKey unlocked = passwordManager.unlockVault(unlock);
        assertEquals("site.com", passwordManager.listEntries(unlocked, "").get(0).getSite());

        PasswordManager.wipe(current);
        PasswordManager.wipe(updated);
        PasswordManager.wipe(unlock);
    }

    @Test
    void changeMasterPassword_rejectsWrongCurrentPassword() {
        char[] wrong = "wrong".toCharArray();
        char[] updated = "new-password".toCharArray();

        assertThrows(PasswordManager.VaultException.class,
                () -> passwordManager.changeMasterPassword(wrong, updated, key));

        PasswordManager.wipe(wrong);
        PasswordManager.wipe(updated);
    }

    @Test
    void setupVault_allowsShortPassword() {
        databaseConnection.close();
        System.setProperty("user.home", tempDir.resolve("short").toString());
        CryptoService cryptoService = new CryptoService();
        databaseConnection = new DatabaseConnection();
        PasswordRepository repo = new PasswordRepository(databaseConnection, cryptoService);
        PasswordManager manager = new PasswordManager(databaseConnection, repo, cryptoService);
        char[] shortPwd = "1".toCharArray();

        SecretKey shortKey = manager.setupVault(shortPwd);
        assertTrue(manager.listEntries(shortKey, "").isEmpty());

        PasswordManager.wipe(shortPwd);
    }
}
