package com.marketintel.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.marketintel.model.MarketQuote;
import com.marketintel.model.SecurityOverview;

import java.io.IOException;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.charset.StandardCharsets;

public class MarketDataApiClient
        implements MarketDataProvider {

    private static final long MIN_REQUEST_INTERVAL_MILLIS = 2000;

    private long lastRequestTimeMillis = 0;

    private static final String BASE_URL =
            "https://www.alphavantage.co/query";

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MarketDataApiClient(String apiKey) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Alpha Vantage API key is required."
            );
        }

        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public MarketQuote getQuote(String symbol) {

        String normalizedSymbol =
                normalizeSymbol(symbol);

        URI uri = buildUri(
                "GLOBAL_QUOTE",
                normalizedSymbol
        );

        JsonNode root = sendRequest(uri);

        checkForApiError(root);

        JsonNode quote =
                root.path("Global Quote");

        if (quote.isMissingNode()
                || quote.isEmpty()) {

            throw new MarketDataException(
                    "No quote data found for symbol: "
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
    public SecurityOverview getSecurityOverview(
            String symbol) {

        String normalizedSymbol =
                normalizeSymbol(symbol);

        URI uri = buildUri(
                "OVERVIEW",
                normalizedSymbol
        );

        JsonNode root = sendRequest(uri);

        checkForApiError(root);

        if (root.isEmpty()
                || root.path("Symbol")
                .asText()
                .isBlank()) {

            throw new MarketDataException(
                    "No security overview found for symbol: "
                            + normalizedSymbol
            );
        }

        return new SecurityOverview(
                root.path("Symbol").asText(),
                root.path("Name").asText(),
                root.path("Description").asText(),
                root.path("Exchange").asText(),
                root.path("Currency").asText(),
                root.path("Sector").asText(),
                root.path("Industry").asText(),
                optionalBigDecimal(
                        root.path(
                                "MarketCapitalization"
                        ).asText()
                )
        );
    }

    private JsonNode sendRequest(URI uri) {

        respectRateLimit();

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(uri)
                        .GET()
                        .build();

        try {

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                throw new MarketDataException(
                        "Market-data request failed "
                                + "with HTTP status "
                                + response.statusCode()
                );
            }

            return objectMapper.readTree(
                    response.body()
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new MarketDataException(
                    "Market-data request was interrupted.",
                    e
            );

        } catch (IOException e) {

            throw new MarketDataException(
                    "Unable to communicate with "
                            + "market-data provider.",
                    e
            );
        }
    }

    private URI buildUri(
            String function,
            String symbol) {

        String encodedSymbol =
                URLEncoder.encode(
                        symbol,
                        StandardCharsets.UTF_8
                );

        return URI.create(
                BASE_URL
                        + "?function="
                        + function
                        + "&symbol="
                        + encodedSymbol
                        + "&apikey="
                        + URLEncoder.encode(
                        apiKey,
                        StandardCharsets.UTF_8
                )
        );
    }

    private void checkForApiError(
            JsonNode root) {

        if (root.has("Error Message")) {

            throw new MarketDataException(
                    root.path("Error Message")
                            .asText()
            );
        }

        if (root.has("Note")) {

            throw new MarketDataException(
                    root.path("Note")
                            .asText()
            );
        }

        if (root.has("Information")) {

            throw new MarketDataException(
                    root.path("Information")
                            .asText()
            );
        }
    }

    private String requiredText(
            JsonNode node,
            String field) {

        String value =
                node.path(field).asText();

        if (value == null
                || value.isBlank()) {

            throw new MarketDataException(
                    "Market-data response is missing field: "
                            + field
            );
        }

        return value;
    }

    private BigDecimal optionalBigDecimal(
            String value) {

        if (value == null
                || value.isBlank()
                || value.equalsIgnoreCase("None")) {

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

    private synchronized void respectRateLimit() {

        long now = System.currentTimeMillis();

        long elapsed =
                now - lastRequestTimeMillis;

        long remainingWait =
                MIN_REQUEST_INTERVAL_MILLIS - elapsed;

        if (remainingWait > 0) {

            try {

                Thread.sleep(remainingWait);

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                throw new MarketDataException(
                        "Interrupted while waiting for market-data rate limit.",
                        e
                );
            }
        }

        lastRequestTimeMillis =
                System.currentTimeMillis();
    }
}