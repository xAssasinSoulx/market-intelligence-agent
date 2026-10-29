package com.marketintel.providers;

import java.util.Locale;

final class Symbols {

    private Symbols() {
    }

    static String require(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be blank.");
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }
}
