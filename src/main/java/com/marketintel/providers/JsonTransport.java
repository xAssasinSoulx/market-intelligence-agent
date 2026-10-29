package com.marketintel.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * Shared HTTP call used by adapters. Status and JSON stay with the
 * adapter, which knows the vendor's error shape.
 */
final class JsonTransport {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    JsonTransport() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    JsonResponse get(URI uri, Map<String, String> headers, String providerName) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .GET();
        if (headers != null) {
            headers.forEach((name, value) -> {
                if (name != null && value != null) {
                    builder.header(name, value);
                }
            });
        }

        try {
            HttpResponse<String> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            String body = response.body() == null ? "" : response.body();
            return new JsonResponse(response.statusCode(), parse(body));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new MarketDataException(providerName + " request was interrupted.", exception);
        } catch (IOException exception) {
            throw new MarketDataException("Unable to reach " + providerName + ".", exception);
        }
    }

    private JsonNode parse(String body) {
        String trimmed = body.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return null;
        }
        try {
            return objectMapper.readTree(trimmed);
        } catch (IOException exception) {
            return null;
        }
    }

    record JsonResponse(int status, JsonNode json) {
    }
}
