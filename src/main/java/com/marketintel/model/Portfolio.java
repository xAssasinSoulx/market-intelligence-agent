package com.marketintel.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Portfolio {

    private final long userId;
    private final List<PortfolioPosition> positions;

    public Portfolio(long userId) {
        this(userId, new ArrayList<>());
    }

    public Portfolio(
            long userId,
            List<PortfolioPosition> positions) {

        this.userId = userId;
        this.positions = new ArrayList<>(positions);
    }

    public long getUserId() {
        return userId;
    }

    public List<PortfolioPosition> getPositions() {
        return Collections.unmodifiableList(positions);
    }

    public void addPosition(PortfolioPosition position) {

        boolean exists = positions.stream()
                .anyMatch(existing ->
                        existing.getSymbol()
                                .equalsIgnoreCase(position.getSymbol()));

        if (exists) {
            throw new IllegalArgumentException(
                    "Portfolio already contains symbol: "
                            + position.getSymbol()
            );
        }

        positions.add(position);
    }

    public boolean removePosition(String symbol) {
        return positions.removeIf(
                position ->
                        position.getSymbol()
                                .equalsIgnoreCase(symbol)
        );
    }
}