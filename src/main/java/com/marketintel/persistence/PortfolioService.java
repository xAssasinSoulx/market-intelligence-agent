package com.marketintel.services;

import com.marketintel.model.AssetType;
import com.marketintel.model.Portfolio;
import com.marketintel.model.PortfolioPosition;
import com.marketintel.persistence.PortfolioRepository;

import java.math.BigDecimal;

public class PortfolioService {

    private final PortfolioRepository repository;

    public PortfolioService(
            PortfolioRepository repository) {

        this.repository = repository;
    }

    public Portfolio getPortfolio(long userId) {
        validateUserId(userId);
        return repository.loadForUser(userId);
    }

    public void addPosition(
            long userId,
            String symbol,
            BigDecimal quantity,
            AssetType assetType) {

        validateUserId(userId);

        String normalizedSymbol =
                normalizeSymbol(symbol);

        validateQuantity(quantity);

        if (assetType == null) {
            throw new IllegalArgumentException(
                    "Asset type cannot be null."
            );
        }

        Portfolio portfolio =
                repository.loadForUser(userId);

        portfolio.addPosition(
                new PortfolioPosition(
                        normalizedSymbol,
                        quantity,
                        assetType
                )
        );

        repository.saveForUser(
                userId,
                portfolio
        );
    }

    public boolean removePosition(
            long userId,
            String symbol) {

        validateUserId(userId);

        String normalizedSymbol =
                normalizeSymbol(symbol);

        Portfolio portfolio =
                repository.loadForUser(userId);

        boolean removed =
                portfolio.removePosition(
                        normalizedSymbol
                );

        if (removed) {
            repository.saveForUser(
                    userId,
                    portfolio
            );
        }

        return removed;
    }

    private String normalizeSymbol(
            String symbol) {

        if (symbol == null
                || symbol.isBlank()) {

            throw new IllegalArgumentException(
                    "Symbol cannot be blank."
            );
        }

        return symbol.trim().toUpperCase();
    }

    private void validateQuantity(
            BigDecimal quantity) {

        if (quantity == null
                || quantity.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            throw new IllegalArgumentException(
                    "Quantity must be greater than zero."
            );
        }
    }

    private void validateUserId(long userId) {

        if (userId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid user ID."
            );
        }
    }
}