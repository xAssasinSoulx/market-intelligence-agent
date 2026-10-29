package com.marketintel.services;

import com.marketintel.model.AssetType;
import com.marketintel.model.ETFHolding;
import com.marketintel.model.PortfolioExposureInput;
import com.marketintel.model.PortfolioExposureResult;
import com.marketintel.model.PortfolioExposureResult.CurrencyBook;
import com.marketintel.model.PortfolioExposureResult.HoldingLine;
import com.marketintel.model.PortfolioExposureResult.PositionLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Turns priced positions into sector weights and ETF look-through.
 * Market value is quantity times price. Different currencies stay apart.
 */
public final class PortfolioExposureCalculator {

    static final String UNKNOWN = "Unknown";
    static final String UNKNOWN_CURRENCY = "UNKNOWN";
    private static final int WEIGHT_SCALE = 8;

    public PortfolioExposureResult calculate(List<PortfolioExposureInput> inputs) {
        if (inputs == null) {
            throw new IllegalArgumentException("Portfolio inputs cannot be null.");
        }

        List<String> limitations = new ArrayList<>();
        List<PositionLine> lines = new ArrayList<>();
        Map<String, List<PortfolioExposureInput>> included = new TreeMap<>();

        if (inputs.isEmpty()) {
            limitations.add("The portfolio has no positions.");
            return new PortfolioExposureResult(List.of(), List.of(), limitations);
        }

        for (PortfolioExposureInput input : inputs) {
            PositionLine line = value(input, limitations);
            lines.add(line);
            if (line.isIncluded()) {
                included.computeIfAbsent(line.getCurrency(), key -> new ArrayList<>())
                        .add(input);
            }
        }

        if (included.size() > 1) {
            limitations.add(
                    "Market values in different currencies were not combined."
            );
        }

        List<CurrencyBook> books = new ArrayList<>();
        for (Map.Entry<String, List<PortfolioExposureInput>> entry : included.entrySet()) {
            books.add(book(entry.getKey(), entry.getValue(), limitations));
        }

        return new PortfolioExposureResult(lines, books, limitations);
    }

    private PositionLine value(PortfolioExposureInput input, List<String> limitations) {
        String symbol = input.getSymbol();
        BigDecimal price = input.getPrice();
        String currency = normalizeCurrency(input.getCurrency());
        boolean priced = price != null && price.signum() > 0;

        if (!priced) {
            limitations.add(symbol + " has no positive price and was excluded from the weights.");
            appendNote(limitations, input.getNote());
            return new PositionLine(
                    symbol,
                    input.getAssetType(),
                    input.getQuantity(),
                    price,
                    currency,
                    null,
                    false
            );
        }

        if (currency == null) {
            currency = UNKNOWN_CURRENCY;
            limitations.add(symbol + " has no currency and was kept in its own book.");
        }

        appendNote(limitations, input.getNote());
        return new PositionLine(
                symbol,
                input.getAssetType(),
                input.getQuantity(),
                price,
                currency,
                price.multiply(input.getQuantity()),
                true
        );
    }

    private CurrencyBook book(
            String currency,
            List<PortfolioExposureInput> positions,
            List<String> limitations) {

        BigDecimal total = BigDecimal.ZERO;
        for (PortfolioExposureInput input : positions) {
            total = total.add(input.getPrice().multiply(input.getQuantity()));
        }

        Map<String, BigDecimal> sectors = new LinkedHashMap<>();
        Map<String, BigDecimal> holdingWeights = new LinkedHashMap<>();
        Map<String, String> holdingSources = new LinkedHashMap<>();

        for (PortfolioExposureInput input : positions) {
            BigDecimal marketValue = input.getPrice().multiply(input.getQuantity());
            BigDecimal positionWeight = marketValue.divide(total, WEIGHT_SCALE, RoundingMode.HALF_UP);
            addSectors(input, positionWeight, sectors, limitations);
            if (input.getAssetType() == AssetType.ETF) {
                addLookThrough(input, positionWeight, holdingWeights, holdingSources, limitations);
            }
        }

        List<HoldingLine> lookThrough = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : holdingWeights.entrySet()) {
            lookThrough.add(new HoldingLine(
                    entry.getKey(),
                    entry.getValue(),
                    holdingSources.get(entry.getKey())
            ));
        }
        lookThrough.sort(Comparator.comparing(HoldingLine::getPortfolioWeight).reversed());

        return new CurrencyBook(
                currency,
                total,
                sortByWeight(sectors),
                lookThrough
        );
    }

    private void addSectors(
            PortfolioExposureInput input,
            BigDecimal positionWeight,
            Map<String, BigDecimal> sectors,
            List<String> limitations) {

        if (input.getSectorWeights().isEmpty()) {
            add(sectors, UNKNOWN, positionWeight);
            return;
        }

        BigDecimal assigned = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : input.getSectorWeights().entrySet()) {
            BigDecimal weight = entry.getValue();
            if (weight == null || weight.signum() <= 0) {
                continue;
            }
            add(sectors, entry.getKey(), positionWeight.multiply(weight));
            assigned = assigned.add(weight);
        }

        if (assigned.compareTo(BigDecimal.ONE) < 0) {
            add(sectors, UNKNOWN, positionWeight.multiply(BigDecimal.ONE.subtract(assigned)));
        } else if (assigned.compareTo(new BigDecimal("1.01")) > 0) {
            limitations.add(
                    input.getSymbol()
                            + " sector weights sum above 100% and were not rescaled."
            );
        }
    }

    private void addLookThrough(
            PortfolioExposureInput input,
            BigDecimal positionWeight,
            Map<String, BigDecimal> holdingWeights,
            Map<String, String> holdingSources,
            List<String> limitations) {

        if (input.getHoldings().isEmpty()) {
            limitations.add(
                    input.getSymbol()
                            + " has no holding list. Its sector weights are the fund allocation, not holding look-through."
            );
            return;
        }

        for (ETFHolding holding : input.getHoldings()) {
            if (holding.getSymbol() == null || holding.getSymbol().isBlank()) {
                continue;
            }
            String symbol = holding.getSymbol().trim().toUpperCase(Locale.ROOT);
            BigDecimal contribution = positionWeight.multiply(holding.getWeight());
            add(holdingWeights, symbol, contribution);
            holdingSources.merge(symbol, input.getSymbol(), (left, right) -> left + ", " + right);
        }
    }

    private static void add(Map<String, BigDecimal> target, String key, BigDecimal amount) {
        target.merge(key, amount, BigDecimal::add);
    }

    private static Map<String, BigDecimal> sortByWeight(Map<String, BigDecimal> weights) {
        List<Map.Entry<String, BigDecimal>> entries = new ArrayList<>(weights.entrySet());
        entries.sort(Map.Entry.<String, BigDecimal>comparingByValue().reversed());
        Map<String, BigDecimal> sorted = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : entries) {
            sorted.put(entry.getKey(), entry.getValue());
        }
        return sorted;
    }

    private static String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return null;
        }
        return currency.trim().toUpperCase(Locale.ROOT);
    }

    private static void appendNote(List<String> limitations, String note) {
        if (note != null && !note.isBlank()) {
            limitations.add(note.trim());
        }
    }
}
