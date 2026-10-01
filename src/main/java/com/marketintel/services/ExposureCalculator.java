package com.marketintel.services;

import com.marketintel.model.ETFHolding;
import com.marketintel.model.ETFProfile;
import com.marketintel.model.ExposureResult;
import com.marketintel.model.OverlapResult;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExposureCalculator {

    public ExposureResult
    calculateSectorExposure(
            ETFProfile profile) {

        if (profile == null) {

            throw new IllegalArgumentException(
                    "ETF profile cannot be null."
            );
        }

        return new ExposureResult(
                profile.getSymbol(),
                profile.getSectorWeights()
        );
    }

    public OverlapResult calculateOverlap(
            ETFProfile first,
            ETFProfile second) {

        if (first == null
                || second == null) {

            throw new IllegalArgumentException(
                    "Both ETF profiles are required."
            );
        }

        Map<String, ETFHolding>
                secondHoldings =
                new HashMap<>();

        for (ETFHolding holding
                : second.getHoldings()) {

            String symbol =
                    holding.getSymbol();

            if (symbol == null
                    || symbol.isBlank()) {

                continue;
            }

            secondHoldings.put(
                    symbol.toUpperCase(),
                    holding
            );
        }

        BigDecimal totalOverlap =
                BigDecimal.ZERO;

        List<ETFHolding>
                commonHoldings =
                new ArrayList<>();

        for (ETFHolding firstHolding
                : first.getHoldings()) {

            String symbol =
                    firstHolding.getSymbol();

            if (symbol == null
                    || symbol.isBlank()) {

                continue;
            }

            ETFHolding secondHolding =
                    secondHoldings.get(
                            symbol.toUpperCase()
                    );

            if (secondHolding == null) {
                continue;
            }

            BigDecimal sharedWeight =
                    firstHolding
                            .getWeight()
                            .min(
                                    secondHolding
                                            .getWeight()
                            );

            totalOverlap =
                    totalOverlap.add(
                            sharedWeight
                    );

            commonHoldings.add(
                    new ETFHolding(
                            firstHolding
                                    .getSymbol(),

                            firstHolding
                                    .getDescription(),

                            sharedWeight
                    )
            );
        }

        commonHoldings.sort(
                Comparator.comparing(
                        ETFHolding::getWeight
                ).reversed()
        );

        return new OverlapResult(
                first.getSymbol(),
                second.getSymbol(),
                totalOverlap,
                commonHoldings
        );
    }
}