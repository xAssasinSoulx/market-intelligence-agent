package com.marketintel.tools;

import com.marketintel.model.ActionType;
import com.marketintel.model.ETFProfile;
import com.marketintel.model.ToolRequest;
import com.marketintel.model.ToolResult;
import com.marketintel.model.ValidationResult;

import com.marketintel.providers.ETFDataProvider;
import com.marketintel.providers.MarketDataException;

import com.marketintel.services.ExposureCalculator;
import com.marketintel.services.RequestValidator;

public class ComparisonTool
        implements AgentTool {

    private final ETFDataProvider etfDataProvider;
    private final ExposureCalculator calculator;
    private final RequestValidator validator;

    public ComparisonTool(
            ETFDataProvider etfDataProvider,
            ExposureCalculator calculator,
            RequestValidator validator) {

        if (etfDataProvider == null) {

            throw new IllegalArgumentException(
                    "ETF data provider cannot be null."
            );
        }

        if (calculator == null) {

            throw new IllegalArgumentException(
                    "Exposure calculator cannot be null."
            );
        }

        if (validator == null) {

            throw new IllegalArgumentException(
                    "Request validator cannot be null."
            );
        }

        this.etfDataProvider =
                etfDataProvider;

        this.calculator =
                calculator;

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

        if (!supports(request.getAction())) {

            return ToolResult.failure(
                    "Unsupported comparison action: "
                            + request.getAction()
            );
        }

        String firstSymbol =
                request.getArgument(
                        "firstSymbol"
                );

        String secondSymbol =
                request.getArgument(
                        "secondSymbol"
                );

        ValidationResult firstValidation =
                validator.validateSymbol(
                        firstSymbol
                );

        if (!firstValidation.isValid()) {

            return ToolResult.failure(
                    firstValidation
                            .getErrorMessage()
            );
        }

        ValidationResult secondValidation =
                validator.validateSymbol(
                        secondSymbol
                );

        if (!secondValidation.isValid()) {

            return ToolResult.failure(
                    secondValidation
                            .getErrorMessage()
            );
        }

        String normalizedFirst =
                firstSymbol
                        .trim()
                        .toUpperCase();

        String normalizedSecond =
                secondSymbol
                        .trim()
                        .toUpperCase();

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
                    calculator.calculateOverlap(
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
}