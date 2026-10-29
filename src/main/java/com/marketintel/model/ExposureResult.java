package com.marketintel.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExposureResult {

    private final String symbol;

    private final Map<String, BigDecimal>
            categories;

    private final String note;

    public ExposureResult(
            String symbol,
            Map<String, BigDecimal> categories) {

        this(symbol, categories, null);
    }

    public ExposureResult(
            String symbol,
            Map<String, BigDecimal> categories,
            String note) {

        this.symbol = symbol;

        this.categories =
                new LinkedHashMap<>(
                        categories
                );

        this.note = note == null || note.isBlank()
                ? null
                : note.trim();
    }

    public String getSymbol() {
        return symbol;
    }

    public Map<String, BigDecimal>
    getCategories() {

        return Collections.unmodifiableMap(
                categories
        );
    }

    /**
     * Explains what the weights measure. Null for a plain sector breakdown.
     */
    public String getNote() {
        return note;
    }
}