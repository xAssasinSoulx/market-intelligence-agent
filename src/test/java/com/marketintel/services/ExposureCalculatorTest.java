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
    void sumsHoldingCountriesAndKeepsABlankCountryUnknown() {

        ETFProfile profile = new ETFProfile(
                "QQQ",
                null,
                null,
                List.of(
                        createHolding("NVDA", "0.50", "United States"),
                        createHolding("SHOP", "0.30", "Canada"),
                        createHolding("TSM", "0.10", null),
                        createHolding("ASML", "0.10", "none")
                ),
                Map.of()
        );

        ExposureResult result = calculator.calculateGeographicExposure(profile);

        assertEquals(new BigDecimal("0.50"), result.getCategories().get("United States"));
        assertEquals(new BigDecimal("0.30"), result.getCategories().get("Canada"));
        assertEquals(new BigDecimal("0.20"), result.getCategories().get("Unknown"));
        assertTrue(result.getNote().contains("not the fund's domicile"));
        assertFalse(result.getNote().contains("remainder"));
    }

    @Test
    void mergesCountryNamesThatDifferOnlyByCase() {

        ETFProfile profile = new ETFProfile(
                "QQQ",
                null,
                null,
                List.of(
                        createHolding("A", "0.40", "United States"),
                        createHolding("B", "0.60", "united states")
                ),
                Map.of()
        );

        ExposureResult result = calculator.calculateGeographicExposure(profile);

        assertEquals(1, result.getCategories().size());
        assertEquals(new BigDecimal("1.00"), result.getCategories().get("United States"));
    }

    @Test
    void doesNotEstimateGeographyWhenNoHoldingHasACountry() {

        ETFProfile profile = createProfile(
                "QQQ",
                List.of(createHolding("NVDA", "0.10"))
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculateGeographicExposure(profile)
        );

        assertTrue(error.getMessage().contains("not estimated"));
        assertTrue(error.getMessage().contains("QQQ"));
    }

    @Test
    void doesNotRescaleCountryWeightsThatSumAboveOne() {

        ETFProfile profile = new ETFProfile(
                "QQQ",
                null,
                null,
                List.of(
                        createHolding("A", "0.80", "United States"),
                        createHolding("B", "0.50", "Ireland")
                ),
                Map.of()
        );

        ExposureResult result = calculator.calculateGeographicExposure(profile);

        assertEquals(new BigDecimal("0.80"), result.getCategories().get("United States"));
        assertEquals(new BigDecimal("0.50"), result.getCategories().get("Ireland"));
        assertTrue(result.getNote().contains("not rescaled"));
    }

    @Test
    void reportsTheUnassignedRemainderInsteadOfInventingACountry() {

        ETFProfile profile = new ETFProfile(
                "QQQ",
                null,
                null,
                List.of(createHolding("NVDA", "0.40", "United States")),
                Map.of()
        );

        ExposureResult result = calculator.calculateGeographicExposure(profile);

        assertEquals(new BigDecimal("0.40"), result.getCategories().get("United States"));
        assertFalse(result.getCategories().containsKey("Unknown"));
        assertTrue(result.getNote().contains("40.00%"));
        assertTrue(result.getNote().contains("was not estimated"));
    }

    @Test
    void labelsProviderCountryWeightsAsFundExposure() {

        ExposureResult result = calculator.calculateReportedCountryExposure(
                "spy",
                Map.of(
                        "United States", new BigDecimal("0.97"),
                        "Ireland", new BigDecimal("0.03")
                )
        );

        assertEquals("SPY", result.getSymbol());
        assertEquals(new BigDecimal("0.97"), result.getCategories().get("United States"));
        assertTrue(result.getNote().contains("not a sum of the listing countries"));
        assertFalse(result.getNote().contains("remainder"));
    }

    @Test
    void refusesAnEmptyProviderCountryReport() {

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculateReportedCountryExposure("QQQ", Map.of())
        );

        assertTrue(error.getMessage().contains("not estimated"));
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

        return createHolding(symbol, weight, null);
    }

    private ETFHolding createHolding(
            String symbol,
            String weight,
            String country) {

        return new ETFHolding(
                symbol,
                symbol,
                new BigDecimal(weight),
                country
        );
    }
}