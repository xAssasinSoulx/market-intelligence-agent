package com.marketintel.model;

import java.math.BigDecimal;
import java.util.Objects;

public class ETFHolding {

    private final String symbol;
    private final String description;
    private final BigDecimal weight;
    private final String country;

    public ETFHolding(
            String symbol,
            String description,
            BigDecimal weight) {

        this(symbol, description, weight, null);
    }

    public ETFHolding(
            String symbol,
            String description,
            BigDecimal weight,
            String country) {

        this.symbol = symbol;
        this.description = description;
        this.weight = Objects.requireNonNull(
                weight,
                "Weight cannot be null."
        );
        this.country = country == null || country.isBlank()
                ? null
                : country.trim();
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

    /**
     * Country label supplied with the holding, when the provider sent one.
     * A null value means the field was absent. It is not inferred.
     */
    public String getCountry() {
        return country;
    }
}