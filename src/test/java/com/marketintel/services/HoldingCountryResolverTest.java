package com.marketintel.services;

import com.marketintel.model.ETFHolding;
import com.marketintel.model.ETFProfile;
import com.marketintel.providers.HeadquartersLookup;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HoldingCountryResolverTest {

    @Test
    void looksUpOnlyTheLargestHoldings() {
        AtomicInteger calls = new AtomicInteger();
        HeadquartersLookup lookup = symbol -> {
            calls.incrementAndGet();
            return "US";
        };

        ETFProfile profile = new ETFProfile(
                "IVV",
                null,
                null,
                List.of(
                        new ETFHolding("SMALL", "Small", new BigDecimal("0.01")),
                        new ETFHolding("LARGE", "Large", new BigDecimal("0.20"))
                ),
                Map.of()
        );

        ETFProfile resolved = new HoldingCountryResolver(1).apply(profile, lookup);

        assertEquals(1, calls.get());
        ETFHolding large = resolved.getHoldings().stream()
                .filter(holding -> "LARGE".equals(holding.getSymbol()))
                .findFirst()
                .orElseThrow();
        ETFHolding small = resolved.getHoldings().stream()
                .filter(holding -> "SMALL".equals(holding.getSymbol()))
                .findFirst()
                .orElseThrow();
        assertEquals("US", large.getCountry());
        assertNull(small.getCountry());
    }

    @Test
    void keepsACountryTheHoldingAlreadyCarries() {
        HeadquartersLookup lookup = symbol -> {
            throw new AssertionError("lookup was not required");
        };

        ETFProfile profile = new ETFProfile(
                "IVV",
                null,
                null,
                List.of(new ETFHolding("AAPL", "Apple", new BigDecimal("0.07"), "US")),
                Map.of()
        );

        ETFProfile resolved = new HoldingCountryResolver().apply(profile, lookup);

        assertEquals("US", resolved.getHoldings().getFirst().getCountry());
    }
}
