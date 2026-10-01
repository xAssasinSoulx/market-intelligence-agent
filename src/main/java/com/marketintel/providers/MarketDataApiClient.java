package com.marketintel.providers;

import com.fasterxml.jackson.databind.JsonNode;

import com.marketintel.model.MarketQuote;
import com.marketintel.model.SecurityOverview;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MarketDataApiClient
        implements MarketDataProvider {

    private final AlphaVantageClient client;

    public MarketDataApiClient(
            String apiKey) {

        this(
                new AlphaVantageClient(
                        apiKey
                )
        );
    }

    public MarketDataApiClient(
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
    public MarketQuote getQuote(
            String symbol) {

        String normalizedSymbol =
                normalizeSymbol(symbol);

        JsonNode root =
                client.query(
                        "GLOBAL_QUOTE",
                        normalizedSymbol
                );

        JsonNode quote =
                root.path(
                        "Global Quote"
                );

        if (quote.isMissingNode()
                || quote.isEmpty()) {

            throw new MarketDataException(
                    "No quote data found "
                            + "for symbol: "
                            + normalizedSymbol
            );
        }

        try {

            String returnedSymbol =
                    requiredText(
                            quote,
                            "01. symbol"
                    );

            BigDecimal price =
                    new BigDecimal(
                            requiredText(
                                    quote,
                                    "05. price"
                            )
                    );

            BigDecimal previousClose =
                    new BigDecimal(
                            requiredText(
                                    quote,
                                    "08. previous close"
                            )
                    );

            BigDecimal percentChange =
                    calculatePercentChange(
                            price,
                            previousClose
                    );

            return new MarketQuote(
                    returnedSymbol,
                    price,
                    previousClose,
                    percentChange
            );

        } catch (NumberFormatException e) {

            throw new MarketDataException(
                    "Market-data provider returned "
                            + "an invalid numeric value.",
                    e
            );
        }
    }

    @Override
    public SecurityOverview
    getSecurityOverview(
            String symbol) {

        String normalizedSymbol =
                normalizeSymbol(symbol);

        JsonNode root =
                client.query(
                        "OVERVIEW",
                        normalizedSymbol
                );

        if (root.isEmpty()
                || root.path("Symbol")
                .asText()
                .isBlank()) {

            throw new MarketDataException(
                    "No security overview found "
                            + "for symbol: "
                            + normalizedSymbol
            );
        }

        return new SecurityOverview(
                root.path("Symbol")
                        .asText(),

                root.path("Name")
                        .asText(),

                root.path("Description")
                        .asText(),

                root.path("Exchange")
                        .asText(),

                root.path("Currency")
                        .asText(),

                root.path("Sector")
                        .asText(),

                root.path("Industry")
                        .asText(),

                optionalBigDecimal(
                        root.path(
                                "MarketCapitalization"
                        ).asText()
                )
        );
    }

    private String requiredText(
            JsonNode node,
            String field) {

        String value =
                node.path(field)
                        .asText();

        if (value == null
                || value.isBlank()) {

            throw new MarketDataException(
                    "Market-data response "
                            + "is missing field: "
                            + field
            );
        }

        return value;
    }

    private BigDecimal optionalBigDecimal(
            String value) {

        if (value == null
                || value.isBlank()
                || value.equalsIgnoreCase(
                "None"
        )) {

            return null;
        }

        try {

            return new BigDecimal(value);

        } catch (NumberFormatException e) {

            return null;
        }
    }

    private BigDecimal calculatePercentChange(
            BigDecimal price,
            BigDecimal previousClose) {

        if (previousClose.compareTo(
                BigDecimal.ZERO
        ) == 0) {

            return BigDecimal.ZERO;
        }

        return price
                .subtract(previousClose)
                .divide(
                        previousClose,
                        8,
                        RoundingMode.HALF_UP
                )
                .multiply(
                        new BigDecimal("100")
                )
                .setScale(
                        4,
                        RoundingMode.HALF_UP
                );
    }

    private String normalizeSymbol(
            String symbol) {

        if (symbol == null
                || symbol.isBlank()) {

            throw new IllegalArgumentException(
                    "Symbol cannot be blank."
            );
        }

        return symbol
                .trim()
                .toUpperCase();
    }
}