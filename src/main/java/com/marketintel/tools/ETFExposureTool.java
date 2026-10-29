package com.marketintel.tools;

import com.marketintel.model.ActionType;
import com.marketintel.model.ETFProfile;
import com.marketintel.model.ToolRequest;
import com.marketintel.model.ToolResult;
import com.marketintel.model.ValidationResult;

import com.marketintel.providers.CountryExposureSource;
import com.marketintel.providers.ETFDataProvider;
import com.marketintel.providers.HeadquartersLookup;
import com.marketintel.providers.MarketDataException;
import com.marketintel.providers.ProviderText;

import com.marketintel.services.ExposureCalculator;
import com.marketintel.services.HoldingCountryResolver;
import com.marketintel.services.RequestValidator;

public class ETFExposureTool
        implements AgentTool {

    private final ETFDataProvider provider;
    private final ExposureCalculator calculator;
    private final RequestValidator validator;
    private final CountryExposureSource countryExposure;
    private final boolean sectorAvailable;
    private final HeadquartersLookup headquartersLookup;

    public ETFExposureTool(
            ETFDataProvider provider,
            ExposureCalculator calculator,
            RequestValidator validator) {

        this(provider, calculator, validator, null, true, null);
    }

    /**
     * @param countryExposure fund-level country weights, or null when that
     *                        feed is not configured
     * @param sectorAvailable true when the ETF profile provider can serve
     *                        sector weights
     */
    public ETFExposureTool(
            ETFDataProvider provider,
            ExposureCalculator calculator,
            RequestValidator validator,
            CountryExposureSource countryExposure,
            boolean sectorAvailable) {

        this(
                provider,
                calculator,
                validator,
                countryExposure,
                sectorAvailable,
                null
        );
    }

    public ETFExposureTool(
            ETFDataProvider provider,
            ExposureCalculator calculator,
            RequestValidator validator,
            CountryExposureSource countryExposure,
            boolean sectorAvailable,
            HeadquartersLookup headquartersLookup) {

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
        this.countryExposure = countryExposure;
        this.sectorAvailable = sectorAvailable;
        this.headquartersLookup = headquartersLookup;
    }

    @Override
    public String getName() {
        return "etf-exposure";
    }

    @Override
    public boolean supports(
            ActionType action) {

        if (action == ActionType.ETF_SECTOR_EXPOSURE) {
            return sectorAvailable;
        }

        return action == ActionType.ETF_GEOGRAPHIC_EXPOSURE
                && (countryExposure != null || sectorAvailable);
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

            if (request.getAction()
                    == ActionType.ETF_GEOGRAPHIC_EXPOSURE) {

                return geographic(normalizedSymbol);
            }

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

    private ToolResult geographic(String symbol) {

        if (countryExposure != null) {
            try {
                return ToolResult.success(
                        calculator.calculateReportedCountryExposure(
                                symbol,
                                countryExposure.getCountryExposure(symbol)
                        )
                );
            } catch (MarketDataException fundReport) {
                if (headquartersLookup == null || !sectorAvailable) {
                    throw fundReport;
                }
                try {
                    return holdingsGeography(symbol);
                } catch (RuntimeException fallback) {
                    return ToolResult.failure(ProviderText.redact(
                            fundReport.getMessage() + " " + fallback.getMessage()
                    ));
                }
            }
        }

        return holdingsGeography(symbol);
    }

    private ToolResult holdingsGeography(String symbol) {
        ETFProfile profile = provider.getProfile(symbol);
        String note = null;
        if (headquartersLookup != null) {
            profile = new HoldingCountryResolver().apply(profile, headquartersLookup);
            note = HoldingCountryResolver.NOTE;
        }
        return ToolResult.success(
                calculator.calculateGeographicExposure(profile, note)
        );
    }
}