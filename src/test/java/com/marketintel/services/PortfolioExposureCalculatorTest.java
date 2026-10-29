package com.marketintel.services;

import com.marketintel.model.AssetType;
import com.marketintel.model.ETFHolding;
import com.marketintel.model.PortfolioExposureInput;
import com.marketintel.model.PortfolioExposureResult;
import com.marketintel.model.PortfolioExposureResult.CurrencyBook;
import com.marketintel.model.PortfolioExposureResult.HoldingLine;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortfolioExposureCalculatorTest {

    private final PortfolioExposureCalculator calculator = new PortfolioExposureCalculator();

    @Test
    void valuesASingleStockFromPriceTimesQuantity() {
        PortfolioExposureResult result = calculator.calculate(List.of(
                stock("NVDA", "10", "100", "USD", "Technology")
        ));

        assertDecimal("1000", result.getPositions().getFirst().getMarketValue());
        CurrencyBook book = result.getBooks().getFirst();
        assertDecimal("1000", book.getTotalMarketValue());
        assertDecimal("1", book.getSectorWeights().get("Technology"));
        assertTrue(book.getLookThrough().isEmpty());
    }

    @Test
    void weightsSeveralPositionsByMarketValue() {
        PortfolioExposureResult result = calculator.calculate(List.of(
                stock("NVDA", "1", "100", "USD", "Technology"),
                stock("JPM", "1", "300", "usd", "Financials")
        ));

        CurrencyBook book = result.getBooks().getFirst();
        assertEquals("USD", book.getCurrency());
        assertDecimal("0.25", book.getSectorWeights().get("Technology"));
        assertDecimal("0.75", book.getSectorWeights().get("Financials"));
    }

    @Test
    void looksThroughEtfHoldingsWithoutAddingTheDirectStockAgain() {
        PortfolioExposureResult result = calculator.calculate(List.of(
                etf("QQQ", "2", "50", "USD", Map.of("Technology", new BigDecimal("0.80")),
                        List.of(holding("AAPL", "0.40"), holding("MSFT", "0.60"))),
                stock("XOM", "1", "100", "USD", "Energy")
        ));

        CurrencyBook book = result.getBooks().getFirst();
        assertDecimal("0.40", book.getSectorWeights().get("Technology"));
        assertDecimal("0.50", book.getSectorWeights().get("Energy"));
        HoldingLine apple = book.getLookThrough().stream()
                .filter(line -> line.getSymbol().equals("AAPL"))
                .findFirst()
                .orElseThrow();
        assertDecimal("0.20", apple.getPortfolioWeight());
        assertEquals("QQQ", apple.getSource());
        assertTrue(book.getLookThrough().stream().noneMatch(line -> line.getSymbol().equals("XOM")));
    }

    @Test
    void combinesTheSameHoldingFromTwoEtfs() {
        PortfolioExposureResult result = calculator.calculate(List.of(
                etf("QQQ", "1", "100", "USD", Map.of("Technology", BigDecimal.ONE),
                        List.of(holding("AAPL", "0.20"))),
                etf("SPY", "1", "100", "USD", Map.of("Technology", BigDecimal.ONE),
                        List.of(holding("AAPL", "0.40")))
        ));

        HoldingLine apple = result.getBooks().getFirst().getLookThrough().getFirst();
        assertDecimal("0.30", apple.getPortfolioWeight());
        assertTrue(apple.getSource().contains("QQQ"));
        assertTrue(apple.getSource().contains("SPY"));
    }

    @Test
    void excludesAMissingQuoteFromTheWeights() {
        PortfolioExposureResult result = calculator.calculate(List.of(
                stock("NVDA", "1", "100", "USD", "Technology"),
                stock("MSFT", "5", null, "USD", "Technology")
        ));

        assertEquals(1, result.getBooks().size());
        assertDecimal("100", result.getBooks().getFirst().getTotalMarketValue());
        assertTrue(result.getLimitations().stream().anyMatch(note -> note.contains("MSFT")));
        assertTrue(result.getPositions().stream().anyMatch(line ->
                line.getSymbol().equals("MSFT") && !line.isIncluded()));
    }

    @Test
    void doesNotAddDifferentCurrenciesTogether() {
        PortfolioExposureResult result = calculator.calculate(List.of(
                stock("NVDA", "1", "100", "USD", "Technology"),
                stock("SAP", "1", "100", "EUR", "Technology")
        ));

        assertEquals(2, result.getBooks().size());
        assertDecimal("100", result.getBooks().get(0).getTotalMarketValue());
        assertDecimal("100", result.getBooks().get(1).getTotalMarketValue());
        assertTrue(result.getLimitations().stream().anyMatch(note -> note.contains("not combined")));
    }

    @Test
    void classifiesAMissingSectorAsUnknown() {
        PortfolioExposureInput input = new PortfolioExposureInput(
                "XYZ",
                new BigDecimal("1"),
                AssetType.OTHER,
                new BigDecimal("40"),
                "USD",
                Map.of(),
                List.of(),
                null
        );

        PortfolioExposureResult result = calculator.calculate(List.of(input));

        assertDecimal("1", result.getBooks().getFirst().getSectorWeights().get("Unknown"));
    }

    @Test
    void reportsAnEmptyPortfolio() {
        PortfolioExposureResult result = calculator.calculate(List.of());

        assertTrue(result.getPositions().isEmpty());
        assertTrue(result.getBooks().isEmpty());
        assertTrue(result.getLimitations().getFirst().contains("no positions"));
    }

    private static PortfolioExposureInput stock(
            String symbol,
            String quantity,
            String price,
            String currency,
            String sector) {

        return new PortfolioExposureInput(
                symbol,
                new BigDecimal(quantity),
                AssetType.STOCK,
                price == null ? null : new BigDecimal(price),
                currency,
                Map.of(sector, BigDecimal.ONE),
                List.of(),
                null
        );
    }

    private static PortfolioExposureInput etf(
            String symbol,
            String quantity,
            String price,
            String currency,
            Map<String, BigDecimal> sectors,
            List<ETFHolding> holdings) {

        return new PortfolioExposureInput(
                symbol,
                new BigDecimal(quantity),
                AssetType.ETF,
                new BigDecimal(price),
                currency,
                sectors,
                holdings,
                null
        );
    }

    private static ETFHolding holding(String symbol, String weight) {
        return new ETFHolding(symbol, symbol, new BigDecimal(weight));
    }

    private static void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), actual.toPlainString());
    }
}
