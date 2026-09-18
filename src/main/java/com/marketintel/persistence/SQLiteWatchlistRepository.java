package com.marketintel.persistence;

import com.marketintel.model.Watchlist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLiteWatchlistRepository
        implements WatchlistRepository {

    private final DatabaseManager databaseManager;

    public SQLiteWatchlistRepository(
            DatabaseManager databaseManager) {

        this.databaseManager = databaseManager;
    }

    @Override
    public Watchlist loadForUser(long userId) {

        String sql = """
                SELECT symbol
                FROM watchlist_items
                WHERE user_id = ?
                ORDER BY symbol;
                """;

        Watchlist watchlist =
                new Watchlist(userId);

        try (Connection connection =
                     databaseManager.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, userId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {

                    watchlist.add(
                            resultSet.getString("symbol")
                    );
                }
            }

            return watchlist;

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "Failed to load watchlist.",
                    e
            );
        }
    }

    @Override
    public void saveForUser(
            long userId,
            Watchlist watchlist) {

        String deleteSql = """
                DELETE FROM watchlist_items
                WHERE user_id = ?;
                """;

        String insertSql = """
                INSERT INTO watchlist_items (
                    user_id,
                    symbol
                )
                VALUES (?, ?);
                """;

        try (Connection connection =
                     databaseManager.getConnection()) {

            connection.setAutoCommit(false);

            try {

                try (PreparedStatement delete =
                             connection.prepareStatement(
                                     deleteSql
                             )) {

                    delete.setLong(1, userId);
                    delete.executeUpdate();
                }

                try (PreparedStatement insert =
                             connection.prepareStatement(
                                     insertSql
                             )) {

                    for (String symbol
                            : watchlist.getSymbols()) {

                        insert.setLong(1, userId);
                        insert.setString(2, symbol);
                        insert.addBatch();
                    }

                    insert.executeBatch();
                }

                connection.commit();

            } catch (SQLException e) {

                connection.rollback();
                throw e;

            } finally {

                connection.setAutoCommit(true);
            }

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "Failed to save watchlist.",
                    e
            );
        }
    }
}