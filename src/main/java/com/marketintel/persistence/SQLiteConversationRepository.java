package com.marketintel.persistence;

import com.marketintel.model.ConversationTurn;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SQLiteConversationRepository implements ConversationRepository {

    private static final int MAX_BODY = 1500;
    private static final int MAX_KEPT = 80;

    private final DatabaseManager databaseManager;

    public SQLiteConversationRepository(DatabaseManager databaseManager) {
        if (databaseManager == null) {
            throw new IllegalArgumentException("Database manager cannot be null.");
        }
        this.databaseManager = databaseManager;
    }

    @Override
    public void append(ConversationTurn turn) {
        if (turn == null) {
            throw new IllegalArgumentException("Conversation turn cannot be null.");
        }
        String insert = """
                INSERT INTO conversation_turns (
                    user_id, role, body, symbol, action, created_at
                ) VALUES (?, ?, ?, ?, ?, ?);
                """;
        String trim = """
                DELETE FROM conversation_turns
                WHERE user_id = ?
                  AND id NOT IN (
                      SELECT id FROM conversation_turns
                      WHERE user_id = ?
                      ORDER BY id DESC
                      LIMIT ?
                  );
                """;

        try (Connection connection = databaseManager.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(insert)) {
                statement.setLong(1, turn.userId());
                statement.setString(2, turn.role());
                statement.setString(3, clip(turn.body()));
                statement.setString(4, blankToNull(turn.symbol()));
                statement.setString(5, blankToNull(turn.action()));
                statement.setString(6, Instant.now().toString());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(trim)) {
                statement.setLong(1, turn.userId());
                statement.setLong(2, turn.userId());
                statement.setInt(3, MAX_KEPT);
                statement.executeUpdate();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to store the conversation turn.", exception);
        }
    }

    @Override
    public List<ConversationTurn> recent(long userId, int limit) {
        int bounded = Math.max(1, Math.min(limit, MAX_KEPT));
        String sql = """
                SELECT user_id, role, body, symbol, action
                FROM conversation_turns
                WHERE user_id = ?
                ORDER BY id DESC
                LIMIT ?;
                """;
        List<ConversationTurn> turns = new ArrayList<>();
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setInt(2, bounded);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    turns.add(new ConversationTurn(
                            rows.getLong("user_id"),
                            rows.getString("role"),
                            rows.getString("body"),
                            rows.getString("symbol"),
                            rows.getString("action")
                    ));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load the conversation.", exception);
        }
        Collections.reverse(turns);
        return turns;
    }

    private static String clip(String body) {
        if (body == null) {
            return "";
        }
        String trimmed = body.trim();
        if (trimmed.length() <= MAX_BODY) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_BODY);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
