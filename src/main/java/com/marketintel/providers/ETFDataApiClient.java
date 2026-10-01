package com.marketintel.providers;

import com.fasterxml.jackson.databind.JsonNode;

import com.marketintel.model.ETFHolding;
import com.marketintel.model.ETFProfile;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ETFDataApiClient
        implements ETFDataProvider {

    private final AlphaVantageClient client;

    public ETFDataApiClient(
            AlphaVantageClient client) {

        if (client == null) {

            throw new IllegalArgumentException(
                    "Alpha Vantage client "
                            + "cannot be null."
            );
        }

        this.client = client;
    }

    @Override
    public ETFProfile getProfile(
            String symbol) {

        String normalizedSymbol =
                normalizeSymbol(symbol);

        JsonNode root =
                client.query(
                        "ETF_PROFILE",
                        normalizedSymbol
                );

        return parseProfile(
                normalizedSymbol,
                root
        );
    }

    ETFProfile parseProfile(
            String symbol,
            JsonNode root) {

        if (root == null) {

            throw new IllegalArgumentException(
                    "ETF response cannot be null."
            );
        }

        List<ETFHolding> holdings =
                parseHoldings(root);

        Map<String, BigDecimal>
                sectors =
                parseSectors(root);

        holdings =
                normalizeHoldingWeights(
                        holdings
                );

        sectors =
                normalizeSectorWeights(
                        sectors
                );

        return new ETFProfile(
                symbol,

                optionalDecimal(
                        root.path(
                                "net_assets"
                        )
                ),

                optionalDecimal(
                        root.path(
                                "net_expense_ratio"
                        )
                ),

                holdings,
                sectors
        );
    }

    private List<ETFHolding>
    parseHoldings(
            JsonNode root) {

        List<ETFHolding> holdings =
                new ArrayList<>();

        JsonNode array =
                root.path("holdings");

        if (!array.isArray()) {
            return holdings;
        }

        for (JsonNode node : array) {

            String symbol =
                    node.path("symbol")
                            .asText("");

            String description =
                    node.path(
                            "description"
                    ).asText("");

            BigDecimal weight =
                    optionalDecimal(
                            node.path(
                                    "weight"
                            )
                    );

            if (symbol.isBlank()
                    || weight == null) {

                continue;
            }

            holdings.add(
                    new ETFHolding(
                            symbol
                                    .trim()
                                    .toUpperCase(),

                            description,

                            weight
                    )
            );
        }

        return holdings;
    }

    private Map<String, BigDecimal>
    parseSectors(
            JsonNode root) {

        Map<String, BigDecimal> sectors =
                new LinkedHashMap<>();

        JsonNode array =
                root.path("sectors");

        if (!array.isArray()) {
            return sectors;
        }

        for (JsonNode node : array) {

            String sector =
                    node.path("sector")
                            .asText("");

            BigDecimal weight =
                    optionalDecimal(
                            node.path(
                                    "weight"
                            )
                    );

            if (sector.isBlank()
                    || weight == null) {

                continue;
            }

            sectors.put(
                    sector.trim(),
                    weight
            );
        }

        return sectors;
    }

    private List<ETFHolding>
    normalizeHoldingWeights(
            List<ETFHolding> holdings) {

        BigDecimal total =
                holdings.stream()
                        .map(
                                ETFHolding::getWeight
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        if (total.compareTo(
                new BigDecimal("2")
        ) <= 0) {

            return holdings;
        }

        List<ETFHolding> normalized =
                new ArrayList<>();

        for (ETFHolding holding
                : holdings) {

            normalized.add(
                    new ETFHolding(
                            holding.getSymbol(),

                            holding.getDescription(),

                            holding.getWeight()
                                    .movePointLeft(2)
                    )
            );
        }

        return normalized;
    }

    private Map<String, BigDecimal>
    normalizeSectorWeights(
            Map<String, BigDecimal> sectors) {

        BigDecimal total =
                sectors.values()
                        .stream()
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        if (total.compareTo(
                new BigDecimal("2")
        ) <= 0) {

            return sectors;
        }

        Map<String, BigDecimal>
                normalized =
                new LinkedHashMap<>();

        for (Map.Entry<String, BigDecimal>
                entry : sectors.entrySet()) {

            normalized.put(
                    entry.getKey(),

                    entry.getValue()
                            .movePointLeft(2)
            );
        }

        return normalized;
    }

    private BigDecimal optionalDecimal(
            JsonNode node) {

        if (node == null
                || node.isMissingNode()
                || node.isNull()) {

            return null;
        }

        String text =
                node.asText();

        if (text == null
                || text.isBlank()
                || text.equalsIgnoreCase(
                "None"
        )) {

            return null;
        }

        try {

            return new BigDecimal(text);

        } catch (NumberFormatException e) {

            return null;
        }
    }

    private String normalizeSymbol(
            String symbol) {

        if (symbol == null
                || symbol.isBlank()) {

            throw new IllegalArgumentException(
                    "ETF symbol cannot be blank."
            );
        }

        return symbol
                .trim()
                .toUpperCase();
    }
}