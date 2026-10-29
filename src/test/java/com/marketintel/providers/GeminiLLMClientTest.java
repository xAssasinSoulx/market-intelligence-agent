
package com.marketintel.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeminiLLMClientTest {

    private final GeminiLLMClient client =
            new GeminiLLMClient("dummy-test-key");

    private final ObjectMapper mapper =
            new ObjectMapper();

    @Test
    void createsValidRequestBody() throws Exception {

        String prompt =
                "Explain NVDA's movement.\nUse \"evidence\".";

        String json =
                client.createRequestBody(prompt);

        JsonNode root =
                mapper.readTree(json);

        String submittedPrompt =
                root.path("contents")
                        .get(0)
                        .path("parts")
                        .get(0)
                        .path("text")
                        .asText();

        assertEquals(prompt, submittedPrompt);

        assertEquals(
                "user",
                root.path("contents")
                        .get(0)
                        .path("role")
                        .asText()
        );
    }

    @Test
    void parsesGeneratedText() {

        String response = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "NVIDIA declined during the session."
                          }
                        ]
                      },
                      "finishReason": "STOP"
                    }
                  ]
                }
                """;

        String result =
                client.parseResponse(response);

        assertEquals(
                "NVIDIA declined during the session.",
                result
        );
    }

    @Test
    void combinesMultipleTextParts() {

        String response = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {"text": "First observation."},
                          {"text": "Second observation."}
                        ]
                      }
                    }
                  ]
                }
                """;

        String result =
                client.parseResponse(response);

        assertEquals(
                "First observation.\\nSecond observation."
                        .replace("\\n", "\n"),
                result
        );
    }

    @Test
    void rejectsEmptyCandidates() {

        String response = """
                {
                  "candidates": []
                }
                """;

        assertThrows(
                LLMException.class,
                () -> client.parseResponse(response)
        );
    }

    @Test
    void rejectsBlockedResponse() {

        String response = """
                {
                  "promptFeedback": {
                    "blockReason": "SAFETY"
                  }
                }
                """;

        LLMException error =
                assertThrows(
                        LLMException.class,
                        () -> client.parseResponse(response)
                );

        assertTrue(
                error.getMessage().contains("SAFETY")
        );
    }

    @Test
    void rejectsInvalidJson() {

        assertThrows(
                LLMException.class,
                () -> client.parseResponse("{invalid-json")
        );
    }

    @Test
    void rejectsBlankPrompt() {

        assertThrows(
                IllegalArgumentException.class,
                () -> client.generate("   ")
        );
    }

    @Test
    void rejectsMissingApiKey() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new GeminiLLMClient("")
        );
    }

    @Test
    void rejectsInvalidModelName() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new GeminiLLMClient(
                        "dummy-test-key",
                        "../invalid-model"
                )
        );
    }
}
