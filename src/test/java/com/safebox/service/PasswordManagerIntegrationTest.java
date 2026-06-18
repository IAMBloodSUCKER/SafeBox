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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PasswordManagerIntegrationTest {

  @TempDir
  Path tempDir;

  private PasswordManager passwordManager;
  private SecretKey sessionKey;
  private DatabaseConnection databaseConnection;
  private char[] masterPassword = "integration-test-password".toCharArray();

  @BeforeEach
  void setUp() {
    System.setProperty("user.home", tempDir.toString());
    CryptoService cryptoService = new CryptoService();
    databaseConnection = new DatabaseConnection();
    PasswordRepository passwordRepository = new PasswordRepository(databaseConnection, cryptoService);
    passwordManager = new PasswordManager(databaseConnection, passwordRepository, cryptoService);
    sessionKey = passwordManager.setupVault(masterPassword);
  }

  @AfterEach
  void tearDown() {
    databaseConnection.close();
    System.clearProperty("user.home");
    PasswordManager.wipe(masterPassword);
  }

  @Test
  void saveEntry_persistsAndReloads() {
    PasswordEntry entry = new PasswordEntry("example.com", "user", "secret123", "note");
    assertDoesNotThrow(() -> passwordManager.saveEntry(entry, sessionKey));

    var loaded = passwordManager.listEntries(sessionKey, "");
    assertEquals(1, loaded.size());
    assertEquals("example.com", loaded.get(0).getSite());
    assertEquals("user", loaded.get(0).getLogin());
    assertEquals("secret123", loaded.get(0).getPassword());
    assertEquals("note", loaded.get(0).getNotes());
  }
}
