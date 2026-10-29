package com.marketintel.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketintel.model.FinancialStatements;
import com.marketintel.model.ReportingPeriod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialModelingPrepAdapterTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final FinancialModelingPrepAdapter adapter =
            new FinancialModelingPrepAdapter("dummy-key");

    @Test
    void rejectsABlankKey() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FinancialModelingPrepAdapter(" ")
        );
    }

    @Test
    void mergesThreeStatementsOnTheReportingDate() throws Exception {
        var income = mapper.readTree("""
                [{
                  "date": "2024-09-28",
                  "symbol": "AAPL",
                  "reportedCurrency": "USD",
                  "period": "FY",
                  "revenue": 391035000000,
                  "grossProfit": 180683000000,
                  "operatingIncome": 123216000000,
                  "netIncome": 93736000000,
                  "epsDiluted": 6.08
                }]
                """);
        var balance = mapper.readTree("""
                [{
                  "date": "2024-09-28",
                  "reportedCurrency": "USD",
                  "period": "FY",
                  "totalAssets": 364980000000,
                  "totalLiabilities": 308030000000,
                  "totalStockholdersEquity": 56950000000,
                  "cashAndCashEquivalents": 29943000000
                }]
                """);
        var cashFlow = mapper.readTree("""
                [{
                  "date": "2024-09-28",
                  "operatingCashFlow": 118254000000,
                  "freeCashFlow": 108807000000
                }]
                """);

        FinancialStatements statements = adapter.parseStatements("AAPL", income, balance, cashFlow);

        assertEquals("AAPL", statements.getSymbol());
        assertEquals("Financial Modeling Prep", statements.getSource());
        assertEquals("USD", statements.getCurrency());
        assertEquals(1, statements.getPeriods().size());
        ReportingPeriod period = statements.getPeriods().getFirst();
        assertEquals("2024-09-28", period.getDate());
        assertEquals("FY", period.getPeriodLabel());
        assertEquals(0, new java.math.BigDecimal("93736000000").compareTo(period.getNetIncome()));
        assertEquals(0, new java.math.BigDecimal("6.08").compareTo(period.getDilutedEps()));
        assertEquals(0, new java.math.BigDecimal("56950000000").compareTo(period.getTotalEquity()));
        assertEquals(0, new java.math.BigDecimal("108807000000").compareTo(period.getFreeCashFlow()));
    }

    @Test
    void acceptsTheCashFlowAliasAndLeavesMissingLinesEmpty() throws Exception {
        var income = mapper.readTree("""
                [{"date": "2023-09-30", "period": "FY", "revenue": 100}]
                """);
        var balance = mapper.readTree("[]");
        var cashFlow = mapper.readTree("""
                [{"date": "2023-09-30", "netCashProvidedByOperatingActivities": 40}]
                """);

        ReportingPeriod period = adapter.parseStatements("TEST", income, balance, cashFlow)
                .getPeriods()
                .getFirst();

        assertEquals(0, new java.math.BigDecimal("40").compareTo(period.getOperatingCashFlow()));
        assertNull(period.getTotalAssets());
        assertNull(period.getFreeCashFlow());
    }

    @Test
    void rejectsAnEmptyStatementSet() throws Exception {
        var empty = mapper.readTree("[]");

        MarketDataException error = assertThrows(
                MarketDataException.class,
                () -> adapter.parseStatements("NONE", empty, empty, empty)
        );

        assertTrue(error.getMessage().contains("NONE"));
    }
}
