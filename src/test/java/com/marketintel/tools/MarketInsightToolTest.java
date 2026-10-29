package com.marketintel.tools;

import com.marketintel.model.ActionType;
import com.marketintel.model.SentimentReading;
import com.marketintel.model.ToolRequest;
import com.marketintel.model.ToolResult;
import com.marketintel.providers.MarketDataException;
import com.marketintel.providers.SentimentProvider;
import com.marketintel.services.RequestValidator;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketInsightToolTest {

    @Test
    void returnsTheSentimentReadingForAValidSymbol() {
        SentimentProvider sentiment = symbol -> new SentimentReading(
                symbol,
                "SentiSense",
                new BigDecimal("0.20"),
                "Somewhat-Bullish",
                null,
                "",
                "note"
        );
        MarketInsightTool tool = new MarketInsightTool(null, null, sentiment, new RequestValidator());

        assertTrue(tool.supports(ActionType.NEWS_SENTIMENT));
        assertFalse(tool.supports(ActionType.PRICE_HISTORY));

        ToolResult result = tool.execute(new ToolRequest(
                1,
                ActionType.NEWS_SENTIMENT,
                Map.of("symbol", "nvda")
        ));

        assertTrue(result.isSuccess());
        SentimentReading reading = (SentimentReading) result.getData();
        assertEquals("NVDA", reading.getSymbol());
    }

    @Test
    void turnsAProviderFailureIntoAToolFailure() {
        SentimentProvider sentiment = symbol -> {
            throw new MarketDataException("SentiSense monthly quota is exhausted.");
        };
        MarketInsightTool tool = new MarketInsightTool(null, null, sentiment, new RequestValidator());

        ToolResult result = tool.execute(new ToolRequest(
                1,
                ActionType.NEWS_SENTIMENT,
                Map.of("symbol", "NVDA")
        ));

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("quota"));
    }
}
