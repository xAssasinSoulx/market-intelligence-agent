package com.marketintel.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import java.net.URI;
import java.net.URLEncoder;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.nio.charset.StandardCharsets;

import java.time.Duration;

import java.util.LinkedHashMap;
import java.util.Map;

public class AlphaVantageClient {

    private static final String BASE_URL =
            "https://www.alphavantage.co/query";

    private static final long
            MIN_REQUEST_INTERVAL_MILLIS =
            2000;

    private final String apiKey;

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    private long lastRequestTimeMillis = 0;

    public AlphaVantageClient(
            String apiKey) {

        if (apiKey == null
                || apiKey.isBlank()) {

            throw new IllegalArgumentException(
                    "Alpha Vantage API key "
                            + "cannot be blank."
            );
        }

        this.apiKey =
                apiKey.trim();

        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(
                                Duration.ofSeconds(10)
                        )
                        .build();

        this.objectMapper =
                new ObjectMapper();
    }

    public JsonNode query(
            String function,
            String symbol) {

        if (symbol == null
                || symbol.isBlank()) {

            throw new IllegalArgumentException(
                    "Symbol cannot be blank."
            );
        }

        return query(
                function,
                Map.of(
                        "symbol",
                        symbol.trim()
                )
        );
    }

    public synchronized JsonNode query(
            String function,
            Map<String, String> parameters) {

        if (function == null
                || function.isBlank()) {

            throw new IllegalArgumentException(
                    "Alpha Vantage function "
                            + "cannot be blank."
            );
        }

        Map<String, String> queryParameters =
                new LinkedHashMap<>();

        queryParameters.put(
                "function",
                function.trim()
        );

        if (parameters != null) {

            parameters.forEach(
                    (key, value) -> {

                        if (key != null
                                && !key.isBlank()
                                && value != null
                                && !value.isBlank()) {

                            queryParameters.put(
                                    key.trim(),
                                    value.trim()
                            );
                        }
                    }
            );
        }

        queryParameters.put(
                "apikey",
                apiKey
        );

        try {

            respectRateLimit();

            URI uri =
                    buildUri(
                            queryParameters
                    );

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(uri)
                            .timeout(
                                    Duration.ofSeconds(20)
                            )
                            .GET()
                            .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse
                                    .BodyHandlers
                                    .ofString()
                    );

            lastRequestTimeMillis =
                    System.currentTimeMillis();

            if (response.statusCode()
                    < 200
                    || response.statusCode()
                    >= 300) {

                throw new MarketDataException(
                        "Alpha Vantage request failed "
                                + "with HTTP status "
                                + response.statusCode()
                                + "."
                );
            }

            JsonNode root =
                    objectMapper.readTree(
                            response.body()
                    );

            checkForApiError(
                    root
            );

            return root;

        } catch (InterruptedException e) {

            Thread.currentThread()
                    .interrupt();

            throw new MarketDataException(
                    "Alpha Vantage request "
                            + "was interrupted.",
                    e
            );

        } catch (IOException e) {

            throw new MarketDataException(
                    "Unable to communicate "
                            + "with Alpha Vantage.",
                    e
            );
        }
    }

    private URI buildUri(
            Map<String, String> parameters) {

        StringBuilder builder =
                new StringBuilder(
                        BASE_URL
                );

        builder.append("?");

        boolean first = true;

        for (Map.Entry<String, String> entry
                : parameters.entrySet()) {

            if (!first) {
                builder.append("&");
            }

            builder.append(
                    encode(
                            entry.getKey()
                    )
            );

            builder.append("=");

            builder.append(
                    encode(
                            entry.getValue()
                    )
            );

            first = false;
        }

        return URI.create(
                builder.toString()
        );
    }

    private String encode(
            String value) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }

    private void respectRateLimit()
            throws InterruptedException {

        long now =
                System.currentTimeMillis();

        long elapsed =
                now
                        - lastRequestTimeMillis;

        long remaining =
                MIN_REQUEST_INTERVAL_MILLIS
                        - elapsed;

        if (lastRequestTimeMillis > 0
                && remaining > 0) {

            Thread.sleep(
                    remaining
            );
        }
    }

    private void checkForApiError(
            JsonNode root) {

        if (root == null
                || root.isNull()) {

            throw new MarketDataException(
                    "Alpha Vantage returned "
                            + "an empty response."
            );
        }

        if (root.has(
                "Error Message")) {

            throw new MarketDataException(
                    root.path(
                            "Error Message"
                    ).asText(
                            "Alpha Vantage returned an error."
                    )
            );
        }

        if (root.has(
                "Note")) {

            throw new MarketDataException(
                    root.path(
                            "Note"
                    ).asText(
                            "Alpha Vantage rate limit reached."
                    )
            );
        }

        if (root.has(
                "Information")) {

            throw new MarketDataException(
                    root.path(
                            "Information"
                    ).asText(
                            "Alpha Vantage request "
                                    + "could not be completed."
                    )
            );
        }
    }
}