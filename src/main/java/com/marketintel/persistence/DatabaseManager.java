package com.marketintel.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private final String databaseUrl;

    public DatabaseManager(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    public void initialize() {

        createDatabaseDirectory();

        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE COLLATE NOCASE,
                password_hash TEXT NOT NULL,
                created_at TEXT NOT NULL
            );
            """;

        String createPortfolioPositionsTable = """
            CREATE TABLE IF NOT EXISTS portfolio_positions (
                user_id INTEGER NOT NULL,
                symbol TEXT NOT NULL COLLATE NOCASE,
                quantity TEXT NOT NULL,
                asset_type TEXT NOT NULL,

                PRIMARY KEY (user_id, symbol),

                FOREIGN KEY (user_id)
                    REFERENCES users(id)
                    ON DELETE CASCADE
            );
            """;

        String createWatchlistTable = """
            CREATE TABLE IF NOT EXISTS watchlist_items (
                user_id INTEGER NOT NULL,
                symbol TEXT NOT NULL COLLATE NOCASE,

                PRIMARY KEY (user_id, symbol),

                FOREIGN KEY (user_id)
                    REFERENCES users(id)
                    ON DELETE CASCADE
            );
            """;

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(createUsersTable);
            statement.execute(createPortfolioPositionsTable);
            statement.execute(createWatchlistTable);

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "Failed to initialize database.",
                    e
            );
        }
    }

    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(databaseUrl);

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;");
        }

        return connection;
    }

    private void createDatabaseDirectory() {
        if (!databaseUrl.startsWith("jdbc:sqlite:")) {
            return;
        }

        String pathString =
                databaseUrl.substring("jdbc:sqlite:".length());

        // Special SQLite values such as :memory:
        if (pathString.startsWith(":")) {
            return;
        }

        Path databasePath = Path.of(pathString);
        Path parent = databasePath.getParent();

        if (parent == null) {
            return;
        }

        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to create database directory.",
                    e
            );
        }
    }
}