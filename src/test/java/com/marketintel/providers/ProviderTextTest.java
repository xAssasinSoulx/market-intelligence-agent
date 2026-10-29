package com.marketintel.providers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderTextTest {

    @Test
    void dropsTheKeyFromAnAlphaVantageRateLimitNotice() {
        String raw = "We have detected your API key as DEMOKEY123 "
                + "and our standard API rate limit is 25 requests per day. "
                + "Please subscribe to any of the premium plans at "
                + "https://www.alphavantage.co/premium/ to instantly remove all daily rate limits.";

        String safe = ProviderText.alphaVantageNotice(raw, "DEMOKEY123");

        assertFalse(safe.contains("DEMOKEY123"));
        assertTrue(safe.contains("daily request limit"));
    }

    @Test
    void redactsAKeyThatAProviderRepeatsInProse() {
        String safe = ProviderText.redact(
                "Finnhub refused the call. We have detected your API key as DEMOKEY123."
        );

        assertFalse(safe.contains("DEMOKEY123"));
        assertTrue(safe.contains("[redacted]"));
    }
}
