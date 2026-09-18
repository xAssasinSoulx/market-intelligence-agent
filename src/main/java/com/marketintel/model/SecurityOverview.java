package com.marketintel.model;

import java.math.BigDecimal;

public class SecurityOverview {

    private final String symbol;
    private final String name;
    private final String description;
    private final String exchange;
    private final String currency;
    private final String sector;
    private final String industry;
    private final BigDecimal marketCapitalization;

    public SecurityOverview(
            String symbol,
            String name,
            String description,
            String exchange,
            String currency,
            String sector,
            String industry,
            BigDecimal marketCapitalization) {

        this.symbol = symbol;
        this.name = name;
        this.description = description;
        this.exchange = exchange;
        this.currency = currency;
        this.sector = sector;
        this.industry = industry;
        this.marketCapitalization = marketCapitalization;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getExchange() {
        return exchange;
    }

    public String getCurrency() {
        return currency;
    }

    public String getSector() {
        return sector;
    }

    public String getIndustry() {
        return industry;
    }

    public BigDecimal getMarketCapitalization() {
        return marketCapitalization;
    }
}