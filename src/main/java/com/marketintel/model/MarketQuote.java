package com.marketintel.model;

import java.math.BigDecimal;
import java.util.Objects;

public class MarketQuote {

    private final String symbol;
    private final BigDecimal price;
    private final BigDecimal previousClose;
    private final BigDecimal percentChange;

    public MarketQuote(
            String symbol,
            BigDecimal price,
            BigDecimal previousClose,
            BigDecimal percentChange) {

        this.symbol = Objects.requireNonNull(symbol);
        this.price = Objects.requireNonNull(price);
        this.previousClose = Objects.requireNonNull(previousClose);
        this.percentChange = Objects.requireNonNull(percentChange);
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getPreviousClose() {
        return previousClose;
    }

    public BigDecimal getPercentChange() {
        return percentChange;
    }
}