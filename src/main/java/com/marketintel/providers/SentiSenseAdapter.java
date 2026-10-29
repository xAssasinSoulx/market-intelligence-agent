package com.marketintel.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketintel.model.SentimentReading;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

/**
 * Adapter for the SentiSense sentiment metric.
 *
 * The free plan is about 1,000 requests a month and 30 a minute. Confirm
 * both when you sign up. The call reads the sentiment series and keeps
 * the latest point. The display label is assigned here from the same
 * bands used for Marketaux scores.
 */
public final class SentiSenseAdapter implements DataProvider {

    static final String SOURCE = "SentiSense";
    private static final String BASE_URL = "https://app.sentisense.ai/api/v2/metrics";
    private static final int MAX_POINTS = 8;

    private final String apiKey;
    private final JsonTransport transport;

    public SentiSenseAdapter(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("SentiSense API key cannot be blank.");
        }
        this.apiKey = apiKey.trim();
        this.transport = new JsonTransport();
    }

    @Override
    public String providerName() {
        return SOURCE;
    }

    @Override
    public boolean supports(DataCapability capability) {
        return capability == DataCapability.SENTIMENT;
    }

    @Override
    public SentimentReading getSentiment(String symbol) {
        String normalized = Symbols.require(symbol);
        JsonTransport.JsonResponse response = transport.get(
                URI.create(
                        BASE_URL + "/entity/" + encode(normalized)
                                + "/metric/sentiment?maxDataPoints=" + MAX_POINTS
                ),
                Map.of("X-SentiSense-API-Key", apiKey),
                SOURCE
        );
        return parseSentiment(normalized, ok(response));
    }

    SentimentReading parseSentiment(String symbol, JsonNode root) {
        if (root == null || !root.isArray()) {
            throw new MarketDataException(
                    "SentiSense returned an unreadable sentiment series for " + symbol + "."
            );
        }
        if (root.isEmpty()) {
            throw new MarketDataException(
                    "SentiSense returned no sentiment score for " + symbol + "."
            );
        }
        JsonNode latest = root.get(root.size() - 1);
        BigDecimal score = reading(latest);
        if (score == null) {
            throw new MarketDataException(
                    "SentiSense returned no sentiment score for " + symbol + "."
            );
        }
        BigDecimal change = null;
        if (root.size() >= 2) {
            BigDecimal previous = reading(root.get(root.size() - 2));
            if (previous != null) {
                change = score.subtract(previous);
            }
        }
        String asOf = "";
        JsonNode timestamp = latest.path("timestamp");
        if (timestamp.isNumber() && timestamp.asLong() > 0) {
            asOf = Instant.ofEpochMilli(timestamp.asLong()).toString();
        }
        String note = "SentiSense sentiment is an average tone from -1 to 1 over the returned window. "
                + "The label is this application's display band for that score. "
                + "It is not a price forecast.";
        if (change != null) {
            note = note + " Change is the latest point minus the previous point.";
        }
        return new SentimentReading(
                symbol,
                SOURCE,
                score,
                SentimentBands.label(score),
                change,
                asOf,
                note
        );
    }

    private JsonNode ok(JsonTransport.JsonResponse response) {
        String detail = ProviderText.redact(JsonValues.errorMessage(response.json()), apiKey);
        if (response.status() == 401 || response.status() == 403) {
            throw new MarketDataException("SentiSense rejected the API key (HTTP "
                    + response.status() + ").");
        }
        if (response.status() == 404) {
            throw new MarketDataException(
                    "SentiSense has no sentiment entity for this symbol."
                            + (detail.isBlank() ? "" : " " + detail)
            );
        }
        if (response.status() == 429) {
            String code = JsonValues.text(response.json(), "code");
            if (code.isBlank()) {
                code = JsonValues.text(response.json(), "error");
            }
            if ("quota_exceeded".equalsIgnoreCase(code)) {
                throw new MarketDataException(
                        "SentiSense monthly quota is exhausted. "
                                + "The free plan allows about 1,000 requests a month."
                );
            }
            throw new MarketDataException(
                    "SentiSense rate limit reached. The free plan allows about 30 calls a minute."
            );
        }
        if (response.status() < 200 || response.status() >= 300) {
            throw new MarketDataException(
                    "SentiSense returned HTTP " + response.status() + "."
                            + (detail.isBlank() ? "" : " " + detail)
            );
        }
        if (response.json() == null) {
            throw new MarketDataException("SentiSense returned an unreadable response.");
        }
        return response.json();
    }

    static BigDecimal reading(JsonNode point) {
        if (point == null || point.isNull()) {
            return null;
        }
        BigDecimal direct = JsonValues.decimal(point, "value");
        if (direct != null) {
            return direct;
        }
        JsonNode metricValue = point.path("metricValue");
        direct = JsonValues.decimal(metricValue, "value");
        if (direct != null) {
            return direct;
        }
        return JsonValues.decimal(metricValue.path("value"), "value");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
