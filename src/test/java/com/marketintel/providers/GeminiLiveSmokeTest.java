
package com.marketintel.providers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

class GeminiLiveSmokeTest {

    @Test
    void generatesRealResponse() {

        // Never call the live API during normal tests.
        assumeTrue(
                "1".equals(
                        System.getenv("RUN_GEMINI_LIVE_TEST")
                ),
                "Live Gemini test is disabled."
        );

        String apiKey =
                System.getenv("GEMINI_API_KEY");

        assertNotNull(
                apiKey,
                "GEMINI_API_KEY is missing."
        );

        assertFalse(
                apiKey.isBlank(),
                "GEMINI_API_KEY is blank."
        );

        String model =
                System.getenv("GEMINI_MODEL");

        if (model == null || model.isBlank()) {
            model = "gemini-2.5-flash";
        }

        LLMProvider provider =
                new GeminiLLMClient(apiKey, model);

        String response =
                provider.generate(
                        "Reply with exactly the word READY."
                );

        assertNotNull(response);
        assertFalse(response.isBlank());

        System.out.println(
                "Gemini response: " + response
        );
    }
}
