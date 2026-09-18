package com.marketintel.tools;

import com.marketintel.model.ActionType;
import com.marketintel.model.ToolRequest;
import com.marketintel.model.ToolResult;
import com.marketintel.model.ValidationResult;

import com.marketintel.providers.MarketDataException;
import com.marketintel.providers.MarketDataProvider;

import com.marketintel.services.RequestValidator;

public class MarketDataTool
        implements AgentTool {

    private final MarketDataProvider provider;
    private final RequestValidator validator;

    public MarketDataTool(
            MarketDataProvider provider,
            RequestValidator validator) {

        this.provider = provider;
        this.validator = validator;
    }

    @Override
    public String getName() {
        return "market-data";
    }

    @Override
    public boolean supports(ActionType action) {

        return action == ActionType.MARKET_QUOTE
                || action
                == ActionType.SECURITY_OVERVIEW;
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
                    "Unsupported market-data action: "
                            + request.getAction()
            );
        }

        String symbol =
                request.getArgument("symbol");

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
                symbol.trim().toUpperCase();

        try {

            return switch (
                    request.getAction()
                    ) {

                case MARKET_QUOTE ->
                        ToolResult.success(
                                provider.getQuote(
                                        normalizedSymbol
                                )
                        );

                case SECURITY_OVERVIEW ->
                        ToolResult.success(
                                provider
                                        .getSecurityOverview(
                                                normalizedSymbol
                                        )
                        );

                default ->
                        ToolResult.failure(
                                "Unsupported market-data action."
                        );
            };

        } catch (MarketDataException e) {

            return ToolResult.failure(
                    e.getMessage()
            );
        }
    }
}