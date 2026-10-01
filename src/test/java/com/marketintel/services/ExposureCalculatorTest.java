package com.marketintel.services;

import com.marketintel.model.ETFHolding;
import com.marketintel.model.ETFProfile;
import com.marketintel.model.ExposureResult;
import com.marketintel.model.OverlapResult;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExposureCalculatorTest {

    private final ExposureCalculator calculator =
            new ExposureCalculator();

    @Test
    void calculatesSectorExposure() {

        ETFProfile profile =
                new ETFProfile(
                        "TEST",
                        null,
                        null,

                        List.of(),

                        Map.of(
                                "Technology",
                                new BigDecimal(
                                        "0.60"
                                ),

                                "Financials",
                                new BigDecimal(
                                        "0.40"
                                )
                        )
                );

        ExposureResult result =
                calculator
                        .calculateSectorExposure(
                                profile
                        );

        assertEquals(
                "TEST",
                result.getSymbol()
        );

        assertEquals(
                new BigDecimal("0.60"),
                result.getCategories()
                        .get(
                                "Technology"
                        )
        );

        assertEquals(
                new BigDecimal("0.40"),
                result.getCategories()
                        .get(
                                "Financials"
                        )
        );
    }

    @Test
    void calculatesHoldingOverlap() {

        ETFProfile first =
                createProfile(
                        "ETF1",

                        List.of(
                                createHolding(
                                        "NVDA",
                                        "0.10"
                                ),

                                createHolding(
                                        "MSFT",
                                        "0.08"
                                )
                        )
                );

        ETFProfile second =
                createProfile(
                        "ETF2",

                        List.of(
                                createHolding(
                                        "NVDA",
                                        "0.06"
                                ),

                                createHolding(
                                        "AAPL",
                                        "0.07"
                                )
                        )
                );

        OverlapResult result =
                calculator.calculateOverlap(
                        first,
                        second
                );

        assertEquals(
                new BigDecimal("0.06"),
                result.getOverlapWeight()
        );

        assertEquals(
                1,
                result.getCommonHoldings()
                        .size()
        );

        assertEquals(
                "NVDA",
                result.getCommonHoldings()
                        .getFirst()
                        .getSymbol()
        );

        assertEquals(
                new BigDecimal("0.06"),
                result.getCommonHoldings()
                        .getFirst()
                        .getWeight()
        );
    }

    @Test
    void multipleSharedHoldingsAreAdded() {

        ETFProfile first =
                createProfile(
                        "ETF1",

                        List.of(
                                createHolding(
                                        "NVDA",
                                        "0.10"
                                ),

                                createHolding(
                                        "MSFT",
                                        "0.08"
                                )
                        )
                );

        ETFProfile second =
                createProfile(
                        "ETF2",

                        List.of(
                                createHolding(
                                        "NVDA",
                                        "0.06"
                                ),

                                createHolding(
                                        "MSFT",
                                        "0.05"
                                )
                        )
                );

        OverlapResult result =
                calculator.calculateOverlap(
                        first,
                        second
                );

        assertEquals(
                new BigDecimal("0.11"),
                result.getOverlapWeight()
        );

        assertEquals(
                2,
                result.getCommonHoldings()
                        .size()
        );
    }

    @Test
    void nonOverlappingEtfsReturnZero() {

        ETFProfile first =
                createProfile(
                        "ETF1",

                        List.of(
                                createHolding(
                                        "NVDA",
                                        "0.10"
                                )
                        )
                );

        ETFProfile second =
                createProfile(
                        "ETF2",

                        List.of(
                                createHolding(
                                        "AAPL",
                                        "0.10"
                                )
                        )
                );

        OverlapResult result =
                calculator.calculateOverlap(
                        first,
                        second
                );

        assertEquals(
                BigDecimal.ZERO,
                result.getOverlapWeight()
        );

        assertTrue(
                result.getCommonHoldings()
                        .isEmpty()
        );
    }

    private ETFProfile createProfile(
            String symbol,
            List<ETFHolding> holdings) {

        return new ETFProfile(
                symbol,
                null,
                null,
                holdings,
                Map.of()
        );
    }

    private ETFHolding createHolding(
            String symbol,
            String weight) {

        return new ETFHolding(
                symbol,
                symbol,
                new BigDecimal(weight)
        );
    }
}