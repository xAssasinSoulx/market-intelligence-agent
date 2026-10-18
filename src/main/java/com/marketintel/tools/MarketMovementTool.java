
package com.marketintel.tools;

import com.marketintel.model.ActionType;
import com.marketintel.model.MarketMovementContext;
import com.marketintel.model.MarketQuote;
import com.marketintel.model.NewsArticle;
import com.marketintel.model.ToolRequest;
import com.marketintel.model.ToolResult;
import com.marketintel.model.ValidationResult;

import com.marketintel.providers.MarketDataException;
import com.marketintel.providers.MarketDataProvider;
import com.marketintel.providers.NewsProvider;

import com.marketintel.services.RequestValidator;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class MarketMovementTool implements AgentTool {

    private static final int NEWS_LIMIT = 3;

    private final MarketDataProvider marketDataProvider;
    private final NewsProvider newsProvider;
    private final RequestValidator validator;
    private final Clock clock;

    public MarketMovementTool(
            MarketDataProvider marketDataProvider,
            NewsProvider newsProvider,
            RequestValidator validator) {

        this(
                marketDataProvider,
                newsProvider,
                validator,
                Clock.systemUTC()
        );
    }

    // Clock injection makes time-dependent behavior
    // deterministic during JUnit testing.
    MarketMovementTool(
            MarketDataProvider marketDataProvider,
            NewsProvider newsProvider,
            RequestValidator validator,
            Clock clock) {

        this.marketDataProvider = Objects.requireNonNull(
                marketDataProvider,
                "Market data provider cannot be null."
        );

        this.newsProvider = Objects.requireNonNull(
                newsProvider,
                "News provider cannot be null."
        );

        this.validator = Objects.requireNonNull(
                validator,
                "Request validator cannot be null."
        );

        this.clock = Objects.requireNonNull(
                clock,
                "Clock cannot be null."
        );
    }

    @Override
    public String getName() {
        return "market-movement";
    }

    @Override
    public boolean supports(ActionType action) {
        return action == ActionType.MARKET_MOVEMENT;
    }

    @Override
    public ToolResult execute(ToolRequest request) {

        ValidationResult requestValidation =
                validator.validateToolRequest(request);

        if (!requestValidation.isValid()) {
            return ToolResult.failure(
                    requestValidation.getErrorMessage()
            );
        }

        if (!supports(request.getAction())) {
            return ToolResult.failure(
                    "Unsupported market movement action: "
                            + request.getAction()
            );
        }

        String symbol = request.getArgument("symbol");

        ValidationResult symbolValidation =
                validator.validateSymbol(symbol);

        if (!symbolValidation.isValid()) {
            return ToolResult.failure(
                    symbolValidation.getErrorMessage()
            );
        }

        String normalizedSymbol =
                symbol.trim().toUpperCase(Locale.ROOT);

        MarketQuote quote;

        // A price quote is essential for this operation.
        try {
            quote = marketDataProvider.getQuote(
                    normalizedSymbol
            );

            if (quote == null) {
                return ToolResult.failure(
                        "No market quote was returned for "
                                + normalizedSymbol + "."
                );
            }

        } catch (MarketDataException
                 | IllegalArgumentException e) {

            return ToolResult.failure(
                    "Unable to retrieve market quote: "
                            + e.getMessage()
            );
        }

        List<NewsArticle> articles = List.of();
        String newsWarning = null;

        // News is supplementary. If it fails,
        // preserve the quote and report the limitation.
        try {
            List<NewsArticle> retrieved =
                    newsProvider.getNews(
                            normalizedSymbol,
                            NEWS_LIMIT
                    );

            if (retrieved == null) {
                newsWarning =
                        "News provider returned no result list.";
            } else {
                articles = List.copyOf(retrieved);
            }

        } catch (MarketDataException
                 | IllegalArgumentException e) {

            newsWarning =
                    "Recent news could not be retrieved: "
                            + e.getMessage();
        }

        MarketMovementContext context =
                new MarketMovementContext(
                        quote,
                        articles,
                        clock.instant(),
                        newsWarning
                );

        return ToolResult.success(context);
    }
}
