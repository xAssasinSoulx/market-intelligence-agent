
package com.marketintel.providers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class GeminiLLMClient implements LLMProvider {

    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    private static final String DEFAULT_MODEL =
            "gemini-3.8-flash";

    private static final Duration REQUEST_TIMEOUT =
            Duration.ofSeconds(45);

    private static final int EXPLANATION_TOKENS = 400;
    private static final int BRIEF_TOKENS = 160;

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiLLMClient(String apiKey) {
        this(apiKey, DEFAULT_MODEL);
    }

    public GeminiLLMClient(
            String apiKey,
            String model) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Gemini API key cannot be blank."
            );
        }

        if (model == null
                || !model.matches("[A-Za-z0-9._-]+")) {

            throw new IllegalArgumentException(
                    "Invalid Gemini model name."
            );
        }

        this.apiKey = apiKey.trim();
        this.model = model;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String generate(String prompt) {

        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException(
                    "LLM prompt cannot be blank."
            );
        }

        String requestBody = createRequestBody(prompt, EXPLANATION_TOKENS);
        return send(requestBody);
    }

    @Override
    public String generateBrief(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("LLM prompt cannot be blank.");
        }
        return send(createRequestBody(prompt, BRIEF_TOKENS));
    }

    private String send(String requestBody) {

        URI uri = URI.create(
                BASE_URL + model + ":generateContent"
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                requestBody,
                                StandardCharsets.UTF_8
                        )
                )
                .build();

        try {

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString(
                                    StandardCharsets.UTF_8
                            )
                    );

            if (response.statusCode() != 200) {

                throw new LLMException(
                        "Gemini request failed (HTTP "
                                + response.statusCode()
                                + "). Check the API key, "
                                + "model access and usage quota."
                );
            }

            return parseResponse(response.body());

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new LLMException(
                    "Gemini request was interrupted.",
                    e
            );

        } catch (IOException e) {

            throw new LLMException(
                    describeCommunicationFailure(e),
                    e
            );
        }
    }

    static String describeCommunicationFailure(IOException exception) {

        if (exception instanceof HttpTimeoutException) {
            return "Unable to communicate with Gemini. The request exceeded "
                    + REQUEST_TIMEOUT.toSeconds()
                    + " seconds.";
        }

        String detail = exception.getMessage();
        if (detail == null || detail.isBlank()) {
            return "Unable to communicate with Gemini.";
        }

        return "Unable to communicate with Gemini. " + detail.trim();
    }

    /*
     * Build JSON using Jackson instead of manually
     * concatenating strings.
     *
     * This safely escapes quotes, newlines and other
     * special characters inside the prompt.
     *
     * Package-private for offline JUnit testing.
     */
    String createRequestBody(String prompt) {
        return createRequestBody(prompt, EXPLANATION_TOKENS);
    }

    String createRequestBody(String prompt, int maxOutputTokens) {

        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException(
                    "LLM prompt cannot be blank."
            );
        }

        ObjectNode root =
                objectMapper.createObjectNode();

        ArrayNode contents =
                root.putArray("contents");

        ObjectNode content =
                contents.addObject();

        content.put("role", "user");

        content.putArray("parts")
                .addObject()
                .put("text", prompt);

        ObjectNode generation = root.putObject("generationConfig");
        generation.put("maxOutputTokens", maxOutputTokens);
        generation.put("temperature", 0.2);
        ObjectNode thinking = generation.putObject("thinkingConfig");
        if (model.startsWith("gemini-3")) {
            thinking.put("thinkingLevel", "LOW");
        } else {
            thinking.put("thinkingBudget", 0);
        }

        try {

            return objectMapper.writeValueAsString(root);

        } catch (JsonProcessingException e) {

            throw new LLMException(
                    "Unable to serialize Gemini request.",
                    e
            );
        }
    }

    /*
     * Extract generated text from the Gemini response.
     *
     * A response can contain multiple text parts, so
     * we collect them instead of assuming one part.
     *
     * Package-private for offline JUnit testing.
     */
    String parseResponse(String responseBody) {

        if (responseBody == null
                || responseBody.isBlank()) {

            throw new LLMException(
                    "Gemini returned an empty response."
            );
        }

        try {

            JsonNode root =
                    objectMapper.readTree(responseBody);

            JsonNode candidates =
                    root.path("candidates");

            if (!candidates.isArray()
                    || candidates.isEmpty()) {

                String blockReason =
                        root.path("promptFeedback")
                                .path("blockReason")
                                .asText("");

                if (!blockReason.isBlank()) {

                    throw new LLMException(
                            "Gemini did not generate a response. "
                                    + "Block reason: "
                                    + blockReason
                    );
                }

                throw new LLMException(
                        "Gemini returned no response candidates."
                );
            }

            JsonNode parts =
                    candidates.get(0)
                            .path("content")
                            .path("parts");

            if (!parts.isArray()) {

                throw new LLMException(
                        "Gemini response contained no text parts."
                );
            }

            StringBuilder answer =
                    new StringBuilder();

            for (JsonNode part : parts) {

                if (part.path("thought").asBoolean(false)) {
                    continue;
                }

                JsonNode textNode = part.path("text");
                if (!textNode.isTextual() || textNode.asText().isBlank()) {
                    continue;
                }

                if (!answer.isEmpty()) {
                    answer.append("\n");
                }

                answer.append(textNode.asText());
            }

            if (answer.isEmpty()) {

                throw new LLMException(
                        "Gemini generated no usable text."
                );
            }

            return answer.toString().trim();

        } catch (JsonProcessingException e) {

            throw new LLMException(
                    "Gemini returned invalid JSON.",
                    e
            );
        }
    }
}
