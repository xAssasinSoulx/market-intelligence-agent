package com.marketintel.persistence;

import com.marketintel.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Optional;

public class SQLiteUserRepository implements UserRepository {

    private final DatabaseManager databaseManager;

    public SQLiteUserRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public User save(User user) {

        String sql = """
                INSERT INTO users (
                    username,
                    password_hash,
                    created_at
                )
                VALUES (?, ?, ?);
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(
                             sql,
                             Statement.RETURN_GENERATED_KEYS
                     )) {

            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPasswordHash());
            statement.setString(
                    3,
                    user.getCreatedAt().toString()
            );

            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {

                if (!keys.next()) {
                    throw new SQLException(
                            "Database did not return a generated user ID."
                    );
                }

                long generatedId = keys.getLong(1);

                return new User(
                        generatedId,
                        user.getUsername(),
                        user.getPasswordHash(),
                        user.getCreatedAt()
                );
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to save user.",
                    e
            );
        }
    }

    @Override
    public Optional<User> findById(long userId) {

        String sql = """
                SELECT id, username, password_hash, created_at
                FROM users
                WHERE id = ?;
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return Optional.of(mapUser(resultSet));
                }

                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to retrieve user.",
                    e
            );
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {

        String sql = """
                SELECT id, username, password_hash, created_at
                FROM users
                WHERE username = ?;
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return Optional.of(mapUser(resultSet));
                }

                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to retrieve user.",
                    e
            );
        }
    }

    @Override
    public boolean existsByUsername(String username) {

        String sql = """
                SELECT 1
                FROM users
                WHERE username = ?
                LIMIT 1;
                """;

        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to check username.",
                    e
            );
        }
    }

    private User mapUser(ResultSet resultSet)
            throws SQLException {

        return new User(
                resultSet.getLong("id"),
                resultSet.getString("username"),
                resultSet.getString("password_hash"),
                LocalDateTime.parse(
                        resultSet.getString("created_at")
                )
        );
    }
}