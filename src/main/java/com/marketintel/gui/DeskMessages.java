package com.marketintel.gui;

import com.marketintel.providers.ProviderText;

import java.util.concurrent.ExecutionException;

/**
 * Short text for the status line and the home note.
 * Provider messages are kept, with keys removed and blank failures
 * replaced by a sentence the screen already knows.
 */
public final class DeskMessages {

    private static final int MAX_LENGTH = 240;

    private DeskMessages() {
    }

    public static String clean(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String cleaned = ProviderText.redact(reason).trim().replaceAll("\\s+", " ");
        if (cleaned.isBlank()) {
            return null;
        }
        if (cleaned.length() > MAX_LENGTH) {
            return cleaned.substring(0, MAX_LENGTH - 3) + "...";
        }
        return cleaned;
    }

    public static String orFallback(Throwable failure, String fallback) {
        if (failure == null) {
            return fallback;
        }
        Throwable cause = failure;
        if (failure instanceof ExecutionException && failure.getCause() != null) {
            cause = failure.getCause();
        }
        return orFallback(cause.getMessage(), fallback);
    }

    public static String orFallback(String reason, String fallback) {
        String cleaned = clean(reason);
        return cleaned == null ? fallback : cleaned;
    }

    public static String loadFailure(Throwable failure) {
        String detail = orFallback(failure, null);
        if (detail == null) {
            return "PORTFOLIO PRICES COULD NOT BE LOADED.";
        }
        return "PORTFOLIO PRICES COULD NOT BE LOADED. " + detail;
    }
}
