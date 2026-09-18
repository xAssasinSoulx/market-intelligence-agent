package com.marketintel.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class Watchlist {

    private final long userId;
    private final Set<String> symbols;

    public Watchlist(long userId) {
        this(userId, new LinkedHashSet<>());
    }

    public Watchlist(
            long userId,
            Set<String> symbols) {

        this.userId = userId;
        this.symbols = new LinkedHashSet<>();

        symbols.forEach(symbol ->
                this.symbols.add(
                        normalizeSymbol(symbol)
                )
        );
    }

    public long getUserId() {
        return userId;
    }

    public Set<String> getSymbols() {
        return Collections.unmodifiableSet(symbols);
    }

    public void add(String symbol) {

        String normalized =
                normalizeSymbol(symbol);

        if (!symbols.add(normalized)) {
            throw new IllegalArgumentException(
                    "Watchlist already contains symbol: "
                            + normalized
            );
        }
    }

    public boolean remove(String symbol) {
        return symbols.remove(
                normalizeSymbol(symbol)
        );
    }

    public boolean contains(String symbol) {
        return symbols.contains(
                normalizeSymbol(symbol)
        );
    }

    private String normalizeSymbol(String symbol) {

        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException(
                    "Symbol cannot be blank."
            );
        }

        return symbol.trim().toUpperCase();
    }
}