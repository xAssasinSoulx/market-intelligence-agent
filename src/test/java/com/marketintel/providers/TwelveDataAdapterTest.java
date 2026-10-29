package com.marketintel.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketintel.model.MarketQuote;
import com.marketintel.model.PriceBar;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TwelveDataAdapterTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final TwelveDataAdapter adapter = new TwelveDataAdapter("dummy-key");

    @Test
    void parsesAQuoteAndDerivesTheChangeWhenThePercentIsAbsent() throws Exception {
        var root = mapper.readTree("""
                {
                  "symbol": "AAPL",
                  "close": "110",
                  "previous_close": "100",
                  "timestamp": 1761681600
                }
                """);

        MarketQuote quote = adapter.parseQuote("AAPL", root);

        assertEquals("AAPL", quote.getSymbol());
        assertEquals(0, new BigDecimal("110").compareTo(quote.getPrice()));
        assertEquals(0, new BigDecimal("10.0000").compareTo(quote.getPercentChange()));
        assertEquals(Instant.parse("2025-10-28T20:00:00Z"), quote.getLastTradeAt().orElseThrow());
        assertEquals("USD", adapter.quoteCurrency().orElseThrow());
    }

    @Test
    void parsesDailyBarsAndSkipsAnIncompleteRow() throws Exception {
        var root = mapper.readTree("""
                {
                  "meta": {"symbol": "AAPL", "currency": "USD"},
                  "values": [
                    {
                      "datetime": "2024-10-29",
                      "open": "230",
                      "high": "234",
                      "low": "229",
                      "close": "233",
                      "volume": "1000"
                    },
                    {"datetime": "2024-10-28", "close": "230"}
                  ],
                  "status": "ok"
                }
                """);

        List<PriceBar> bars = adapter.parseBars("AAPL", root);

        assertEquals(1, bars.size());
        assertEquals("2024-10-29", bars.getFirst().getDate());
        assertEquals(0, new BigDecimal("233").compareTo(bars.getFirst().getClose()));
    }

    @Test
    void readsTheNewestRsiAndSurfacesVendorErrors() throws Exception {
        var rsi = mapper.readTree("""
                {"values": [{"datetime": "2024-10-29", "rsi": "62.5"}], "status": "ok"}
                """);
        assertEquals(0, new BigDecimal("62.5").compareTo(adapter.parseLatestRsi(rsi)));

        var error = mapper.readTree("""
                {"code": 400, "message": "Invalid symbol", "status": "error"}
                """);
        MarketDataException failure = assertThrows(
                MarketDataException.class,
                () -> adapter.parseBars("BAD", error)
        );
        assertTrue(failure.getMessage().contains("Invalid symbol"));
    }
}
