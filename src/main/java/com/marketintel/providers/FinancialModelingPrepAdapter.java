package com.marketintel.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketintel.model.FinancialStatements;
import com.marketintel.model.ReportingPeriod;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for Financial Modeling Prep's stable statement endpoints.
 *
 * The free plan is the statement source. Published limit is about 250
 * calls a day; confirm it at signup. One statements request uses three
 * calls: income, balance sheet, and cash flow. ETF holdings are not
 * requested, because that dataset is not part of the free plan this
 * adapter relies on.
 */
public final class FinancialModelingPrepAdapter implements DataProvider {

    static final String SOURCE = "Financial Modeling Prep";
    private static final String BASE_URL = "https://financialmodelingprep.com/stable";
    private static final int ANNUAL_LIMIT = 4;

    private final String apiKey;
    private final JsonTransport transport;

    public FinancialModelingPrepAdapter(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Financial Modeling Prep API key cannot be blank."
            );
        }
        this.apiKey = apiKey.trim();
        this.transport = new JsonTransport();
    }

    @Override
    public String providerName() {
        return SOURCE;
    }

    @Override
    public boolean supports(DataCapability capability) {
        return capability == DataCapability.FINANCIAL_STATEMENTS;
    }

    @Override
    public FinancialStatements getFinancialStatements(String symbol) {
        String normalized = Symbols.require(symbol);
        JsonNode income = ok(get("income-statement", normalized), "income statement");
        JsonNode balance = ok(get("balance-sheet-statement", normalized), "balance sheet");
        JsonNode cashFlow = ok(get("cash-flow-statement", normalized), "cash-flow statement");
        return parseStatements(normalized, income, balance, cashFlow);
    }

    FinancialStatements parseStatements(
            String symbol,
            JsonNode income,
            JsonNode balance,
            JsonNode cashFlow) {

        Map<String, Draft> drafts = new LinkedHashMap<>();
        String currency = "";
        currency = readIncome(symbol, income, drafts, currency);
        currency = readBalance(symbol, balance, drafts, currency);
        currency = readCashFlow(symbol, cashFlow, drafts, currency);

        if (drafts.isEmpty()) {
            throw new MarketDataException(
                    "No financial statements found for symbol: " + symbol
            );
        }

        List<ReportingPeriod> periods = new ArrayList<>();
        for (Draft draft : drafts.values()) {
            periods.add(draft.build());
        }
        return new FinancialStatements(
                symbol,
                SOURCE,
                currency,
                "Annual figures from Financial Modeling Prep. "
                        + "A missing line was absent from that statement and was not stored as zero. "
                        + "This request uses three API calls.",
                periods
        );
    }

    private JsonTransport.JsonResponse get(String path, String symbol) {
        String query = "symbol=" + encode(symbol)
                + "&period=annual&limit=" + ANNUAL_LIMIT
                + "&apikey=" + encode(apiKey);
        return transport.get(
                URI.create(BASE_URL + "/" + path + "?" + query),
                Map.of(),
                SOURCE
        );
    }

    private JsonNode ok(JsonTransport.JsonResponse response, String document) {
        String detail = ProviderText.redact(JsonValues.errorMessage(response.json()), apiKey);
        if (response.status() == 401 || response.status() == 403) {
            throw new MarketDataException(
                    "Financial Modeling Prep refused the " + document
                            + " (HTTP " + response.status() + ")."
                            + suffix(detail)
            );
        }
        if (response.status() == 429) {
            throw new MarketDataException(
                    "Financial Modeling Prep rate limit reached. "
                            + "The free plan allows about 250 calls a day. "
                            + "Confirm the current limit when you sign up."
                            + suffix(detail)
            );
        }
        if (response.status() < 200 || response.status() >= 300) {
            throw new MarketDataException(
                    "Financial Modeling Prep returned HTTP " + response.status()
                            + " for the " + document + "."
                            + suffix(detail)
            );
        }
        JsonNode json = response.json();
        if (json == null) {
            throw new MarketDataException(
                    "Financial Modeling Prep returned an unreadable " + document + "."
            );
        }
        if (json.isObject() && !detail.isBlank()) {
            throw new MarketDataException(
                    "Financial Modeling Prep returned an error for the "
                            + document + ": " + detail
            );
        }
        if (!json.isArray()) {
            throw new MarketDataException(
                    "Financial Modeling Prep returned an unreadable " + document + "."
            );
        }
        return json;
    }

    private static String readIncome(
            String symbol,
            JsonNode rows,
            Map<String, Draft> drafts,
            String currency) {

        requireArray(symbol, rows, "income statement");
        for (JsonNode row : rows) {
            String date = JsonValues.text(row, "date");
            if (date.isBlank()) {
                continue;
            }
            Draft draft = drafts.computeIfAbsent(date, Draft::new);
            draft.periodLabel = firstText(draft.periodLabel, JsonValues.text(row, "period"));
            draft.revenue = JsonValues.decimal(row, "revenue");
            draft.grossProfit = JsonValues.decimal(row, "grossProfit");
            draft.operatingIncome = JsonValues.decimal(row, "operatingIncome");
            draft.netIncome = JsonValues.decimal(row, "netIncome");
            draft.dilutedEps = firstDecimal(
                    JsonValues.decimal(row, "epsDiluted"),
                    JsonValues.decimal(row, "eps")
            );
            currency = firstText(currency, JsonValues.text(row, "reportedCurrency"));
        }
        return currency;
    }

    private static String readBalance(
            String symbol,
            JsonNode rows,
            Map<String, Draft> drafts,
            String currency) {

        requireArray(symbol, rows, "balance sheet");
        for (JsonNode row : rows) {
            String date = JsonValues.text(row, "date");
            if (date.isBlank()) {
                continue;
            }
            Draft draft = drafts.computeIfAbsent(date, Draft::new);
            draft.periodLabel = firstText(draft.periodLabel, JsonValues.text(row, "period"));
            draft.totalAssets = JsonValues.decimal(row, "totalAssets");
            draft.totalLiabilities = JsonValues.decimal(row, "totalLiabilities");
            draft.totalEquity = firstDecimal(
                    JsonValues.decimal(row, "totalStockholdersEquity"),
                    JsonValues.decimal(row, "totalEquity")
            );
            draft.cash = firstDecimal(
                    JsonValues.decimal(row, "cashAndCashEquivalents"),
                    JsonValues.decimal(row, "cash")
            );
            currency = firstText(currency, JsonValues.text(row, "reportedCurrency"));
        }
        return currency;
    }

    private static String readCashFlow(
            String symbol,
            JsonNode rows,
            Map<String, Draft> drafts,
            String currency) {

        requireArray(symbol, rows, "cash-flow statement");
        for (JsonNode row : rows) {
            String date = JsonValues.text(row, "date");
            if (date.isBlank()) {
                continue;
            }
            Draft draft = drafts.computeIfAbsent(date, Draft::new);
            draft.periodLabel = firstText(draft.periodLabel, JsonValues.text(row, "period"));
            draft.operatingCashFlow = firstDecimal(
                    JsonValues.decimal(row, "operatingCashFlow"),
                    JsonValues.decimal(row, "netCashProvidedByOperatingActivities")
            );
            draft.freeCashFlow = JsonValues.decimal(row, "freeCashFlow");
            currency = firstText(currency, JsonValues.text(row, "reportedCurrency"));
        }
        return currency;
    }

    private static void requireArray(String symbol, JsonNode rows, String document) {
        if (rows == null || !rows.isArray()) {
            throw new MarketDataException(
                    "Financial Modeling Prep returned an unreadable "
                            + document + " for " + symbol + "."
            );
        }
    }

    private static String firstText(String current, String candidate) {
        if (current != null && !current.isBlank()) {
            return current;
        }
        return candidate == null ? "" : candidate;
    }

    private static BigDecimal firstDecimal(BigDecimal preferred, BigDecimal fallback) {
        return preferred != null ? preferred : fallback;
    }

    private static String suffix(String detail) {
        if (detail == null || detail.isBlank()) {
            return "";
        }
        return " " + detail;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static final class Draft {
        private final String date;
        private String periodLabel = "";
        private BigDecimal revenue;
        private BigDecimal grossProfit;
        private BigDecimal operatingIncome;
        private BigDecimal netIncome;
        private BigDecimal dilutedEps;
        private BigDecimal totalAssets;
        private BigDecimal totalLiabilities;
        private BigDecimal totalEquity;
        private BigDecimal cash;
        private BigDecimal operatingCashFlow;
        private BigDecimal freeCashFlow;

        private Draft(String date) {
            this.date = date;
        }

        private ReportingPeriod build() {
            return ReportingPeriod.builder(date)
                    .periodLabel(periodLabel)
                    .revenue(revenue)
                    .grossProfit(grossProfit)
                    .operatingIncome(operatingIncome)
                    .netIncome(netIncome)
                    .dilutedEps(dilutedEps)
                    .totalAssets(totalAssets)
                    .totalLiabilities(totalLiabilities)
                    .totalEquity(totalEquity)
                    .cash(cash)
                    .operatingCashFlow(operatingCashFlow)
                    .freeCashFlow(freeCashFlow)
                    .build();
        }
    }
}
