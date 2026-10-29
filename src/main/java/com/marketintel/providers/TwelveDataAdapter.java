package com.marketintel.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketintel.model.MarketQuote;
import com.marketintel.model.PriceBar;
import com.marketintel.model.PriceHistory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adapter for Twelve Data price history, RSI, and quotes.
 *
 * The free plan is about 800 requests a day and 8 a minute. Confirm both
 * when you sign up. History asks for 30 daily bars and one 14-day RSI,
 * which is two requests. A quote is a separate request and is only used
 * when an earlier quote adapter was not registered.
 */
public final class TwelveDataAdapter implements DataProvider {

    static final String SOURCE = "Twelve Data";
    private static final String BASE_URL = "https://api.twelvedata.com";
    private static final int DAILY_BARS = 30;
    private static final int RSI_PERIOD = 14;

    private final String apiKey;
    private final JsonTransport transport;

    public TwelveDataAdapter(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Twelve Data API key cannot be blank.");
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
        return capability == DataCapability.QUOTE
                || capability == DataCapability.PRICE_HISTORY;
    }

    /**
     * The free plan used here serves US listings. A quote that comes back
     * is treated as US dollars.
     */
    @Override
    public Optional<String> quoteCurrency() {
        return Optional.of("USD");
    }

    @Override
    public MarketQuote getQuote(String symbol) {
        String normalized = Symbols.require(symbol);
        JsonNode root = ok(get("/quote", "symbol=" + encode(normalized)));
        return parseQuote(normalized, root);
    }

    @Override
    public PriceHistory getPriceHistory(String symbol) {
        String normalized = Symbols.require(symbol);
        JsonNode series = ok(get(
                "/time_series",
                "symbol=" + encode(normalized)
                        + "&interval=1day&outputsize=" + DAILY_BARS
        ));
        List<PriceBar> bars = parseBars(normalized, series);
        String currency = JsonValues.text(series.path("meta"), "currency");
        BigDecimal rsi = null;
        String note = "Daily bars from Twelve Data. "
                + "RSI is the latest 14-day relative strength index.";
        try {
            JsonNode rsiRoot = ok(get(
                    "/rsi",
                    "symbol=" + encode(normalized)
                            + "&interval=1day&time_period=" + RSI_PERIOD
                            + "&outputsize=1"
            ));
            rsi = parseLatestRsi(rsiRoot);
        } catch (MarketDataException exception) {
            note = note + " RSI was not returned: " + exception.getMessage();
        }
        return new PriceHistory(
                normalized,
                SOURCE,
                "1day",
                currency,
                bars,
                rsi,
                note
        );
    }

    MarketQuote parseQuote(String symbol, JsonNode root) {
        if (root == null || !root.isObject()) {
            throw new MarketDataException(
                    "Twelve Data returned an unreadable quote for " + symbol + "."
            );
        }
        rejectError(root);
        BigDecimal price = firstDecimal(
                JsonValues.decimal(root, "close"),
                JsonValues.decimal(root, "price")
        );
        BigDecimal previousClose = JsonValues.decimal(root, "previous_close");
        if (price == null || previousClose == null) {
            throw new MarketDataException("No quote data found for symbol: " + symbol);
        }
        BigDecimal percentChange = JsonValues.decimal(root, "percent_change");
        if (percentChange == null) {
            percentChange = percentChange(price, previousClose);
        }
        Instant lastTradeAt = null;
        JsonNode timestamp = root.path("timestamp");
        if (timestamp.isNumber() && timestamp.asLong() > 0) {
            lastTradeAt = Instant.ofEpochSecond(timestamp.asLong());
        }
        String returned = JsonValues.text(root, "symbol");
        return new MarketQuote(
                returned.isBlank() ? symbol : returned,
                price,
                previousClose,
                percentChange.setScale(4, RoundingMode.HALF_UP),
                lastTradeAt
        );
    }

    List<PriceBar> parseBars(String symbol, JsonNode root) {
        if (root == null || !root.isObject()) {
            throw new MarketDataException(
                    "Twelve Data returned an unreadable price history for " + symbol + "."
            );
        }
        rejectError(root);
        JsonNode values = root.path("values");
        if (!values.isArray()) {
            throw new MarketDataException(
                    "Twelve Data returned no price bars for " + symbol + "."
            );
        }
        List<PriceBar> bars = new ArrayList<>();
        for (JsonNode node : values) {
            BigDecimal open = JsonValues.decimal(node, "open");
            BigDecimal high = JsonValues.decimal(node, "high");
            BigDecimal low = JsonValues.decimal(node, "low");
            BigDecimal close = JsonValues.decimal(node, "close");
            BigDecimal volume = JsonValues.decimal(node, "volume");
            String date = JsonValues.text(node, "datetime");
            if (date.isBlank() || open == null || high == null
                    || low == null || close == null || volume == null) {
                continue;
            }
            bars.add(new PriceBar(date, open, high, low, close, volume));
        }
        if (bars.isEmpty()) {
            throw new MarketDataException(
                    "Twelve Data returned no price bars for " + symbol + "."
            );
        }
        return List.copyOf(bars);
    }

    BigDecimal parseLatestRsi(JsonNode root) {
        if (root == null || !root.isObject()) {
            throw new MarketDataException("Twelve Data returned an unreadable RSI.");
        }
        rejectError(root);
        JsonNode values = root.path("values");
        if (!values.isArray() || values.isEmpty()) {
            throw new MarketDataException("Twelve Data returned no RSI value.");
        }
        BigDecimal rsi = JsonValues.decimal(values.get(0), "rsi");
        if (rsi == null) {
            throw new MarketDataException("Twelve Data returned no RSI value.");
        }
        return rsi;
    }

    private JsonTransport.JsonResponse get(String path, String query) {
        return transport.get(
                URI.create(BASE_URL + path + "?" + query),
                Map.of("Authorization", "apikey " + apiKey),
                SOURCE
        );
    }

    private JsonNode ok(JsonTransport.JsonResponse response) {
        if (response.status() == 401 || response.status() == 403) {
            throw new MarketDataException(
                    "Twelve Data rejected the API key (HTTP " + response.status() + ")."
            );
        }
        if (response.status() == 429) {
            throw new MarketDataException(
                    "Twelve Data rate limit reached. The free plan allows about "
                            + "8 calls a minute and 800 a day. Confirm the current limit when you sign up."
            );
        }
        if (response.status() < 200 || response.status() >= 300) {
            String detail = ProviderText.redact(JsonValues.errorMessage(response.json()), apiKey);
            throw new MarketDataException(
                    "Twelve Data returned HTTP " + response.status() + "."
                            + (detail.isBlank() ? "" : " " + detail)
            );
        }
        if (response.json() == null) {
            throw new MarketDataException("Twelve Data returned an unreadable response.");
        }
        rejectError(response.json());
        return response.json();
    }

    private void rejectError(JsonNode root) {
        if (root != null && "error".equalsIgnoreCase(JsonValues.text(root, "status"))) {
            String detail = ProviderText.redact(JsonValues.errorMessage(root), apiKey);
            throw new MarketDataException(
                    "Twelve Data returned an error"
                            + (detail.isBlank() ? "." : ": " + detail)
            );
        }
    }

    private static BigDecimal percentChange(BigDecimal price, BigDecimal previousClose) {
        if (previousClose.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return price.subtract(previousClose)
                .divide(previousClose, 8, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private static BigDecimal firstDecimal(BigDecimal preferred, BigDecimal fallback) {
        return preferred != null ? preferred : fallback;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
