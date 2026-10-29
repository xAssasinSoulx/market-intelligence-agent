package com.marketintel.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One sentiment score for a symbol. {@code label} is this application's
 * display band for {@code score}. A null change means the payload had
 * fewer than two points.
 */
public final class SentimentReading {

    private final String symbol;
    private final String source;
    private final BigDecimal score;
    private final String label;
    private final BigDecimal change;
    private final String asOf;
    private final String note;

    public SentimentReading(
            String symbol,
            String source,
            BigDecimal score,
            String label,
            BigDecimal change,
            String asOf,
            String note) {

        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null.");
        this.source = Objects.requireNonNull(source, "Source cannot be null.");
        this.score = Objects.requireNonNull(score, "Score cannot be null.");
        this.label = label == null ? "" : label;
        this.change = change;
        this.asOf = asOf == null ? "" : asOf;
        this.note = note == null ? "" : note;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSource() {
        return source;
    }

    public BigDecimal getScore() {
        return score;
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal getChange() {
        return change;
    }

    public String getAsOf() {
        return asOf;
    }

    public String getNote() {
        return note;
    }
}
