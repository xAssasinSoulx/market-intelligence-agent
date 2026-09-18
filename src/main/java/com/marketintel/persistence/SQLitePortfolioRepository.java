package com.marketintel.persistence;

import com.marketintel.model.AssetType;
import com.marketintel.model.Portfolio;
import com.marketintel.model.PortfolioPosition;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLitePortfolioRepository
        implements PortfolioRepository {

    private final DatabaseManager databaseManager;

    public SQLitePortfolioRepository(
            DatabaseManager databaseManager) {

        this.databaseManager = databaseManager;
    }

    @Override
    public Portfolio loadForUser(long userId) {

        String sql = """
                SELECT symbol, quantity, asset_type
                FROM portfolio_positions
                WHERE user_id = ?
                ORDER BY symbol;
                """;

        Portfolio portfolio =
                new Portfolio(userId);

        try (Connection connection =
                     databaseManager.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, userId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {

                    PortfolioPosition position =
                            new PortfolioPosition(
                                    resultSet.getString("symbol"),
                                    new BigDecimal(
                                            resultSet.getString("quantity")
                                    ),
                                    AssetType.valueOf(
                                            resultSet.getString("asset_type")
                                    )
                            );

                    portfolio.addPosition(position);
                }
            }

            return portfolio;

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "Failed to load portfolio.",
                    e
            );
        }
    }

    @Override
    public void saveForUser(
            long userId,
            Portfolio portfolio) {

        String deleteSql = """
                DELETE FROM portfolio_positions
                WHERE user_id = ?;
                """;

        String insertSql = """
                INSERT INTO portfolio_positions (
                    user_id,
                    symbol,
                    quantity,
                    asset_type
                )
                VALUES (?, ?, ?, ?);
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

                    for (PortfolioPosition position
                            : portfolio.getPositions()) {

                        insert.setLong(1, userId);

                        insert.setString(
                                2,
                                position.getSymbol()
                                        .toUpperCase()
                        );

                        insert.setString(
                                3,
                                position.getQuantity()
                                        .toPlainString()
                        );

                        insert.setString(
                                4,
                                position.getAssetType()
                                        .name()
                        );

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
                    "Failed to save portfolio.",
                    e
            );
        }
    }
}