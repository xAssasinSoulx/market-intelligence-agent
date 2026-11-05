package com.marketintel.gui;

import com.marketintel.model.AssetType;
import com.marketintel.model.MarketQuote;
import com.marketintel.model.PortfolioPosition;
import com.marketintel.services.PortfolioBook;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeDeskViewTest {

    @Test
    void leavesTheTotalBlankWhenNoPositionIsPriced() {
        HomeDeskView.Model model = HomeDeskView.present(PortfolioBook.value(
                List.of(position("SHOP.TO", "4"), position("XEQT.TO", "2")),
                Map.of()
        ));

        assertTrue(model.totalsBlank());
        assertEquals("—", model.marketValue());
        assertEquals("—", model.day());
        assertFalse(model.marketValue().contains("0.00"));
        assertNull(model.coverage());
        assertEquals("NO POSITION COULD BE PRICED.", model.note());
        assertEquals(List.of("SHOP.TO", "XEQT.TO"), symbols(model));
        assertTrue(model.rows().stream().allMatch(HomeDeskView.Row::unpriced));
        assertTrue(model.segments().isEmpty());
    }

    @Test
    void sortsByMarketValueAndNamesTheOmittedSymbols() {
        HomeDeskView.Model model = HomeDeskView.present(sampleBook());

        assertFalse(model.totalsBlank());
        assertEquals("11,763.50", model.marketValue());
        assertEquals("+37.10   (+0.32%)", model.day());
        assertEquals(HomeDeskView.Tone.UP, model.dayTone());
        assertEquals("5 OF 6 PRICED", model.coverage());
        assertTrue(model.note().contains("Total omits SHOP.TO."));
        assertFalse(model.marketValue().contains("USD"));
        assertFalse(model.day().contains("$"));
        assertEquals(
                List.of("QQQ", "AAPL", "MSFT", "SPY", "NVDA", "SHOP.TO"),
                symbols(model)
        );

        HomeDeskView.Row largest = model.rows().getFirst();
        assertEquals("33.29%", largest.weight());
        assertEquals("3,916.40", largest.value());
        assertEquals("+26.80", largest.dayDollars());
        assertEquals("+0.69%", largest.quotePercent());
        assertEquals(HomeDeskView.Tone.UP, largest.dayTone());

        HomeDeskView.Row microsoft = model.rows().get(2);
        assertEquals("17.65%", microsoft.weight());
        assertEquals("-29.00", microsoft.dayDollars());
        assertEquals("-1.38%", microsoft.quotePercent());
        assertEquals(HomeDeskView.Tone.DOWN, microsoft.dayTone());

        HomeDeskView.Row apple = model.rows().get(1);
        assertEquals("0.00%", apple.quotePercent());
        assertEquals("0.00", apple.dayDollars());
        assertEquals(HomeDeskView.Tone.FLAT, apple.dayTone());

        assertTrue(model.rows().getLast().unpriced());
        assertEquals("UNPRICED", model.rows().getLast().weight());
        assertEquals(
                List.of("QQQ", "AAPL", "MSFT", "SPY", "NVDA"),
                model.segments().stream().map(HomeDeskView.Segment::symbol).toList()
        );
        assertEquals(0, BigDecimal.ONE.compareTo(model.segments().stream()
                .map(HomeDeskView.Segment::fraction)
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
    }

    @Test
    void leavesCoverageOffWhenEveryPositionIsPriced() {
        HomeDeskView.Model model = HomeDeskView.present(PortfolioBook.value(
                List.of(position("NVDA", "10")),
                Map.of("NVDA", quote("NVDA", "100", "90", "11.11"))
        ));

        assertNull(model.coverage());
        assertEquals(
                "Quantity times the latest price. Day is the move from the previous close.",
                model.note()
        );
        assertEquals(1, model.rows().size());
        assertEquals(1, model.segments().size());
    }

    @Test
    void breaksEqualValuesBySymbolAndKeepsUnpricedLast() {
        HomeDeskView.Model model = HomeDeskView.present(PortfolioBook.value(
                List.of(
                        position("ZED", "1"),
                        position("MSFT", "1"),
                        position("AAPL", "1")
                ),
                Map.of(
                        "MSFT", quote("MSFT", "10", "10", "0"),
                        "AAPL", quote("AAPL", "10", "10", "0")
                )
        ));

        assertEquals(List.of("AAPL", "MSFT", "ZED"), symbols(model));
        assertEquals("2 OF 3 PRICED", model.coverage());
        assertTrue(model.note().contains("Total omits ZED."));
    }

    @Test
    void showsARealZeroPriceAndLeavesAMissingQuoteBlank() {
        HomeDeskView.Model pricedAtZero = HomeDeskView.present(PortfolioBook.value(
                List.of(position("NVDA", "10")),
                Map.of("NVDA", quote("NVDA", "0", "0", "0"))
        ));
        HomeDeskView.Model missing = HomeDeskView.present(PortfolioBook.value(
                List.of(position("NVDA", "10")),
                Map.of()
        ));

        assertFalse(pricedAtZero.totalsBlank());
        assertEquals("0.00", pricedAtZero.marketValue());
        assertEquals("—", pricedAtZero.rows().getFirst().weight());
        assertTrue(pricedAtZero.segments().isEmpty());
        assertTrue(missing.totalsBlank());
        assertEquals("—", missing.marketValue());
    }

    @Test
    void omitsTheDayPercentWhenThePreviousValueIsZero() {
        HomeDeskView.Model model = HomeDeskView.present(PortfolioBook.value(
                List.of(position("NVDA", "10")),
                Map.of("NVDA", quote("NVDA", "5", "0", "0"))
        ));

        assertEquals("50.00", model.marketValue());
        assertEquals("+50.00", model.day());
        assertFalse(model.day().contains("%"));
    }

    @Test
    void keepsTheLastBookWhileALaterLoadIsRunning() {
        HoldingsRefresh refresh = new HoldingsRefresh();

        assertFalse(refresh.retainWhileLoading());
        refresh.showedBook();
        assertTrue(refresh.retainWhileLoading());
        refresh.clear();
        assertFalse(refresh.retainWhileLoading());
    }

    @Test
    void namesThePricingNoteUsedWhileQuotesAreInFlight() {
        assertEquals("PRICING 6 POSITIONS...", HomeDeskView.pricingNote(6));
    }

    private static List<String> symbols(HomeDeskView.Model model) {
        return model.rows().stream().map(HomeDeskView.Row::symbol).toList();
    }

    private static PortfolioBook sampleBook() {
        return PortfolioBook.value(
                List.of(
                        position("AAPL", "12"),
                        position("MSFT", "5"),
                        position("NVDA", "10"),
                        position("QQQ", "8"),
                        position("SPY", "3"),
                        position("SHOP.TO", "4")
                ),
                Map.of(
                        "AAPL", quote("AAPL", "228.40", "228.40", "0"),
                        "MSFT", quote("MSFT", "415.20", "421.00", "-1.38"),
                        "NVDA", quote("NVDA", "131.40", "128.10", "2.58"),
                        "QQQ", quote("QQQ", "489.55", "486.20", "0.69"),
                        "SPY", quote("SPY", "572.10", "570.00", "0.37")
                )
        );
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
