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

class PasswordManagerBulkDeleteTest {

    @TempDir
    Path tempDir;

    private PasswordManager passwordManager;
    private DatabaseConnection databaseConnection;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        System.setProperty("user.home", tempDir.toString());
        CryptoService cryptoService = new CryptoService();
        databaseConnection = new DatabaseConnection();
        PasswordRepository passwordRepository = new PasswordRepository(databaseConnection, cryptoService);
        passwordManager = new PasswordManager(databaseConnection, passwordRepository, cryptoService);
        char[] master = "master-password".toCharArray();
        key = passwordManager.setupVault(master);
        PasswordManager.wipe(master);
    }

    @AfterEach
    void tearDown() {
        databaseConnection.close();
        System.clearProperty("user.home");
    }

    @Test
    void deleteSelectedEntries() {
        passwordManager.saveEntry(new PasswordEntry("a.com", "u1", "p1", ""), key);
        passwordManager.saveEntry(new PasswordEntry("b.com", "u2", "p2", ""), key);
        List<PasswordEntry> entries = passwordManager.listEntries(key, "");
        List<Long> ids = List.of(entries.get(0).getId(), entries.get(1).getId());

        int deleted = passwordManager.deleteEntries(ids);

        assertEquals(2, deleted);
        assertEquals(0, passwordManager.countEntries());
    }

    @Test
    void deleteAllEntries() {
        passwordManager.saveEntry(new PasswordEntry("a.com", "u1", "p1", ""), key);
        passwordManager.saveEntry(new PasswordEntry("b.com", "u2", "p2", ""), key);

        int deleted = passwordManager.deleteAllEntries();

        assertEquals(2, deleted);
        assertEquals(0, passwordManager.countEntries());
    }
}
