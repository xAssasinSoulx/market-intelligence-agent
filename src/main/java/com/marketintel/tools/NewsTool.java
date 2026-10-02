package com.marketintel.tools;

import com.marketintel.model.ActionType;
import com.marketintel.model.NewsArticle;
import com.marketintel.model.NewsResult;
import com.marketintel.model.ToolRequest;
import com.marketintel.model.ToolResult;
import com.marketintel.model.ValidationResult;

import com.marketintel.providers.MarketDataException;
import com.marketintel.providers.NewsProvider;

import com.marketintel.services.RequestValidator;

import java.util.List;

public class NewsTool
        implements AgentTool {

    private static final int DEFAULT_LIMIT =
            5;

    private final NewsProvider newsProvider;

    private final RequestValidator validator;

    public NewsTool(
            NewsProvider newsProvider,
            RequestValidator validator) {

        if (newsProvider == null) {

            throw new IllegalArgumentException(
                    "News provider cannot be null."
            );
        }

        if (validator == null) {

            throw new IllegalArgumentException(
                    "Request validator cannot be null."
            );
        }

        this.newsProvider =
                newsProvider;

        this.validator =
                validator;
    }

    @Override
    public String getName() {
        return "financial-news";
    }

    @Override
    public boolean supports(
            ActionType action) {

        return action
                == ActionType.FINANCIAL_NEWS;
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

        if (!supports(
                request.getAction())) {

            return ToolResult.failure(
                    "Unsupported news action: "
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

            List<NewsArticle> articles =
                    newsProvider.getNews(
                            normalizedSymbol,
                            DEFAULT_LIMIT
                    );

            return ToolResult.success(
                    new NewsResult(
                            normalizedSymbol,
                            articles
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