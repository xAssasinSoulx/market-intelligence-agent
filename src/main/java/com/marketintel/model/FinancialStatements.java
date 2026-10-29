package com.marketintel.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Annual or quarterly statements for one symbol, already translated out
 * of a vendor payload. Periods stay in the order the adapter assembled.
 */
public final class FinancialStatements {

    private final String symbol;
    private final String source;
    private final String currency;
    private final String note;
    private final List<ReportingPeriod> periods;

    public FinancialStatements(
            String symbol,
            String source,
            String currency,
            String note,
            List<ReportingPeriod> periods) {

        this.symbol = Objects.requireNonNull(symbol, "Symbol cannot be null.");
        this.source = Objects.requireNonNull(source, "Source cannot be null.");
        this.currency = currency == null ? "" : currency;
        this.note = note == null ? "" : note;
        this.periods = new ArrayList<>(
                Objects.requireNonNull(periods, "Periods cannot be null.")
        );
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSource() {
        return source;
    }

    public String getCurrency() {
        return currency;
    }

    public String getNote() {
        return note;
    }

    public List<ReportingPeriod> getPeriods() {
        return Collections.unmodifiableList(periods);
    }
}
