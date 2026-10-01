package com.marketintel.tools;

import com.marketintel.model.ActionType;
import com.marketintel.model.ETFHolding;
import com.marketintel.model.ETFProfile;
import com.marketintel.model.ExposureResult;
import com.marketintel.model.ToolRequest;
import com.marketintel.model.ToolResult;

import com.marketintel.providers.ETFDataProvider;

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