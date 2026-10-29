package com.marketintel.services;

import com.marketintel.model.ETFHolding;
import com.marketintel.model.ETFProfile;
import com.marketintel.providers.HeadquartersLookup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fills a missing holding country from the company's headquarters profile.
 * The fund's own country report is preferred when a plan includes it.
 * This path is the free-plan substitute, and it is not revenue exposure.
 */
public final class HoldingCountryResolver {

    public static final int MAX_LOOKUPS = 25;

    public static final String NOTE =
            "Country is the headquarters country from each holding's company profile. "
                    + "It is not the fund's published country exposure and it is not revenue by country. "
                    + "At most "
                    + MAX_LOOKUPS
                    + " of the largest reported holdings are looked up.";

    private final int maxLookups;

    public HoldingCountryResolver() {
        this(MAX_LOOKUPS);
    }

    HoldingCountryResolver(int maxLookups) {
        if (maxLookups < 1) {
            throw new IllegalArgumentException("At least one country lookup is required.");
        }
        this.maxLookups = maxLookups;
    }

    public ETFProfile apply(ETFProfile profile, HeadquartersLookup lookup) {
        if (profile == null) {
            throw new IllegalArgumentException("ETF profile cannot be null.");
        }
        if (lookup == null) {
            throw new IllegalArgumentException("Headquarters lookup cannot be null.");
        }

        List<ETFHolding> ordered = new ArrayList<>(profile.getHoldings());
        ordered.sort(Comparator.comparing(ETFHolding::getWeight).reversed());

        Map<String, String> cache = new HashMap<>();
        int used = 0;
        List<ETFHolding> resolved = new ArrayList<>();

        for (ETFHolding holding : ordered) {
            String country = holding.getCountry();
            String symbol = holding.getSymbol();
            if ((country == null || country.isBlank())
                    && symbol != null
                    && !symbol.isBlank()) {
                String key = symbol.trim().toUpperCase(Locale.ROOT);
                if (!cache.containsKey(key)) {
                    if (used >= maxLookups) {
                        cache.put(key, null);
                    } else {
                        used++;
                        cache.put(key, normalize(lookup.headquartersCountry(key)));
                    }
                }
                country = cache.get(key);
            }
            resolved.add(new ETFHolding(
                    holding.getSymbol(),
                    holding.getDescription(),
                    holding.getWeight(),
                    country
            ));
        }

        return new ETFProfile(
                profile.getSymbol(),
                profile.getNetAssets(),
                profile.getExpenseRatio(),
                resolved,
                profile.getSectorWeights()
        );
    }

    private static String normalize(String country) {
        if (country == null || country.isBlank()) {
            return null;
        }
        return country.trim();
    }
}
