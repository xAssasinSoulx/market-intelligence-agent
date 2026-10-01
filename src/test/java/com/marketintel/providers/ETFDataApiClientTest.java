package com.marketintel.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.marketintel.model.ETFProfile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ETFDataApiClientTest {

    @Test
    void parsesEtfProfileResponse()
            throws Exception {

        String json = """
                {
                  "net_assets": "300000000000",
                  "net_expense_ratio": "0.0020",
                  "sectors": [
                    {
                      "sector": "Technology",
                      "weight": "0.60"
                    },
                    {
                      "sector": "Communication Services",
                      "weight": "0.40"
                    }
                  ],
                  "holdings": [
                    {
                      "symbol": "NVDA",
                      "description": "NVIDIA Corporation",
                      "weight": "0.10"
                    },
                    {
                      "symbol": "MSFT",
                      "description": "Microsoft Corporation",
                      "weight": "0.08"
                    }
                  ]
                }
                """;

        ObjectMapper objectMapper =
                new ObjectMapper();

        JsonNode root =
                objectMapper.readTree(
                        json
                );

        AlphaVantageClient
                alphaVantageClient =
                new AlphaVantageClient(
                        "dummy-key"
                );

        ETFDataApiClient client =
                new ETFDataApiClient(
                        alphaVantageClient
                );

        ETFProfile profile =
                client.parseProfile(
                        "QQQ",
                        root
                );

        assertEquals(
                "QQQ",
                profile.getSymbol()
        );

        assertEquals(
                new BigDecimal(
                        "300000000000"
                ),
                profile.getNetAssets()
        );

        assertEquals(
                new BigDecimal(
                        "0.0020"
                ),
                profile.getExpenseRatio()
        );

        assertEquals(
                2,
                profile.getHoldings()
                        .size()
        );

        assertEquals(
                "NVDA",
                profile.getHoldings()
                        .getFirst()
                        .getSymbol()
        );

        assertEquals(
                new BigDecimal("0.10"),
                profile.getHoldings()
                        .getFirst()
                        .getWeight()
        );

        assertEquals(
                new BigDecimal("0.60"),
                profile.getSectorWeights()
                        .get(
                                "Technology"
                        )
        );
    }

    @Test
    void normalizesPercentageStyleWeights()
            throws Exception {

        String json = """
                {
                  "sectors": [
                    {
                      "sector": "Technology",
                      "weight": "60"
                    },
                    {
                      "sector": "Financials",
                      "weight": "40"
                    }
                  ],
                  "holdings": [
                    {
                      "symbol": "NVDA",
                      "description": "NVIDIA",
                      "weight": "10"
                    },
                    {
                      "symbol": "MSFT",
                      "description": "Microsoft",
                      "weight": "8"
                    }
                  ]
                }
                """;

        ObjectMapper objectMapper =
                new ObjectMapper();

        JsonNode root =
                objectMapper.readTree(
                        json
                );

        ETFDataApiClient client =
                new ETFDataApiClient(
                        new AlphaVantageClient(
                                "dummy-key"
                        )
                );

        ETFProfile profile =
                client.parseProfile(
                        "TEST",
                        root
                );

        assertEquals(
                new BigDecimal("0.60"),
                profile.getSectorWeights()
                        .get(
                                "Technology"
                        )
        );

        assertEquals(
                new BigDecimal("0.10"),
                profile.getHoldings()
                        .getFirst()
                        .getWeight()
        );
    }
}