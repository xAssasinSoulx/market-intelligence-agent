package com.marketintel.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class MarketQuote {

    private final String symbol;
    private final BigDecimal price;
    private final BigDecimal previousClose;
    private final BigDecimal percentChange;
    private final Instant lastTradeAt;

    public MarketQuote(
            String symbol,
            BigDecimal price,
            BigDecimal previousClose,
            BigDecimal percentChange) {

        this(symbol, price, previousClose, percentChange, null);
    }

    /**
     * @param lastTradeAt time of the last trade the provider reported,
     *                    or null when the provider does not supply one
     */
    public MarketQuote(
            String symbol,
            BigDecimal price,
            BigDecimal previousClose,
            BigDecimal percentChange,
            Instant lastTradeAt) {

        this.symbol = Objects.requireNonNull(symbol);
        this.price = Objects.requireNonNull(price);
        this.previousClose = Objects.requireNonNull(previousClose);
        this.percentChange = Objects.requireNonNull(percentChange);
        this.lastTradeAt = lastTradeAt;
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

    public Optional<Instant> getLastTradeAt() {
        return Optional.ofNullable(lastTradeAt);
    }
}
