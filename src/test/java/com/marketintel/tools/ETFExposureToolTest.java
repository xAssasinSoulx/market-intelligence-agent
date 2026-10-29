package com.marketintel.tools;

import com.marketintel.model.ActionType;
import com.marketintel.model.ETFHolding;
import com.marketintel.model.ETFProfile;
import com.marketintel.model.ExposureResult;
import com.marketintel.model.ToolRequest;
import com.marketintel.model.ToolResult;

import com.marketintel.providers.CountryExposureSource;
import com.marketintel.providers.ETFDataProvider;
import com.marketintel.providers.HeadquartersLookup;
import com.marketintel.providers.MarketDataException;

import com.marketintel.services.ExposureCalculator;
import com.marketintel.services.RequestValidator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ETFExposureToolTest {

    private ETFExposureTool tool;

    @BeforeEach
    void setUp() {

        ETFDataProvider provider =
                new FakeETFDataProvider();

        tool =
                new ETFExposureTool(
                        provider,
                        new ExposureCalculator(),
                        new RequestValidator()
                );
    }

    @Test
    void supportsSectorExposure() {

        assertTrue(
                tool.supports(
                        ActionType
                                .ETF_SECTOR_EXPOSURE
                )
        );
    }

    @Test
    void supportsGeographicExposureWhenHoldingsCanBeRead() {

        assertTrue(
                tool.supports(
                        ActionType.ETF_GEOGRAPHIC_EXPOSURE
                )
        );
    }

    @Test
    void doesNotSupportOverlap() {

        assertFalse(
                tool.supports(
                        ActionType.ETF_OVERLAP
                )
        );
    }

    @Test
    void returnsSectorExposure() {

        ToolRequest request =
                new ToolRequest(
                        1,
                        ActionType
                                .ETF_SECTOR_EXPOSURE,

                        Map.of(
                                "symbol",
                                "QQQ"
                        )
                );

        ToolResult result =
                tool.execute(
                        request
                );

        assertTrue(
                result.isSuccess()
        );

        assertInstanceOf(
                ExposureResult.class,
                result.getData()
        );

        ExposureResult exposure =
                (ExposureResult)
                        result.getData();

        assertEquals(
                "QQQ",
                exposure.getSymbol()
        );

        assertEquals(
                new BigDecimal("0.60"),
                exposure
                        .getCategories()
                        .get("Technology")
        );
    }

    @Test
    void doesNotEstimateGeographyWhenHoldingsHaveNoCountry() {

        ToolResult result = tool.execute(new ToolRequest(
                1,
                ActionType.ETF_GEOGRAPHIC_EXPOSURE,
                Map.of("symbol", "QQQ")
        ));

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("not estimated"));
    }

    @Test
    void aggregatesHoldingCountriesWhenNoFundReportIsConfigured() {

        ETFExposureTool holdingTool = new ETFExposureTool(
                symbol -> new ETFProfile(
                        symbol,
                        null,
                        null,
                        List.of(new ETFHolding(
                                "NVDA",
                                "NVIDIA",
                                new BigDecimal("0.70"),
                                "United States"
                        )),
                        Map.of()
                ),
                new ExposureCalculator(),
                new RequestValidator()
        );

        ToolResult result = holdingTool.execute(new ToolRequest(
                1,
                ActionType.ETF_GEOGRAPHIC_EXPOSURE,
                Map.of("symbol", "QQQ")
        ));

        assertTrue(result.isSuccess());
        ExposureResult exposure = (ExposureResult) result.getData();
        assertEquals(new BigDecimal("0.70"), exposure.getCategories().get("United States"));
        assertTrue(exposure.getNote().contains("not the fund's domicile"));
    }

    @Test
    void usesTheProviderCountryReportInsteadOfTheHoldingList() {

        ETFDataProvider unused = symbol -> {
            throw new AssertionError("Holdings should not be read when a country report exists.");
        };
        CountryExposureSource countries = symbol -> Map.of(
                "United States", new BigDecimal("0.97"),
                "Ireland", new BigDecimal("0.03")
        );

        ETFExposureTool reported = new ETFExposureTool(
                unused,
                new ExposureCalculator(),
                new RequestValidator(),
                countries,
                false
        );

        assertFalse(reported.supports(ActionType.ETF_SECTOR_EXPOSURE));

        ToolResult result = reported.execute(new ToolRequest(
                1,
                ActionType.ETF_GEOGRAPHIC_EXPOSURE,
                Map.of("symbol", "spy")
        ));

        assertTrue(result.isSuccess());
        ExposureResult exposure = (ExposureResult) result.getData();
        assertEquals("SPY", exposure.getSymbol());
        assertEquals(new BigDecimal("0.97"), exposure.getCategories().get("United States"));
        assertTrue(exposure.getNote().contains("not a sum of the listing countries"));
    }

    @Test
    void returnsTheProviderErrorWhenCountryExposureFails() {

        CountryExposureSource countries = symbol -> {
            throw new MarketDataException(
                    "Finnhub did not return country exposure for " + symbol
                            + ". No allocation was estimated."
            );
        };

        ETFExposureTool reported = new ETFExposureTool(
                new FakeETFDataProvider(),
                new ExposureCalculator(),
                new RequestValidator(),
                countries,
                true
        );

        ToolResult result = reported.execute(new ToolRequest(
                1,
                ActionType.ETF_GEOGRAPHIC_EXPOSURE,
                Map.of("symbol", "QQQ")
        ));

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("No allocation was estimated"));
    }

    @Test
    void fallsBackToHeadquartersCountriesWhenTheFundReportIsRefused() {

        CountryExposureSource countries = symbol -> {
            throw new MarketDataException(
                    "Finnhub refused ETF country exposure (HTTP 403)."
            );
        };
        HeadquartersLookup headquarters = symbol -> switch (symbol) {
            case "AAPL" -> "US";
            case "ASML" -> "NL";
            default -> null;
        };

        ETFExposureTool reported = new ETFExposureTool(
                symbol -> new ETFProfile(
                        symbol,
                        null,
                        null,
                        List.of(
                                new ETFHolding("AAPL", "Apple", new BigDecimal("0.07")),
                                new ETFHolding("ASML", "ASML", new BigDecimal("0.02")),
                                new ETFHolding("CASH", "Cash", new BigDecimal("0.01"))
                        ),
                        Map.of()
                ),
                new ExposureCalculator(),
                new RequestValidator(),
                countries,
                true,
                headquarters
        );

        ToolResult result = reported.execute(new ToolRequest(
                1,
                ActionType.ETF_GEOGRAPHIC_EXPOSURE,
                Map.of("symbol", "IVV")
        ));

        assertTrue(result.isSuccess());
        ExposureResult exposure = (ExposureResult) result.getData();
        assertEquals(new BigDecimal("0.07"), exposure.getCategories().get("US"));
        assertEquals(new BigDecimal("0.02"), exposure.getCategories().get("NL"));
        assertEquals(new BigDecimal("0.01"), exposure.getCategories().get("Unknown"));
        assertTrue(exposure.getNote().contains("headquarters country"));
        assertTrue(exposure.getNote().contains("not the fund's published country exposure"));
    }

    @Test
    void missingSymbolIsRejected() {

        ToolRequest request =
                new ToolRequest(
                        1,
                        ActionType
                                .ETF_SECTOR_EXPOSURE,

                        Map.of()
                );

        ToolResult result =
                tool.execute(
                        request
                );

        assertFalse(
                result.isSuccess()
        );
    }

    private static class FakeETFDataProvider
            implements ETFDataProvider {

        @Override
        public ETFProfile getProfile(
                String symbol) {

            return new ETFProfile(
                    symbol,
                    new BigDecimal(
                            "100000000"
                    ),
                    new BigDecimal(
                            "0.002"
                    ),

                    List.of(
                            new ETFHolding(
                                    "NVDA",
                                    "NVIDIA",
                                    new BigDecimal(
                                            "0.10"
                                    )
                            )
                    ),

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
        }
    }
}