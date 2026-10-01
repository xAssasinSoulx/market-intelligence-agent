package com.marketintel.model;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExposureResult {

    private final String symbol;

    private final Map<String, BigDecimal>
            categories;

    public ExposureResult(
            String symbol,
            Map<String, BigDecimal> categories) {

        this.symbol = symbol;

        this.categories =
                new LinkedHashMap<>(
                        categories
                );
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
}