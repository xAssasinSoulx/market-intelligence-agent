package com.marketintel.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketintel.model.MarketQuote;
import com.marketintel.model.SecurityOverview;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinnhubMarketDataClientTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final FinnhubMarketDataClient client =
            new FinnhubMarketDataClient("dummy-key");

    @Test
    void parsesAQuote() throws Exception {
        JsonNode root = mapper.readTree("""
                {"c": 343.83, "d": -3.27, "dp": -0.9421, "h": 350.1,
                 "l": 341.2, "o": 348.0, "pc": 347.1, "t": 1761681600}
                """);

        MarketQuote quote = client.parseQuote("AAPL", root);

        assertEquals("AAPL", quote.getSymbol());
        assertDecimal("343.83", quote.getPrice());
        assertDecimal("347.1", quote.getPreviousClose());
        assertDecimal("-0.9421", quote.getPercentChange());
        assertEquals(
                Instant.parse("2025-10-28T20:00:00Z"),
                quote.getLastTradeAt().orElseThrow()
        );
    }

    @Test
    void leavesTheLastTradeTimeEmptyWhenFinnhubSendsZero() throws Exception {
        JsonNode root = mapper.readTree("""
                {"c": 110, "pc": 100, "t": 0}
                """);

        MarketQuote quote = client.parseQuote("TEST", root);

        assertTrue(quote.getLastTradeAt().isEmpty());
    }

    @Test
    void derivesPercentChangeWhenFinnhubOmitsIt() throws Exception {
        JsonNode root = mapper.readTree("""
                {"c": 110, "pc": 100}
                """);

        MarketQuote quote = client.parseQuote("TEST", root);

        assertDecimal("10", quote.getPercentChange());
    }

    @Test
    void treatsAnAllZeroQuoteAsUnknownSymbol() throws Exception {
        JsonNode root = mapper.readTree("""
                {"c": 0, "d": null, "dp": null, "h": 0, "l": 0, "o": 0, "pc": 0, "t": 0}
                """);

        MarketDataException error = assertThrows(
                MarketDataException.class,
                () -> client.parseQuote("ZZZZ", root)
        );

        assertTrue(error.getMessage().contains("ZZZZ"));
    }

    @Test
    void parsesAProfileIntoAnOverview() throws Exception {
        JsonNode root = mapper.readTree("""
                {"country": "US", "currency": "USD", "exchange": "NASDAQ NMS - GLOBAL MARKET",
                 "finnhubIndustry": "Technology", "ipo": "1980-12-12",
                 "marketCapitalization": 2500000.5, "name": "Apple Inc",
                 "ticker": "AAPL", "weburl": "https://www.apple.com/"}
                """);

        SecurityOverview overview = client.parseOverview("AAPL", root);

        assertEquals("AAPL", overview.getSymbol());
        assertEquals("Apple Inc", overview.getName());
        assertEquals("USD", overview.getCurrency());
        assertEquals("Technology", overview.getSector());
        assertEquals("Technology", overview.getIndustry());
        assertTrue(overview.getDescription().contains("Finnhub industry"));
        assertDecimal("2500000500000", overview.getMarketCapitalization());
    }

    @Test
    void treatsAnEmptyProfileAsUnknownSymbol() throws Exception {
        JsonNode root = mapper.readTree("{}");

        MarketDataException error = assertThrows(
                MarketDataException.class,
                () -> client.parseOverview("ZZZZ", root)
        );

        assertTrue(error.getMessage().contains("ZZZZ"));
    }

    @Test
    void reportsUsDollarsAsTheOnlyQuoteCurrency() {
        assertEquals("USD", client.quoteCurrency().orElseThrow());
    }

    @Test
    void rejectsABlankKey() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FinnhubMarketDataClient("  ")
        );
    }

    private static void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), actual.toPlainString());
    }
}
