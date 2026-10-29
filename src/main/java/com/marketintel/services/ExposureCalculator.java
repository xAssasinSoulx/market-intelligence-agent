package com.marketintel.services;

import com.marketintel.model.ETFHolding;
import com.marketintel.model.ETFProfile;
import com.marketintel.model.ExposureResult;
import com.marketintel.model.OverlapResult;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExposureCalculator {

    static final String UNKNOWN_COUNTRY = "Unknown";

    private static final BigDecimal NEAR_ONE_LOW = new BigDecimal("0.99");
    private static final BigDecimal NEAR_ONE_HIGH = new BigDecimal("1.01");

    private static final String HOLDING_NOTE =
            "Weights sum the country field on each reported holding. "
                    + "A blank country is Unknown. "
                    + "This is the provider's country label for the holding, "
                    + "not the fund's domicile and not revenue by country.";

    private static final String REPORTED_NOTE =
            "Weights are the provider's reported country exposure for the fund. "
                    + "They are not a sum of the listing countries of the holdings, "
                    + "and they are not revenue by country.";

    public ExposureResult
    calculateSectorExposure(
            ETFProfile profile) {

        if (profile == null) {

            throw new IllegalArgumentException(
                    "ETF profile cannot be null."
            );
        }

        return new ExposureResult(
                profile.getSymbol(),
                profile.getSectorWeights()
        );
    }

    /**
     * Sums holding weights by the country the provider attached to each
     * holding. Holdings with no country go to Unknown. If every holding
     * lacks a country, this refuses to invent an allocation.
     */
    public ExposureResult calculateGeographicExposure(ETFProfile profile) {
        return calculateGeographicExposure(profile, HOLDING_NOTE);
    }

    public ExposureResult calculateGeographicExposure(ETFProfile profile, String baseNote) {

        if (profile == null) {
            throw new IllegalArgumentException("ETF profile cannot be null.");
        }

        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        Map<String, String> labels = new LinkedHashMap<>();

        for (ETFHolding holding : profile.getHoldings()) {
            addCountry(
                    totals,
                    labels,
                    holding.getCountry(),
                    holding.getWeight()
            );
        }

        if (!hasNamedCountry(totals)) {
            throw new IllegalArgumentException(
                    "No country was supplied for the holdings of "
                            + profile.getSymbol()
                            + ". A geographic allocation was not estimated."
            );
        }

        String note = baseNote == null || baseNote.isBlank()
                ? HOLDING_NOTE
                : baseNote;
        return countryResult(profile.getSymbol(), totals, labels, note);
    }

    /**
     * Accepts fund-level country weights already published by a provider.
     * Values are fractions of the fund. An empty map is a missing report,
     * not a zero-exposure fund.
     */
    public ExposureResult calculateReportedCountryExposure(
            String symbol,
            Map<String, BigDecimal> weights) {

        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("ETF symbol cannot be blank.");
        }

        String normalized = symbol.trim().toUpperCase(Locale.ROOT);

        if (weights == null || weights.isEmpty()) {
            throw new IllegalArgumentException(
                    "No country exposure was returned for "
                            + normalized
                            + ". A geographic allocation was not estimated."
            );
        }

        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        Map<String, String> labels = new LinkedHashMap<>();

        for (Map.Entry<String, BigDecimal> entry : weights.entrySet()) {
            addCountry(totals, labels, entry.getKey(), entry.getValue());
        }

        if (!hasNamedCountry(totals)) {
            throw new IllegalArgumentException(
                    "No country exposure was returned for "
                            + normalized
                            + ". A geographic allocation was not estimated."
            );
        }

        return countryResult(normalized, totals, labels, REPORTED_NOTE);
    }

    public OverlapResult calculateOverlap(
            ETFProfile first,
            ETFProfile second) {

        if (first == null
                || second == null) {

            throw new IllegalArgumentException(
                    "Both ETF profiles are required."
            );
        }

        Map<String, ETFHolding>
                secondHoldings =
                new HashMap<>();

        for (ETFHolding holding
                : second.getHoldings()) {

            String symbol =
                    holding.getSymbol();

            if (symbol == null
                    || symbol.isBlank()) {

                continue;
            }

            secondHoldings.put(
                    symbol.toUpperCase(),
                    holding
            );
        }

        BigDecimal totalOverlap =
                BigDecimal.ZERO;

        List<ETFHolding>
                commonHoldings =
                new ArrayList<>();

        for (ETFHolding firstHolding
                : first.getHoldings()) {

            String symbol =
                    firstHolding.getSymbol();

            if (symbol == null
                    || symbol.isBlank()) {

                continue;
            }

            ETFHolding secondHolding =
                    secondHoldings.get(
                            symbol.toUpperCase()
                    );

            if (secondHolding == null) {
                continue;
            }

            BigDecimal sharedWeight =
                    firstHolding
                            .getWeight()
                            .min(
                                    secondHolding
                                            .getWeight()
                            );

            totalOverlap =
                    totalOverlap.add(
                            sharedWeight
                    );

            commonHoldings.add(
                    new ETFHolding(
                            firstHolding
                                    .getSymbol(),

                            firstHolding
                                    .getDescription(),

                            sharedWeight
                    )
            );
        }

        commonHoldings.sort(
                Comparator.comparing(
                        ETFHolding::getWeight
                ).reversed()
        );

        return new OverlapResult(
                first.getSymbol(),
                second.getSymbol(),
                totalOverlap,
                commonHoldings
        );
    }

    private void addCountry(
            Map<String, BigDecimal> totals,
            Map<String, String> labels,
            String country,
            BigDecimal weight) {

        if (weight == null || weight.signum() <= 0) {
            return;
        }

        String label = namedCountry(country);
        String key = label.toLowerCase(Locale.ROOT);
        labels.putIfAbsent(key, label);
        totals.merge(key, weight, BigDecimal::add);
    }

    private static String namedCountry(String country) {
        if (country == null || country.isBlank()) {
            return UNKNOWN_COUNTRY;
        }

        String trimmed = country.trim().replaceAll("\\s+", " ");
        String lower = trimmed.toLowerCase(Locale.ROOT);

        if (lower.equals("none")
                || lower.equals("null")
                || lower.equals("n/a")
                || lower.equals("na")
                || lower.equals("-")
                || lower.equals("unknown")) {

            return UNKNOWN_COUNTRY;
        }

        return trimmed;
    }

    private static boolean hasNamedCountry(Map<String, BigDecimal> totals) {
        return totals.keySet().stream()
                .anyMatch(key -> !UNKNOWN_COUNTRY.equalsIgnoreCase(key));
    }

    private ExposureResult countryResult(
            String symbol,
            Map<String, BigDecimal> totals,
            Map<String, String> labels,
            String baseNote) {

        Map<String, BigDecimal> named = new LinkedHashMap<>();
        BigDecimal sum = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : totals.entrySet()) {
            named.put(labels.get(entry.getKey()), entry.getValue());
            sum = sum.add(entry.getValue());
        }

        return new ExposureResult(
                symbol,
                sortByWeight(named),
                coverageNote(baseNote, sum)
        );
    }

    private static String coverageNote(String base, BigDecimal sum) {
        if (sum.compareTo(NEAR_ONE_LOW) < 0) {
            return base
                    + " Reported weights sum to "
                    + percent(sum)
                    + ". The remainder was not assigned a country and was not estimated.";
        }

        if (sum.compareTo(NEAR_ONE_HIGH) > 0) {
            return base
                    + " Reported weights sum above 100% and were not rescaled.";
        }

        return base;
    }

    private static String percent(BigDecimal fraction) {
        return fraction.multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString()
                + "%";
    }

    private static Map<String, BigDecimal> sortByWeight(
            Map<String, BigDecimal> weights) {

        Map<String, BigDecimal> sorted = new LinkedHashMap<>();

        weights.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));

        return sorted;
    }
}