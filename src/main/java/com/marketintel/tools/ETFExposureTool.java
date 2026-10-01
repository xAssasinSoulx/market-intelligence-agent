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

public class ETFExposureTool
        implements AgentTool {

    private final ETFDataProvider provider;
    private final ExposureCalculator calculator;
    private final RequestValidator validator;

    public ETFExposureTool(
            ETFDataProvider provider,
            ExposureCalculator calculator,
            RequestValidator validator) {

        if (provider == null) {
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

        this.provider = provider;
        this.calculator = calculator;
        this.validator = validator;
    }

    @Override
    public String getName() {
        return "etf-exposure";
    }

    @Override
    public boolean supports(
            ActionType action) {

        return action
                == ActionType.ETF_SECTOR_EXPOSURE;
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
                    "Unsupported ETF exposure action: "
                            + request.getAction()
            );
        }

        String symbol =
                request.getArgument(
                        "symbol"
                );

        ValidationResult symbolValidation =
                validator.validateSymbol(
                        symbol
                );

        if (!symbolValidation.isValid()) {

            return ToolResult.failure(
                    symbolValidation
                            .getErrorMessage()
            );
        }

        String normalizedSymbol =
                symbol.trim()
                        .toUpperCase();

        try {

            ETFProfile profile =
                    provider.getProfile(
                            normalizedSymbol
                    );

            return ToolResult.success(
                    calculator
                            .calculateSectorExposure(
                                    profile
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