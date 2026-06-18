package com.safebox.repository;

import com.safebox.model.PasswordEntry;
import com.safebox.service.CryptoService;

import javax.crypto.SecretKey;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CRUD operations for password entries with field-level AES encryption.
 */
public class PasswordRepository {

    private final DatabaseConnection databaseConnection;
    private final CryptoService cryptoService;

    public PasswordRepository(DatabaseConnection databaseConnection, CryptoService cryptoService) {
        this.databaseConnection = databaseConnection;
        this.cryptoService = cryptoService;
    }

    /**
     * Returns all password entries, optionally filtered by search query.
     *
     * @param encryptionKey active session key
     * @param searchQuery   optional filter for site/login (case-insensitive)
     * @return list of entries
     * @throws SQLException on database error
     */
    public List<PasswordEntry> findAll(SecretKey encryptionKey, String searchQuery) throws SQLException {
        String sql = """
                SELECT id, site, login, password_encrypted, notes_encrypted, created_at, updated_at
                FROM passwords
                WHERE (? = '' OR LOWER(site) LIKE LOWER(?) OR LOWER(login) LIKE LOWER(?))
                ORDER BY site COLLATE NOCASE, login COLLATE NOCASE
                """;
        String pattern = searchQuery == null || searchQuery.isBlank() ? "" : "%" + searchQuery.trim() + "%";
        List<PasswordEntry> entries = new ArrayList<>();

        try (PreparedStatement ps = databaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, pattern.isEmpty() ? "" : pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(mapRow(rs, encryptionKey));
                }
            }
        }
        return entries;
    }

    /**
     * Finds a single entry by id.
     *
     * @param id            entry id
     * @param encryptionKey active session key
     * @return optional entry
     * @throws SQLException on database error
     */
    public Optional<PasswordEntry> findById(long id, SecretKey encryptionKey) throws SQLException {
        String sql = """
                SELECT id, site, login, password_encrypted, notes_encrypted, created_at, updated_at
                FROM passwords WHERE id = ?
                """;
        try (PreparedStatement ps = databaseConnection.getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs, encryptionKey));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Inserts a new password entry.
     *
     * @param entry         entry to insert
     * @param encryptionKey active session key
     * @return generated id
     * @throws SQLException on database error
     */
    public long insert(PasswordEntry entry, SecretKey encryptionKey) throws SQLException {
        String sql = """
                INSERT INTO passwords (site, login, password_encrypted, notes_encrypted, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        Instant now = Instant.now();
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);

        var connection = databaseConnection.getConnection();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, entry.getSite());
            ps.setString(2, entry.getLogin());
            ps.setString(3, cryptoService.encrypt(entry.getPassword(), encryptionKey));
            ps.setString(4, cryptoService.encrypt(nullToEmpty(entry.getNotes()), encryptionKey));
            ps.setLong(5, now.toEpochMilli());
            ps.setLong(6, now.toEpochMilli());
            ps.executeUpdate();
        }

        long id;
        try (var stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
            if (!rs.next()) {
                throw new SQLException("Failed to obtain generated id");
            }
            id = rs.getLong(1);
        }
        entry.setId(id);
        return id;
    }

    /**
     * Updates an existing password entry.
     *
     * @param entry         entry to update
     * @param encryptionKey active session key
     * @throws SQLException on database error
     */
    public void update(PasswordEntry entry, SecretKey encryptionKey) throws SQLException {
        String sql = """
                UPDATE passwords
                SET site = ?, login = ?, password_encrypted = ?, notes_encrypted = ?, updated_at = ?
                WHERE id = ?
                """;
        Instant now = Instant.now();
        entry.setUpdatedAt(now);

        try (PreparedStatement ps = databaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, entry.getSite());
            ps.setString(2, entry.getLogin());
            ps.setString(3, cryptoService.encrypt(entry.getPassword(), encryptionKey));
            ps.setString(4, cryptoService.encrypt(nullToEmpty(entry.getNotes()), encryptionKey));
            ps.setLong(5, now.toEpochMilli());
            ps.setLong(6, entry.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Deletes an entry by id.
     *
     * @param id entry id
     * @throws SQLException on database error
     */
    public void delete(long id) throws SQLException {
        try (PreparedStatement ps = databaseConnection.getConnection()
                .prepareStatement("DELETE FROM passwords WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Deletes multiple entries by id.
     *
     * @param ids entry ids
     * @throws SQLException on database error
     */
    public void deleteByIds(List<Long> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        String placeholders = "?,".repeat(ids.size());
        placeholders = placeholders.substring(0, placeholders.length() - 1);
        String sql = "DELETE FROM passwords WHERE id IN (" + placeholders + ")";
        try (PreparedStatement ps = databaseConnection.getConnection().prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) {
                ps.setLong(i + 1, ids.get(i));
            }
            ps.executeUpdate();
        }
    }

    /**
     * Deletes all password entries.
     *
     * @throws SQLException on database error
     */
    public void deleteAll() throws SQLException {
        try (PreparedStatement ps = databaseConnection.getConnection()
                .prepareStatement("DELETE FROM passwords")) {
            ps.executeUpdate();
        }
    }

    /**
     * Returns the number of stored password entries.
     *
     * @return entry count
     * @throws SQLException on database error
     */
    public int count() throws SQLException {
        try (var stmt = databaseConnection.getConnection().createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) FROM passwords")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /**
     * Stores the master password verifier hash.
     *
     * @param verifier base64 verifier hash
     * @throws SQLException on database error
     */
    public void saveMasterVerifier(String verifier) throws SQLException {
        String sql = """
                INSERT INTO settings (key, value) VALUES ('master_verifier', ?)
                ON CONFLICT(key) DO UPDATE SET value = excluded.value
                """;
        try (PreparedStatement ps = databaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, verifier);
            ps.executeUpdate();
        }
    }

    /**
     * Loads the stored master password verifier.
     *
     * @return verifier hash
     * @throws SQLException on database error
     */
    public String loadMasterVerifier() throws SQLException {
        try (var stmt = databaseConnection.getConnection().createStatement();
             var rs = stmt.executeQuery("SELECT value FROM settings WHERE key = 'master_verifier'")) {
            if (rs.next()) {
                return rs.getString(1);
            }
        }
        throw new SQLException("Master verifier not found");
    }

    private PasswordEntry mapRow(ResultSet rs, SecretKey encryptionKey) throws SQLException {
        PasswordEntry entry = new PasswordEntry();
        entry.setId(rs.getLong("id"));
        entry.setSite(rs.getString("site"));
        entry.setLogin(rs.getString("login"));
        entry.setPassword(cryptoService.decrypt(rs.getString("password_encrypted"), encryptionKey));
        entry.setNotes(cryptoService.decrypt(rs.getString("notes_encrypted"), encryptionKey));
        entry.setCreatedAt(Instant.ofEpochMilli(rs.getLong("created_at")));
        entry.setUpdatedAt(Instant.ofEpochMilli(rs.getLong("updated_at")));
        return entry;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
