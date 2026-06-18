package com.safebox.repository;

import com.safebox.util.AppPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite database connection and schema initialization.
 */
public class DatabaseConnection {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseConnection.class);
    private static final String JDBC_URL_PREFIX = "jdbc:sqlite:";

    private Connection connection;

    /**
     * Opens a connection to the local SQLite database.
     *
     * @throws SQLException if connection fails
     */
    public void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return;
        }
        try {
            Files.createDirectories(AppPaths.dataDir());
        } catch (IOException e) {
            throw new SQLException("Failed to create data directory", e);
        }
        String url = JDBC_URL_PREFIX + AppPaths.databaseFile().toAbsolutePath();
        connection = DriverManager.getConnection(url);
        connection.setAutoCommit(true);
        initializeSchema();
        LOG.info("Database connected at {}", AppPaths.databaseFile());
    }

    /**
     * Returns the active JDBC connection.
     *
     * @return connection
     * @throws SQLException if not connected
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect();
        }
        return connection;
    }

    /**
     * Closes the database connection.
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOG.warn("Failed to close database connection", e);
            } finally {
                connection = null;
            }
        }
    }

    /**
     * Checks whether the vault has been initialized.
     *
     * @return true if master verifier exists
     */
    public boolean isVaultInitialized() {
        try {
            connect();
            try (var stmt = connection.createStatement();
                 var rs = stmt.executeQuery("SELECT value FROM settings WHERE key = 'master_verifier'")) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOG.error("Failed to check vault state", e);
            return false;
        }
    }

    private void initializeSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS passwords (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        site TEXT NOT NULL,
                        login TEXT NOT NULL,
                        password_encrypted TEXT NOT NULL,
                        notes_encrypted TEXT,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS settings (
                        key TEXT PRIMARY KEY,
                        value TEXT NOT NULL
                    )
                    """);
        }
    }
}
