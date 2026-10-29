package com.marketintel.providers;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Provider error text sometimes repeats the caller's own key.
 * Nothing shown to a person should contain that value.
 */
public final class ProviderText {

    private static final Pattern KEY_AS = Pattern.compile(
            "(?i)(api key as\\s+)\\S+"
    );
    private static final Pattern QUERY_KEY = Pattern.compile(
            "(?i)([?&](?:apikey|api_key|token|access_token)=)[^&\\s]+"
    );

    private ProviderText() {
    }

    public static String redact(String text, String... secrets) {
        if (text == null || text.isBlank()) {
            return text == null ? "" : text;
        }
        String cleaned = text;
        if (secrets != null) {
            for (String secret : secrets) {
                if (secret != null && secret.length() >= 6) {
                    cleaned = cleaned.replace(secret, "[redacted]");
                }
            }
        }
        cleaned = KEY_AS.matcher(cleaned).replaceAll("$1[redacted]");
        cleaned = QUERY_KEY.matcher(cleaned).replaceAll("$1[redacted]");
        return cleaned;
    }

    public static String alphaVantageNotice(String raw, String apiKey) {
        if (raw == null || raw.isBlank()) {
            return "Alpha Vantage could not complete the request.";
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("rate limit")
                || lower.contains("25 requests")
                || lower.contains("api key")
                || lower.contains("premium")) {
            return "Alpha Vantage's free daily request limit was reached. Try again tomorrow.";
        }
        return redact(raw, apiKey);
    }
}
