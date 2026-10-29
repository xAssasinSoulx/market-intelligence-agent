package com.marketintel.tools;

import com.marketintel.model.ActionType;
import com.marketintel.model.AssetType;
import com.marketintel.model.ETFHolding;
import com.marketintel.model.ETFProfile;
import com.marketintel.model.MarketQuote;
import com.marketintel.model.Portfolio;
import com.marketintel.model.PortfolioExposureInput;
import com.marketintel.model.PortfolioPosition;
import com.marketintel.model.SecurityOverview;
import com.marketintel.model.ToolRequest;
import com.marketintel.model.ToolResult;
import com.marketintel.model.ValidationResult;
import com.marketintel.providers.ETFDataProvider;
import com.marketintel.providers.MarketDataException;
import com.marketintel.providers.MarketDataProvider;
import com.marketintel.services.PortfolioExposureCalculator;
import com.marketintel.services.PortfolioService;
import com.marketintel.services.RequestValidator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Values the signed-in user's positions and asks the calculator for
 * sector weights. Provider failures are recorded, not replaced with
 * made-up allocations.
 */
public final class PortfolioAnalysisTool implements AgentTool {

    private final PortfolioService portfolioService;
    private final MarketDataProvider marketDataProvider;
    private final ETFDataProvider etfDataProvider;
    private final PortfolioExposureCalculator calculator;
    private final RequestValidator validator;

    public PortfolioAnalysisTool(
            PortfolioService portfolioService,
            MarketDataProvider marketDataProvider,
            ETFDataProvider etfDataProvider,
            PortfolioExposureCalculator calculator,
            RequestValidator validator) {

        if (portfolioService == null
                || marketDataProvider == null
                || etfDataProvider == null
                || calculator == null
                || validator == null) {
            throw new IllegalArgumentException("Portfolio analysis dependencies cannot be null.");
        }

        this.portfolioService = portfolioService;
        this.marketDataProvider = marketDataProvider;
        this.etfDataProvider = etfDataProvider;
        this.calculator = calculator;
        this.validator = validator;
    }

    @Override
    public String getName() {
        return "portfolio-analysis";
    }

    @Override
    public boolean supports(ActionType action) {
        return action == ActionType.PORTFOLIO_EXPOSURE;
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        ValidationResult validation = validator.validateToolRequest(request);
        if (!validation.isValid()) {
            return ToolResult.failure(validation.getErrorMessage());
        }
        if (!supports(request.getAction())) {
            return ToolResult.failure(
                    "Unsupported portfolio action: " + request.getAction()
            );
        }

        Portfolio portfolio = portfolioService.getPortfolio(request.getUserId());
        List<PortfolioExposureInput> inputs = new ArrayList<>();
        for (PortfolioPosition position : portfolio.getPositions()) {
            inputs.add(load(position));
        }
        return ToolResult.success(calculator.calculate(inputs));
    }

    private PortfolioExposureInput load(PortfolioPosition position) {
        BigDecimal price = null;
        String currency = null;
        String note = null;
        Map<String, BigDecimal> sectors = Map.of();
        List<ETFHolding> holdings = List.of();

        try {
            MarketQuote quote = marketDataProvider.getQuote(position.getSymbol());
            price = quote.getPrice();
        } catch (MarketDataException exception) {
            note = exception.getMessage();
        }

        try {
            SecurityOverview overview = marketDataProvider.getSecurityOverview(position.getSymbol());
            currency = overview.getCurrency();
            if (position.getAssetType() != AssetType.ETF
                    && overview.getSector() != null
                    && !overview.getSector().isBlank()) {
                sectors = Map.of(overview.getSector().trim(), BigDecimal.ONE);
            }
        } catch (MarketDataException exception) {
            if (position.getAssetType() == AssetType.ETF
                    && price != null
                    && marketDataProvider.quoteCurrency().isPresent()) {
                currency = marketDataProvider.quoteCurrency().get();
                note = join(note, position.getSymbol()
                        + " has no company profile, as is usual for an ETF. Its currency is "
                        + currency + " because the quote provider serves only that market.");
            } else {
                note = join(note, exception.getMessage());
            }
        }

        if (position.getAssetType() == AssetType.ETF) {
            try {
                ETFProfile profile = etfDataProvider.getProfile(position.getSymbol());
                sectors = profile.getSectorWeights();
                holdings = profile.getHoldings();
            } catch (MarketDataException | IllegalArgumentException exception) {
                note = join(note, "ETF holdings are unavailable: " + exception.getMessage());
                sectors = Map.of();
                holdings = List.of();
            }
        }

        return new PortfolioExposureInput(
                position.getSymbol(),
                position.getQuantity(),
                position.getAssetType(),
                price,
                currency,
                sectors,
                holdings,
                note
        );
    }

    private static String join(String current, String addition) {
        if (addition == null || addition.isBlank()) {
            return current;
        }
        if (current == null || current.isBlank()) {
            return addition.trim();
        }
        return current + " " + addition.trim();
    }
}
