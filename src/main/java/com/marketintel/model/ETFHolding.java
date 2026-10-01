package com.marketintel.model;

import java.math.BigDecimal;
import java.util.Objects;

public class ETFHolding {

    private final String symbol;
    private final String description;
    private final BigDecimal weight;

    public ETFHolding(
            String symbol,
            String description,
            BigDecimal weight) {

        this.symbol = symbol;
        this.description = description;
        this.weight = Objects.requireNonNull(
                weight,
                "Weight cannot be null."
        );
    }

    public String getSymbol() {
        return symbol;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getWeight() {
        return weight;
    }
}