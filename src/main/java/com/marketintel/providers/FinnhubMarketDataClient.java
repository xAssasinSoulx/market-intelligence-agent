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
import java.time.Duration;
import java.util.Locale;

/**
 * Finnhub adapter for quotes and security overviews.
 *
 * The free plan serves /quote and /stock/profile2. The profile has a
 * currency, an exchange, a country, and a Finnhub industry label. It has
 * no description and no separate sector, so the industry label is used
 * as the sector and the overview says so.
 */
public final class FinnhubMarketDataClient implements MarketDataProvider {

    private static final String BASE_URL = "https://finnhub.io/api/v1";
    private static final BigDecimal MILLION = new BigDecimal("1000000");

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public FinnhubMarketDataClient(String apiKey) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Finnhub API key cannot be blank."
            );
        }

        this.apiKey = apiKey.trim();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public MarketQuote getQuote(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        JsonNode root = fetch("/quote", normalizedSymbol);
        return parseQuote(normalizedSymbol, root);
    }

    @Override
    public SecurityOverview getSecurityOverview(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        JsonNode root = fetch("/stock/profile2", normalizedSymbol);
        return parseOverview(normalizedSymbol, root);
    }

    /*
     * Package-private for offline tests. Finnhub returns zeros, not an
     * error, for an unknown symbol.
     */
    MarketQuote parseQuote(String symbol, JsonNode root) {

        if (root == null || !root.isObject()) {
            throw new MarketDataException(
                    "Finnhub returned an unreadable quote for " + symbol + "."
            );
        }

        BigDecimal price = decimal(root, "c");
        BigDecimal previousClose = decimal(root, "pc");

        if (price == null || previousClose == null
                || (price.signum() == 0 && previousClose.signum() == 0)) {
            throw new MarketDataException(
                    "No quote data found for symbol: " + symbol
            );
        }

        BigDecimal percentChange = decimal(root, "dp");
        if (percentChange == null) {
            percentChange = calculatePercentChange(price, previousClose);
        }

        return new MarketQuote(
                symbol,
                price,
                previousClose,
                percentChange.setScale(4, RoundingMode.HALF_UP)
        );
    }

    /*
     * Package-private for offline tests. An unknown symbol produces an
     * empty JSON object.
     */
    SecurityOverview parseOverview(String symbol, JsonNode root) {

        if (root == null || !root.isObject() || root.isEmpty()
                || text(root, "name").isBlank()) {
            throw new MarketDataException(
                    "No security overview found for symbol: " + symbol
            );
        }

        String industry = text(root, "finnhubIndustry");
        String ticker = text(root, "ticker");
        String exchange = text(root, "exchange");
        String country = text(root, "country");

        String description = "Finnhub profile."
                + (country.isBlank() ? "" : " Headquarters country: " + country + ".")
                + (industry.isBlank()
                ? " No industry classification was returned."
                : " Sector shown is the Finnhub industry classification, not a GICS sector.");

        BigDecimal marketCapitalization = decimal(root, "marketCapitalization");
        if (marketCapitalization != null) {
            marketCapitalization = marketCapitalization
                    .multiply(MILLION)
                    .setScale(0, RoundingMode.HALF_UP);
        }

        return new SecurityOverview(
                ticker.isBlank() ? symbol : ticker,
                text(root, "name"),
                description,
                exchange,
                text(root, "currency"),
                industry,
                industry,
                marketCapitalization
        );
    }

    private JsonNode fetch(String path, String symbol) {

        URI uri = URI.create(
                BASE_URL + path
                        + "?symbol=" + URLEncoder.encode(symbol, StandardCharsets.UTF_8)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("X-Finnhub-Token", apiKey)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            int status = response.statusCode();
            if (status == 401 || status == 403) {
                throw new MarketDataException(
                        "Finnhub rejected the API key (HTTP " + status + ")."
                );
            }
            if (status == 429) {
                throw new MarketDataException(
                        "Finnhub rate limit reached. Wait a minute and retry."
                );
            }
            if (status != 200) {
                throw new MarketDataException(
                        "Finnhub returned HTTP " + status + "."
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (root.has("error")) {
                throw new MarketDataException(
                        "Finnhub returned an error: " + root.path("error").asText()
                );
            }
            return root;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MarketDataException("Finnhub request was interrupted.", e);
        } catch (IOException e) {
            throw new MarketDataException(
                    "Unable to reach Finnhub: " + safeMessage(e),
                    e
            );
        }
    }

    private static BigDecimal decimal(JsonNode root, String field) {
        JsonNode node = root.path(field);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        try {
            return new BigDecimal(node.asText().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String text(JsonNode root, String field) {
        JsonNode node = root.path(field);
        if (node.isMissingNode() || node.isNull()) {
            return "";
        }
        return node.asText("").trim();
    }

    private static BigDecimal calculatePercentChange(
            BigDecimal price,
            BigDecimal previousClose) {

        if (previousClose.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return price.subtract(previousClose)
                .divide(previousClose, 8, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private static String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Symbol cannot be blank.");
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    private static String safeMessage(IOException e) {
        String message = e.getMessage();
        return message == null || message.isBlank()
                ? e.getClass().getSimpleName()
                : message;
    }
}
