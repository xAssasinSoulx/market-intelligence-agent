package com.marketintel.providers;

import com.marketintel.model.ETFHolding;
import com.marketintel.model.ETFProfile;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.Comparator;
import java.util.Map;

public class ETFDataManualTest {

    public static void main(
            String[] args) {

        String apiKey =
                System.getenv(
                        "ALPHA_VANTAGE_API_KEY"
                );

        if (apiKey == null
                || apiKey.isBlank()) {

            System.err.println(
                    "ALPHA_VANTAGE_API_KEY "
                            + "is not configured."
            );

            return;
        }

        String symbol =
                args.length > 0
                        ? args[0]
                        : "QQQ";

        try {

            AlphaVantageClient
                    alphaVantageClient =
                    new AlphaVantageClient(
                            apiKey
                    );

            ETFDataProvider provider =
                    new ETFDataApiClient(
                            alphaVantageClient
                    );

            ETFProfile profile =
                    provider.getProfile(
                            symbol
                    );

            System.out.println();
            System.out.println(
                    "ETF PROFILE"
            );

            System.out.println(
                    "------------------------------------------------------------"
            );

            System.out.println(
                    "Symbol        : "
                            + profile.getSymbol()
            );

            System.out.println(
                    "Net Assets    : "
                            + profile.getNetAssets()
            );

            System.out.println(
                    "Expense Ratio : "
                            + profile.getExpenseRatio()
            );

            System.out.println();

            System.out.println(
                    "SECTOR EXPOSURE"
            );

            System.out.println(
                    "------------------------------------------------------------"
            );

            profile.getSectorWeights()
                    .entrySet()
                    .stream()

                    .sorted(
                            Map.Entry
                                    .<String, BigDecimal>
                                            comparingByValue()
                                    .reversed()
                    )

                    .forEach(
                            entry -> {

                                BigDecimal percent =
                                        entry
                                                .getValue()
                                                .multiply(
                                                        new BigDecimal(
                                                                "100"
                                                        )
                                                )
                                                .setScale(
                                                        2,
                                                        RoundingMode.HALF_UP
                                                );

                                System.out.printf(
                                        "%-30s %s%%%n",
                                        entry.getKey(),
                                        percent
                                );
                            }
                    );

            System.out.println();

            System.out.println(
                    "TOP HOLDINGS"
            );

            System.out.println(
                    "------------------------------------------------------------"
            );

            profile.getHoldings()
                    .stream()

                    .sorted(
                            Comparator
                                    .comparing(
                                            ETFHolding::getWeight
                                    )
                                    .reversed()
                    )

                    .limit(10)

                    .forEach(
                            holding -> {

                                BigDecimal percent =
                                        holding
                                                .getWeight()
                                                .multiply(
                                                        new BigDecimal(
                                                                "100"
                                                        )
                                                )
                                                .setScale(
                                                        2,
                                                        RoundingMode.HALF_UP
                                                );

                                System.out.printf(
                                        "%-10s %-35s %s%%%n",
                                        holding.getSymbol(),
                                        holding.getDescription(),
                                        percent
                                );
                            }
                    );

        } catch (MarketDataException e) {

            System.err.println();

            System.err.println(
                    "ETF request failed:"
            );

            System.err.println(
                    e.getMessage()
            );
        }
    }
}