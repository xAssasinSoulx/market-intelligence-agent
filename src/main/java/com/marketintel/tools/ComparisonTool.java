package com.marketintel.tools;

import com.marketintel.model.ActionType;
import com.marketintel.model.ETFProfile;
import com.marketintel.model.MarketQuote;
import com.marketintel.model.ToolRequest;
import com.marketintel.model.ToolResult;
import com.marketintel.model.ValidationResult;

import com.marketintel.providers.ETFDataProvider;
import com.marketintel.providers.MarketDataException;
import com.marketintel.providers.MarketDataProvider;

import com.marketintel.services.ExposureCalculator;
import com.marketintel.services.RequestValidator;
import com.marketintel.services.SecurityComparisonCalculator;

public class ComparisonTool
        implements AgentTool {

    private final ETFDataProvider etfDataProvider;

    private final MarketDataProvider marketDataProvider;

    private final ExposureCalculator exposureCalculator;

    private final SecurityComparisonCalculator
            securityComparisonCalculator;

    private final RequestValidator validator;

    public ComparisonTool(
            ETFDataProvider etfDataProvider,
            MarketDataProvider marketDataProvider,
            ExposureCalculator exposureCalculator,
            SecurityComparisonCalculator
                    securityComparisonCalculator,
            RequestValidator validator) {

        if (etfDataProvider == null) {

            throw new IllegalArgumentException(
                    "ETF data provider cannot be null."
            );
        }

        if (marketDataProvider == null) {

            throw new IllegalArgumentException(
                    "Market data provider cannot be null."
            );
        }

        if (exposureCalculator == null) {

            throw new IllegalArgumentException(
                    "Exposure calculator cannot be null."
            );
        }

        if (securityComparisonCalculator == null) {

            throw new IllegalArgumentException(
                    "Security comparison calculator "
                            + "cannot be null."
            );
        }

        if (validator == null) {

            throw new IllegalArgumentException(
                    "Request validator cannot be null."
            );
        }

        this.etfDataProvider =
                etfDataProvider;

        this.marketDataProvider =
                marketDataProvider;

        this.exposureCalculator =
                exposureCalculator;

        this.securityComparisonCalculator =
                securityComparisonCalculator;

        this.validator =
                validator;
    }

    @Override
    public String getName() {
        return "comparison";
    }

    @Override
    public boolean supports(
            ActionType action) {

        return action
                == ActionType.SECURITY_COMPARISON
                || action
                == ActionType.ETF_OVERLAP;
    }

    @Override
    public ToolResult execute(
            ToolRequest request) {

        ValidationResult requestValidation =
                validator.validateToolRequest(
                        request
                );

        if (!requestValidation.isValid()) {

            return ToolResult.failure(
                    requestValidation
                            .getErrorMessage()
            );
        }

        return switch (
                request.getAction()) {

            case SECURITY_COMPARISON ->
                    executeSecurityComparison(
                            request
                    );

            case ETF_OVERLAP ->
                    executeEtfOverlap(
                            request
                    );

            default ->
                    ToolResult.failure(
                            "Unsupported comparison action: "
                                    + request.getAction()
                    );
        };
    }

    private ToolResult executeSecurityComparison(
            ToolRequest request) {

        String firstSymbol =
                request.getArgument(
                        "firstSymbol"
                );

        String secondSymbol =
                request.getArgument(
                        "secondSymbol"
                );

        ValidationResult validation =
                validateSymbols(
                        firstSymbol,
                        secondSymbol
                );

        if (!validation.isValid()) {

            return ToolResult.failure(
                    validation.getErrorMessage()
            );
        }

        String normalizedFirst =
                normalizeSymbol(
                        firstSymbol
                );

        String normalizedSecond =
                normalizeSymbol(
                        secondSymbol
                );

        if (normalizedFirst.equals(
                normalizedSecond)) {

            return ToolResult.failure(
                    "Please provide two different "
                            + "symbols to compare."
            );
        }

        try {

            MarketQuote firstQuote =
                    marketDataProvider
                            .getQuote(
                                    normalizedFirst
                            );

            MarketQuote secondQuote =
                    marketDataProvider
                            .getQuote(
                                    normalizedSecond
                            );

            return ToolResult.success(
                    securityComparisonCalculator
                            .compare(
                                    firstQuote,
                                    secondQuote
                            )
            );

        } catch (MarketDataException e) {

            return ToolResult.failure(
                    e.getMessage()
            );

        } catch (IllegalArgumentException e) {

            return ToolResult.failure(
                    e.getMessage()
            );
        }
    }

    private ToolResult executeEtfOverlap(
            ToolRequest request) {

        String firstSymbol =
                request.getArgument(
                        "firstSymbol"
                );

        String secondSymbol =
                request.getArgument(
                        "secondSymbol"
                );

        ValidationResult validation =
                validateSymbols(
                        firstSymbol,
                        secondSymbol
                );

        if (!validation.isValid()) {

            return ToolResult.failure(
                    validation.getErrorMessage()
            );
        }

        String normalizedFirst =
                normalizeSymbol(
                        firstSymbol
                );

        String normalizedSecond =
                normalizeSymbol(
                        secondSymbol
                );

        if (normalizedFirst.equals(
                normalizedSecond)) {

            return ToolResult.failure(
                    "Please provide two different "
                            + "ETF symbols."
            );
        }

        try {

            ETFProfile firstProfile =
                    etfDataProvider
                            .getProfile(
                                    normalizedFirst
                            );

            ETFProfile secondProfile =
                    etfDataProvider
                            .getProfile(
                                    normalizedSecond
                            );

            return ToolResult.success(
                    exposureCalculator
                            .calculateOverlap(
                                    firstProfile,
                                    secondProfile
                            )
            );

        } catch (MarketDataException e) {

            return ToolResult.failure(
                    e.getMessage()
            );

        } catch (IllegalArgumentException e) {

            return ToolResult.failure(
                    e.getMessage()
            );
        }
    }

    private ValidationResult validateSymbols(
            String firstSymbol,
            String secondSymbol) {

        ValidationResult firstValidation =
                validator.validateSymbol(
                        firstSymbol
                );

        if (!firstValidation.isValid()) {

            return firstValidation;
        }

        ValidationResult secondValidation =
                validator.validateSymbol(
                        secondSymbol
                );

        if (!secondValidation.isValid()) {

            return secondValidation;
        }

        return ValidationResult.valid();
    }

    private String normalizeSymbol(
            String symbol) {

        return symbol
                .trim()
                .toUpperCase();
    }
}