package com.marketintel.gui;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class DeskMessagesTest {

    @Test
    void keepsAProviderReasonAndDropsAQueryKey() {
        String cleaned = DeskMessages.clean(
                "Finnhub refused https://finnhub.io/api?token=SECRETVALUE and\nreturned HTTP 401."
        );

        assertFalse(cleaned.contains("SECRETVALUE"));
        assertFalse(cleaned.contains("\n"));
        assertEquals(
                "Finnhub refused https://finnhub.io/api?token=[redacted] and returned HTTP 401.",
                cleaned
        );
    }

    @Test
    void usesTheFallbackWhenTheFailureHasNoMessage() {
        assertNull(DeskMessages.clean("   "));
        assertEquals(
                "UNABLE TO SAVE THE POSITION",
                DeskMessages.orFallback(new IllegalStateException("  "), "UNABLE TO SAVE THE POSITION")
        );
        assertEquals(
                "Failed to save portfolio.",
                DeskMessages.orFallback(
                        new IllegalStateException("Failed to save portfolio."),
                        "UNABLE TO SAVE THE POSITION"
                )
        );
    }

    @Test
    void loadFailureUsesTheCauseAndTruncatesALongReason() {
        ExecutionException wrapped = new ExecutionException(new IllegalStateException("vendor down"));
        assertEquals(
                "PORTFOLIO PRICES COULD NOT BE LOADED. vendor down",
                DeskMessages.loadFailure(wrapped)
        );
        assertEquals(
                "PORTFOLIO PRICES COULD NOT BE LOADED.",
                DeskMessages.loadFailure(new IllegalStateException())
        );

        String longReason = "x".repeat(300);
        String cleaned = DeskMessages.clean(longReason);
        assertEquals(240, cleaned.length());
        assertEquals("...", cleaned.substring(cleaned.length() - 3));
    }
}
