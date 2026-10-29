package com.marketintel.providers;

import java.math.BigDecimal;

/**
 * Display bands for a signed sentiment score. Marketaux and SentiSense
 * both feed this scale so the desk uses one set of labels.
 */
final class SentimentBands {

    private SentimentBands() {
    }

    static String label(BigDecimal score) {
        if (score == null) {
            return null;
        }
        if (score.compareTo(new BigDecimal("0.35")) >= 0) {
            return "Bullish";
        }
        if (score.compareTo(new BigDecimal("0.15")) >= 0) {
            return "Somewhat-Bullish";
        }
        if (score.compareTo(new BigDecimal("-0.15")) > 0) {
            return "Neutral";
        }
        if (score.compareTo(new BigDecimal("-0.35")) > 0) {
            return "Somewhat-Bearish";
        }
        return "Bearish";
    }
}
