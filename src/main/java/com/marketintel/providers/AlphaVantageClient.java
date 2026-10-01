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

public class AlphaVantageClient {

    private static final String BASE_URL =
            "https://www.alphavantage.co/query";

    private static final long
            MIN_REQUEST_INTERVAL_MILLIS = 2000;

    private final String apiKey;

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    private long lastRequestTimeMillis;

    public AlphaVantageClient(
            String apiKey) {

        if (apiKey == null
                || apiKey.isBlank()) {

            throw new IllegalArgumentException(
                    "Alpha Vantage API key is required."
            );
        }

        this.apiKey = apiKey;

        this.httpClient =
                HttpClient.newHttpClient();

        this.objectMapper =
                new ObjectMapper();
    }

    public synchronized JsonNode query(
            String function,
            String symbol) {

        if (function == null
                || function.isBlank()) {

            throw new IllegalArgumentException(
                    "API function cannot be blank."
            );
        }

        if (symbol == null
                || symbol.isBlank()) {

            throw new IllegalArgumentException(
                    "Symbol cannot be blank."
            );
        }

        respectRateLimit();

        URI uri =
                buildUri(
                        function,
                        symbol
                );

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(uri)
                        .GET()
                        .build();

        try {

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse
                                    .BodyHandlers
                                    .ofString()
                    );

            lastRequestTimeMillis =
                    System.currentTimeMillis();

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                throw new MarketDataException(
                        "Alpha Vantage request failed "
                                + "with HTTP status "
                                + response.statusCode()
                );
            }

            JsonNode root =
                    objectMapper.readTree(
                            response.body()
                    );

            checkForApiError(root);

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

    private void respectRateLimit() {

        if (lastRequestTimeMillis == 0) {
            return;
        }

        long elapsed =
                System.currentTimeMillis()
                        - lastRequestTimeMillis;

        long remaining =
                MIN_REQUEST_INTERVAL_MILLIS
                        - elapsed;

        if (remaining <= 0) {
            return;
        }

        try {

            Thread.sleep(remaining);

        } catch (InterruptedException e) {

            Thread.currentThread()
                    .interrupt();

            throw new MarketDataException(
                    "Interrupted while waiting "
                            + "for API rate limit.",
                    e
            );
        }
    }

    private URI buildUri(
            String function,
            String symbol) {

        String url =
                BASE_URL
                        + "?function="
                        + encode(function)
                        + "&symbol="
                        + encode(symbol)
                        + "&apikey="
                        + encode(apiKey);

        return URI.create(url);
    }

    private String encode(
            String value) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }

    private void checkForApiError(
            JsonNode root) {

        if (root.has("Error Message")) {

            throw new MarketDataException(
                    root.path(
                            "Error Message"
                    ).asText()
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
                    root.path(
                            "Information"
                    ).asText()
            );
        }
    }
}