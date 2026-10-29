package com.marketintel.services;

import com.marketintel.model.AssetType;
import com.marketintel.model.MarketQuote;
import com.marketintel.model.PortfolioPosition;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortfolioBookTest {

    @Test
    void valuesPricedPositionsAndNamesTheRest() {
        PortfolioBook book = PortfolioBook.value(
                List.of(
                        position("NVDA", "10"),
                        position("SHOP.TO", "2")
                ),
                Map.of("NVDA", quote("NVDA", "100", "90", "11.1111"))
        );

        assertEquals(0, new BigDecimal("1000").compareTo(book.marketValue()));
        assertEquals(0, new BigDecimal("100").compareTo(book.dayChange()));
        assertEquals(0, new BigDecimal("11.11").compareTo(book.dayPercent()));
        assertEquals(List.of("SHOP.TO"), book.unpriced());
        assertEquals("NVDA", book.priced().getFirst().symbol());
    }

    @Test
    void leavesTheDayPercentEmptyWhenThePreviousValueIsZero() {
        PortfolioBook book = PortfolioBook.value(
                List.of(position("NVDA", "10")),
                Map.of("NVDA", quote("NVDA", "5", "0", "0"))
        );

        assertNull(book.dayPercent());
        assertTrue(book.unpriced().isEmpty());
    }

    private static PortfolioPosition position(String symbol, String quantity) {
        return new PortfolioPosition(symbol, new BigDecimal(quantity), AssetType.STOCK);
    }

    private static MarketQuote quote(
            String symbol,
            String price,
            String previous,
            String percent) {

        return new MarketQuote(
                symbol,
                new BigDecimal(price),
                new BigDecimal(previous),
                new BigDecimal(percent)
        );
    }
}
