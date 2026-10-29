package com.marketintel.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketintel.model.SentimentReading;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SentiSenseAdapterTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final SentiSenseAdapter adapter = new SentiSenseAdapter("dummy-key");

    @Test
    void usesTheLatestPointAndTheSharedDisplayBand() throws Exception {
        var root = mapper.readTree("""
                [
                  {"timestamp": 1761595200000, "value": 0.10},
                  {"timestamp": 1761681600000, "value": 0.42}
                ]
                """);

        SentimentReading reading = adapter.parseSentiment("NVDA", root);

        assertEquals("NVDA", reading.getSymbol());
        assertEquals("SentiSense", reading.getSource());
        assertEquals(0, new BigDecimal("0.42").compareTo(reading.getScore()));
        assertEquals(0, new BigDecimal("0.32").compareTo(reading.getChange()));
        assertEquals("Bullish", reading.getLabel());
        assertEquals("2025-10-28T20:00:00Z", reading.getAsOf());
    }

    @Test
    void readsANestedMetricValueWhenTheFlatValueIsAbsent() throws Exception {
        var root = mapper.readTree("""
                [{
                  "timestamp": 1761681600000,
                  "metricValue": {"value": {"value": -0.40}}
                }]
                """);

        SentimentReading reading = adapter.parseSentiment("AAPL", root);

        assertEquals(0, new BigDecimal("-0.40").compareTo(reading.getScore()));
        assertEquals("Bearish", reading.getLabel());
        assertNull(reading.getChange());
    }

    @Test
    void rejectsAnEmptySeries() throws Exception {
        var root = mapper.readTree("[]");

        MarketDataException error = assertThrows(
                MarketDataException.class,
                () -> adapter.parseSentiment("ZZZZ", root)
        );

        assertTrue(error.getMessage().contains("ZZZZ"));
    }
}
