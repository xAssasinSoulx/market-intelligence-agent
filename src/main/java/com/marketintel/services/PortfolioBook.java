package com.marketintel.services;

import com.marketintel.model.MarketQuote;
import com.marketintel.model.PortfolioPosition;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Values a portfolio from quotes that have already been fetched.
 * A missing quote is named and left out of the total.
 */
public final class PortfolioBook {

    private final List<Row> priced;
    private final List<String> unpriced;
    private final BigDecimal marketValue;
    private final BigDecimal dayChange;

    private PortfolioBook(
            List<Row> priced,
            List<String> unpriced,
            BigDecimal marketValue,
            BigDecimal dayChange) {

        this.priced = List.copyOf(priced);
        this.unpriced = List.copyOf(unpriced);
        this.marketValue = marketValue;
        this.dayChange = dayChange;
    }

    public static PortfolioBook value(
            List<PortfolioPosition> positions,
            Map<String, MarketQuote> quotes) {

        List<Row> priced = new ArrayList<>();
        List<String> unpriced = new ArrayList<>();
        BigDecimal marketValue = BigDecimal.ZERO;
        BigDecimal dayChange = BigDecimal.ZERO;

        if (positions != null) {
            for (PortfolioPosition position : positions) {
                MarketQuote quote = quoteFor(quotes, position.getSymbol());
                if (quote == null) {
                    unpriced.add(position.getSymbol());
                    continue;
                }
                BigDecimal value = quote.getPrice().multiply(position.getQuantity());
                BigDecimal change = quote.getPrice()
                        .subtract(quote.getPreviousClose())
                        .multiply(position.getQuantity());
                priced.add(new Row(
                        position.getSymbol(),
                        position.getQuantity(),
                        quote.getPrice(),
                        quote.getPercentChange(),
                        value,
                        change
                ));
                marketValue = marketValue.add(value);
                dayChange = dayChange.add(change);
            }
        }

        return new PortfolioBook(priced, unpriced, marketValue, dayChange);
    }

    public List<Row> priced() {
        return priced;
    }

    public List<String> unpriced() {
        return unpriced;
    }

    public BigDecimal marketValue() {
        return marketValue;
    }

    public BigDecimal dayChange() {
        return dayChange;
    }

    /**
     * Day change as a percent of the previous market value.
     * Empty when that previous value is zero.
     */
    public BigDecimal dayPercent() {
        BigDecimal previous = marketValue.subtract(dayChange);
        if (previous.signum() == 0) {
            return null;
        }
        return dayChange
                .multiply(new BigDecimal("100"))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }

    private static MarketQuote quoteFor(Map<String, MarketQuote> quotes, String symbol) {
        if (quotes == null || symbol == null) {
            return null;
        }
        MarketQuote direct = quotes.get(symbol);
        if (direct != null) {
            return direct;
        }
        return quotes.get(symbol.toUpperCase(Locale.ROOT));
    }

    public record Row(
            String symbol,
            BigDecimal quantity,
            BigDecimal price,
            BigDecimal percentChange,
            BigDecimal marketValue,
            BigDecimal dayChange) {
    }
}
