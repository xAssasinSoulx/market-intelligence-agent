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

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("PRAGMA foreign_keys = ON;");
            statement.execute(createUsersTable);

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