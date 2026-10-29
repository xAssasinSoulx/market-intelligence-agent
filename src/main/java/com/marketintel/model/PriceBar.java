package com.marketintel.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One OHLCV bar. The date is the provider's calendar label, not a
 * converted instant.
 */
public final class PriceBar {

    private final String date;
    private final BigDecimal open;
    private final BigDecimal high;
    private final BigDecimal low;
    private final BigDecimal close;
    private final BigDecimal volume;

    public PriceBar(
            String date,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            BigDecimal volume) {

        if (date == null || date.isBlank()) {
            throw new IllegalArgumentException("Bar date cannot be blank.");
        }
        this.date = date.trim();
        this.open = Objects.requireNonNull(open, "Open cannot be null.");
        this.high = Objects.requireNonNull(high, "High cannot be null.");
        this.low = Objects.requireNonNull(low, "Low cannot be null.");
        this.close = Objects.requireNonNull(close, "Close cannot be null.");
        this.volume = Objects.requireNonNull(volume, "Volume cannot be null.");
    }

    public String getDate() {
        return date;
    }

    public BigDecimal getOpen() {
        return open;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public BigDecimal getClose() {
        return close;
    }

    public BigDecimal getVolume() {
        return volume;
    }
}
