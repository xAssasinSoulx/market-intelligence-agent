package com.marketintel.providers;

import com.marketintel.model.MarketQuote;
import com.marketintel.model.SecurityOverview;

public class MarketDataManualTest {

    public static void main(String[] args) {

        String apiKey =
                System.getenv("ALPHA_VANTAGE_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            System.err.println(
                    "ALPHA_VANTAGE_API_KEY is not configured."
            );
            return;
        }

        MarketDataProvider provider =
                new MarketDataApiClient(apiKey);

        String mode =
                args.length > 0
                        ? args[0].toLowerCase()
                        : "quote";

        try {

            switch (mode) {

                case "quote" ->
                        testQuote(provider);

                case "overview" ->
                        testOverview(provider);

                default ->
                        System.err.println(
                                "Usage: quote | overview"
                        );
            }

        } catch (MarketDataException e) {

            System.err.println(
                    "Market-data request failed:"
            );

            System.err.println(
                    e.getMessage()
            );
        }
    }

    private static void testQuote(
            MarketDataProvider provider) {

        MarketQuote quote =
                provider.getQuote("NVDA");

        System.out.println(
                "Symbol: "
                        + quote.getSymbol()
        );

        System.out.println(
                "Price: "
                        + quote.getPrice()
        );

        System.out.println(
                "Previous Close: "
                        + quote.getPreviousClose()
        );

        System.out.println(
                "Change: "
                        + quote.getPercentChange()
                        + "%"
        );
    }

    private static void testOverview(
            MarketDataProvider provider) {

        SecurityOverview overview =
                provider.getSecurityOverview(
                        "NVDA"
                );

        System.out.println(
                "Symbol: "
                        + overview.getSymbol()
        );

        System.out.println(
                "Name: "
                        + overview.getName()
        );

        System.out.println(
                "Exchange: "
                        + overview.getExchange()
        );

        System.out.println(
                "Currency: "
                        + overview.getCurrency()
        );

        System.out.println(
                "Sector: "
                        + overview.getSector()
        );

        System.out.println(
                "Industry: "
                        + overview.getIndustry()
        );

        System.out.println(
                "Market Cap: "
                        + overview.getMarketCapitalization()
        );
    }
}