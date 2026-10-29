package com.marketintel.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Recent price bars plus the latest indicator reading the history
 * adapter chose to attach. A null RSI means the indicator call did not
 * return a number.
 */
public final class PriceHistory {

    private final String symbol;
    private final String source;
    private final String interval;
    private final String currency;
    private final List<PriceBar> bars;
    private final BigDecimal latestRsi;
    private final String note;

    public PriceHistory(
            String symbol,
            String source,
            String interval,
            String currency,
            List<PriceBar> bars,
            BigDecimal latestRsi,
            String note) {

        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null.");
        this.source = Objects.requireNonNull(source, "Source cannot be null.");
        this.interval = interval == null ? "" : interval;
        this.currency = currency == null ? "" : currency;
        this.bars = new ArrayList<>(Objects.requireNonNull(bars, "Bars cannot be null."));
        this.latestRsi = latestRsi;
        this.note = note == null ? "" : note;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSource() {
        return source;
    }

    public String getInterval() {
        return interval;
    }

    public String getCurrency() {
        return currency;
    }

    public List<PriceBar> getBars() {
        return Collections.unmodifiableList(bars);
    }

    public BigDecimal getLatestRsi() {
        return latestRsi;
    }

    public String getNote() {
        return note;
    }
}
