package com.safebox.service;

import com.safebox.model.PasswordEntry;
import com.safebox.repository.DatabaseConnection;
import com.safebox.repository.PasswordRepository;
import com.safebox.util.AppPaths;
import com.safebox.util.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * High-level vault operations: setup, unlock, and CRUD.
 */
public class PasswordManager {

    private static final Logger LOG = LoggerFactory.getLogger(PasswordManager.class);

    private final DatabaseConnection databaseConnection;
    private final PasswordRepository passwordRepository;
    private final CryptoService cryptoService;

    public PasswordManager(DatabaseConnection databaseConnection,
                           PasswordRepository passwordRepository,
                           CryptoService cryptoService) {
        this.databaseConnection = databaseConnection;
        this.passwordRepository = passwordRepository;
        this.cryptoService = cryptoService;
    }

    /**
     * Checks whether the vault needs first-time setup.
     *
     * @return true if no vault exists yet
     */
    public boolean isFirstRun() {
        return !Files.exists(AppPaths.saltFile()) || !databaseConnection.isVaultInitialized();
    }

    /**
     * Creates a new vault with a master password.
     *
     * @param masterPassword master password characters
     * @return derived session encryption key
     */
    public SecretKey setupVault(char[] masterPassword) {
        validateMasterPassword(masterPassword);
        try {
            Files.createDirectories(AppPaths.dataDir());
            byte[] salt = cryptoService.generateSalt();
            Files.write(AppPaths.saltFile(), salt);

            databaseConnection.connect();
            String verifier = cryptoService.createVerifier(masterPassword, salt);
            passwordRepository.saveMasterVerifier(verifier);

            return cryptoService.deriveKey(masterPassword, salt);
        } catch (IOException | SQLException e) {
            throw new VaultException(I18n.get("error.vault.create"), e);
        }
    }

    /**
     * Unlocks the vault with a master password.
     *
     * @param masterPassword master password characters
     * @return derived session encryption key
     */
    public SecretKey unlockVault(char[] masterPassword) {
        validateMasterPassword(masterPassword);
        try {
            if (!Files.exists(AppPaths.saltFile())) {
                throw new VaultException(I18n.get("error.vault.notInitialized"));
            }
            byte[] salt = Files.readAllBytes(AppPaths.saltFile());
            databaseConnection.connect();
            String verifier = passwordRepository.loadMasterVerifier();

            if (!cryptoService.verifyPassword(masterPassword, salt, verifier)) {
                throw new VaultException(I18n.get("error.vault.invalidPassword"));
            }
            return cryptoService.deriveKey(masterPassword, salt);
        } catch (IOException | SQLException e) {
            throw new VaultException(I18n.get("error.vault.unlock"), e);
        }
    }

    /**
     * Returns password entries matching an optional search query.
     *
     * @param encryptionKey session key
     * @param searchQuery   optional filter
     * @return entries list
     */
    public List<PasswordEntry> listEntries(SecretKey encryptionKey, String searchQuery) {
        try {
            return passwordRepository.findAll(encryptionKey, searchQuery);
        } catch (SQLException e) {
            throw new VaultException(I18n.get("error.vault.loadEntries"), e);
        }
    }

    /**
     * Finds an entry by id.
     *
     * @param id            entry id
     * @param encryptionKey session key
     * @return optional entry
     */
    public Optional<PasswordEntry> getEntry(long id, SecretKey encryptionKey) {
        try {
            return passwordRepository.findById(id, encryptionKey);
        } catch (SQLException e) {
            throw new VaultException(I18n.get("error.vault.loadEntry"), e);
        }
    }

    /**
     * Saves a new or existing entry.
     *
     * @param entry         entry data
     * @param encryptionKey session key
     */
    public void saveEntry(PasswordEntry entry, SecretKey encryptionKey) {
        validateEntry(entry);
        try {
            if (entry.getId() == null) {
                passwordRepository.insert(entry, encryptionKey);
            } else {
                passwordRepository.update(entry, encryptionKey);
            }
        } catch (SQLException e) {
            LOG.error("Failed to save entry", e);
            throw new VaultException(I18n.get("error.vault.saveEntry"), e);
        }
    }

    /**
     * Deletes an entry by id.
     *
     * @param id entry id
     */
    public void deleteEntry(long id) {
        try {
            passwordRepository.delete(id);
        } catch (SQLException e) {
            throw new VaultException(I18n.get("error.vault.deleteEntry"), e);
        }
    }

    /**
     * Deletes multiple entries by id.
     *
     * @param ids entry ids
     * @return number of deleted entries
     */
    public int deleteEntries(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        try {
            passwordRepository.deleteByIds(ids);
            return ids.size();
        } catch (SQLException e) {
            throw new VaultException(I18n.get("error.vault.deleteEntry"), e);
        }
    }

    /**
     * Deletes all password entries from the vault.
     *
     * @return number of deleted entries
     */
    public int deleteAllEntries() {
        try {
            int count = passwordRepository.count();
            passwordRepository.deleteAll();
            return count;
        } catch (SQLException e) {
            throw new VaultException(I18n.get("error.vault.deleteEntry"), e);
        }
    }

    /**
     * Returns the total number of password entries.
     *
     * @return entry count
     */
    public int countEntries() {
        try {
            return passwordRepository.count();
        } catch (SQLException e) {
            throw new VaultException(I18n.get("error.vault.loadEntries"), e);
        }
    }

    /**
     * Imports entries from a transfer file into the vault as new records.
     *
     * @param entries       entries without database ids
     * @param encryptionKey session key
     * @return number of imported entries
     */
    public int importEntries(List<PasswordEntry> entries, SecretKey encryptionKey) {
        int count = 0;
        for (PasswordEntry entry : entries) {
            entry.setId(null);
            saveEntry(entry, encryptionKey);
            count++;
        }
        return count;
    }

    /**
     * Changes the master password and re-encrypts all stored entries.
     * Requires an active unlocked session ({@code currentKey}).
     *
     * @param newPassword new master password (any non-empty length)
     * @param currentKey  active session encryption key
     * @return new session encryption key
     */
    public SecretKey changeMasterPassword(char[] newPassword, SecretKey currentKey) {
        if (currentKey == null) {
            throw new VaultException(I18n.get("error.session.locked"));
        }
        validateMasterPassword(newPassword);
        try {
            List<PasswordEntry> entries = passwordRepository.findAll(currentKey, "");
            byte[] newSalt = cryptoService.generateSalt();
            SecretKey newKey = cryptoService.deriveKey(newPassword, newSalt);
            String newVerifier = cryptoService.createVerifier(newPassword, newSalt);

            var connection = databaseConnection.getConnection();
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                for (PasswordEntry entry : entries) {
                    passwordRepository.update(entry, newKey);
                }
                passwordRepository.saveMasterVerifier(newVerifier);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(autoCommit);
            }

            Files.write(AppPaths.saltFile(), newSalt);
            return newKey;
        } catch (IOException | SQLException e) {
            throw new VaultException(I18n.get("error.vault.changePassword"), e);
        }
    }

    /**
     * Securely clears a character array.
     *
     * @param chars characters to wipe
     */
    public static void wipe(char[] chars) {
        if (chars != null) {
            Arrays.fill(chars, '\0');
        }
    }

    private void validateMasterPassword(char[] masterPassword) {
        if (masterPassword == null || masterPassword.length == 0) {
            throw new VaultException(I18n.get("error.vault.masterPasswordEmpty"));
        }
    }

    private void validateEntry(PasswordEntry entry) {
        if (entry.getSite() == null || entry.getSite().isBlank()) {
            throw new VaultException(I18n.get("error.vault.siteRequired"));
        }
        if (entry.getLogin() == null || entry.getLogin().isBlank()) {
            throw new VaultException(I18n.get("error.vault.loginRequired"));
        }
        if (entry.getPassword() == null || entry.getPassword().isBlank()) {
            throw new VaultException(I18n.get("error.vault.passwordRequired"));
        }
    }

    /**
     * Vault operation exception.
     */
    public static class VaultException extends RuntimeException {
        public VaultException(String message) {
            super(message);
        }

        public VaultException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
