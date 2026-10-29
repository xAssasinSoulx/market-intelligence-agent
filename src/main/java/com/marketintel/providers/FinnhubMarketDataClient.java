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
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Finnhub adapter for quotes and security overviews.
 *
 * The free plan serves /quote and /stock/profile2. The profile has a
 * currency, an exchange, a country, and a Finnhub industry label. It has
 * no description and no separate sector, so the industry label is used
 * as the sector and the overview says so.
 */
public final class FinnhubMarketDataClient
        implements MarketDataProvider, CountryExposureSource, HeadquartersLookup {

    private static final String BASE_URL = "https://finnhub.io/api/v1";

    /**
     * Country weights live on the API host at {@code /etf/country}.
     * {@code finnhub.io/etf/country-exposure} answers a valid key with HTTP 302.
     */
    static final String COUNTRY_EXPOSURE_BASE = "https://api.finnhub.io/api/v1";
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

    /**
     * The free plan returns quotes for US listings only. A request for a
     * non-US symbol such as XEQT.TO is refused, so any quote that does
     * come back is priced in US dollars.
     */
    @Override
    public Optional<String> quoteCurrency() {
        return Optional.of("USD");
    }

    /**
     * Finnhub's fund-level country exposure. The published numbers are
     * percentages, so a total above 2 is converted to a fraction of the fund.
     * An empty list is a missing report, not a zero-exposure fund.
     */
    @Override
    public Map<String, BigDecimal> getCountryExposure(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        JsonNode root = fetch(countryExposureUri(normalizedSymbol));
        return parseCountryExposure(normalizedSymbol, root);
    }

    static URI countryExposureUri(String symbol) {
        return URI.create(
                COUNTRY_EXPOSURE_BASE
                        + "/etf/country?symbol="
                        + URLEncoder.encode(symbol, StandardCharsets.UTF_8)
        );
    }

    /**
     * Headquarters country from the free company profile. A missing profile
     * or a refused symbol returns null so one holding cannot fail the fund.
     */
    @Override
    public String headquartersCountry(String symbol) {
        try {
            JsonNode root = fetch("/stock/profile2", normalizeSymbol(symbol));
            return parseHeadquartersCountry(root);
        } catch (MarketDataException | IllegalArgumentException exception) {
            return null;
        }
    }

    static String parseHeadquartersCountry(JsonNode root) {
        if (root == null || !root.isObject() || root.isEmpty()) {
            return null;
        }
        String country = text(root, "country");
        return country.isBlank() ? null : country;
    }

    Map<String, BigDecimal> parseCountryExposure(String symbol, JsonNode root) {
        if (root == null || !root.isObject()) {
            throw new MarketDataException(
                    "Finnhub returned an unreadable country exposure for " + symbol + "."
            );
        }

        JsonNode array = root.path("countryExposure");
        if (!array.isArray() || array.isEmpty()) {
            throw new MarketDataException(
                    "Finnhub did not return country exposure for "
                            + symbol
                            + ". GEO reports an ETF's country weights, so a single stock has none. "
                            + "No allocation was estimated."
            );
        }

        Map<String, BigDecimal> weights = new LinkedHashMap<>();
        for (JsonNode node : array) {
            BigDecimal exposure = decimal(node, "exposure");
            if (exposure == null || exposure.signum() <= 0) {
                continue;
            }
            String country = text(node, "country");
            if (country.isBlank()) {
                country = "Unknown";
            }
            weights.merge(country, exposure, BigDecimal::add);
        }

        if (weights.isEmpty()) {
            throw new MarketDataException(
                    "Finnhub did not return country exposure for "
                            + symbol
                            + ". GEO reports an ETF's country weights, so a single stock has none. "
                            + "No allocation was estimated."
            );
        }

        BigDecimal total = weights.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(new BigDecimal("2")) <= 0) {
            return weights;
        }

        Map<String, BigDecimal> fractions = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : weights.entrySet()) {
            fractions.put(entry.getKey(), entry.getValue().movePointLeft(2));
        }
        return fractions;
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

        Instant lastTradeAt = null;
        JsonNode timestamp = root.path("t");
        if (timestamp.isNumber() && timestamp.asLong() > 0) {
            lastTradeAt = Instant.ofEpochSecond(timestamp.asLong());
        }

        return new MarketQuote(
                symbol,
                price,
                previousClose,
                percentChange.setScale(4, RoundingMode.HALF_UP),
                lastTradeAt
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
        return fetch(URI.create(
                BASE_URL + path
                        + "?symbol=" + URLEncoder.encode(symbol, StandardCharsets.UTF_8)
        ));
    }

    private JsonNode fetch(URI uri) {

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
                        rejectionMessage(uri, status, response.body())
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

    /**
     * 401 means the key itself was refused. 403 on the ETF country report
     * means this plan does not include that endpoint. Quotes can still work.
     */
    static String rejectionMessage(URI uri, int status, String body) {
        String detail = errorText(body);
        boolean countryReport = uri != null
                && uri.getPath() != null
                && uri.getPath().contains("/etf/country");

        if (status == 403 && countryReport) {
            String message = "Finnhub refused ETF country exposure (HTTP 403). "
                    + "This key can still request quotes. "
                    + "The free plan does not include /etf/country, "
                    + "so no allocation was estimated.";
            if (!detail.isBlank()) {
                message = message + " Finnhub said: " + detail;
            }
            return message;
        }

        if (status == 401) {
            return "Finnhub rejected the API key (HTTP 401).";
        }

        if (status == 403) {
            if (detail.isBlank()) {
                return "Finnhub refused this request (HTTP 403). "
                        + "The endpoint may not be included in the current plan.";
            }
            return "Finnhub refused this request (HTTP 403). " + detail;
        }

        return "Finnhub returned HTTP " + status + ".";
    }

    private static String errorText(String body) {
        if (body == null || body.isBlank() || !body.trim().startsWith("{")) {
            return "";
        }
        try {
            JsonNode root = new ObjectMapper().readTree(body);
            String error = root.path("error").asText("").trim();
            if (error.isBlank() || error.toLowerCase(Locale.ROOT).contains("api key")) {
                return "";
            }
            return ProviderText.redact(error);
        } catch (IOException e) {
            return "";
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
