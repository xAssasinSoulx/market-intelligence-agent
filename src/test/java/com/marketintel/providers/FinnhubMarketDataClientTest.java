package com.marketintel.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketintel.model.MarketQuote;
import com.marketintel.model.SecurityOverview;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

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
    void convertsPercentCountryExposureToFractions() throws Exception {
        JsonNode root = mapper.readTree("""
                {"countryExposure": [
                   {"country": "United States", "exposure": 97.5},
                   {"country": "Ireland", "exposure": 2.5}
                ], "symbol": "SPY"}
                """);

        Map<String, BigDecimal> weights = client.parseCountryExposure("SPY", root);

        assertDecimal("0.975", weights.get("United States"));
        assertDecimal("0.025", weights.get("Ireland"));
    }

    @Test
    void keepsCountryWeightsThatAreAlreadyFractions() throws Exception {
        JsonNode root = mapper.readTree("""
                {"countryExposure": [
                   {"country": "United States", "exposure": 0.60},
                   {"country": "Japan", "exposure": 0.40}
                ]}
                """);

        Map<String, BigDecimal> weights = client.parseCountryExposure("QQQ", root);

        assertDecimal("0.60", weights.get("United States"));
        assertDecimal("0.40", weights.get("Japan"));
    }

    @Test
    void treatsABlankCountryAsUnknownAndRefusesAnEmptyReport() throws Exception {
        JsonNode mixed = mapper.readTree("""
                {"countryExposure": [
                   {"country": "Canada", "exposure": 70},
                   {"country": "", "exposure": 30}
                ]}
                """);

        Map<String, BigDecimal> weights = client.parseCountryExposure("XEQT", mixed);

        assertDecimal("0.70", weights.get("Canada"));
        assertDecimal("0.30", weights.get("Unknown"));

        JsonNode empty = mapper.readTree("""
                {"countryExposure": [], "symbol": "QQQ"}
                """);

        MarketDataException error = assertThrows(
                MarketDataException.class,
                () -> client.parseCountryExposure("QQQ", empty)
        );

        assertTrue(error.getMessage().contains("QQQ"));
        assertTrue(error.getMessage().contains("No allocation was estimated"));
    }

    @Test
    void describesACountry403AsAPlanLimitRatherThanABadKey() {
        String message = FinnhubMarketDataClient.rejectionMessage(
                FinnhubMarketDataClient.countryExposureUri("IVV"),
                403,
                "{\"error\":\"You don't have access to this resource.\"}"
        );

        assertTrue(message.contains("HTTP 403"));
        assertTrue(message.contains("free plan"));
        assertTrue(message.contains("quotes"));
        assertTrue(message.contains("You don't have access to this resource."));
        assertTrue(!message.contains("rejected the API key"));
    }

    @Test
    void stillTreats401AsARejectedKey() {
        String message = FinnhubMarketDataClient.rejectionMessage(
                java.net.URI.create("https://finnhub.io/api/v1/quote?symbol=NVDA"),
                401,
                "{\"error\":\"Invalid API key\"}"
        );

        assertEquals("Finnhub rejected the API key (HTTP 401).", message);
    }

    @Test
    void readsHeadquartersCountryFromACompanyProfile() throws Exception {
        JsonNode root = mapper.readTree("""
                {"country": "US", "name": "Apple Inc", "ticker": "AAPL"}
                """);

        assertEquals("US", FinnhubMarketDataClient.parseHeadquartersCountry(root));
        assertEquals(null, FinnhubMarketDataClient.parseHeadquartersCountry(mapper.readTree("{}")));
    }

    @Test
    void requestsTheLiveCountryEndpointRatherThanTheRedirectingPath() {
        java.net.URI uri = FinnhubMarketDataClient.countryExposureUri("SPY");

        assertEquals(
                "https://api.finnhub.io/api/v1/etf/country?symbol=SPY",
                uri.toString()
        );
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
