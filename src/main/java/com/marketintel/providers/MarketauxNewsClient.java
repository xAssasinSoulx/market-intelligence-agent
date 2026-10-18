
package com.marketintel.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketintel.model.NewsArticle;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public class MarketauxNewsClient implements NewsProvider {

    private static final String BASE_URL =
            "https://api.marketaux.com/v1/news/all";

    // Maximum articles per request on the free plan.
    private static final int FREE_PLAN_LIMIT = 3;

    private final String apiToken;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MarketauxNewsClient(String apiToken) {

        if (apiToken == null || apiToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Marketaux API token cannot be blank."
            );
        }

        this.apiToken = apiToken.trim();

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<NewsArticle> getNews(
            String symbol,
            int limit) {

        String normalizedSymbol = normalizeSymbol(symbol);

        if (limit <= 0) {
            throw new IllegalArgumentException(
                    "News limit must be positive."
            );
        }

        int requestedLimit = Math.min(
                limit,
                FREE_PLAN_LIMIT
        );

        // Retrieve news from the last seven days.
        Instant now = Instant.now();

        Instant cutoff = now.minus(
                7,
                ChronoUnit.DAYS
        );

        String publishedAfter = DateTimeFormatter
                .ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                .withZone(ZoneOffset.UTC)
                .format(cutoff);

        URI uri = URI.create(
                BASE_URL
                        + "?symbols=" + encode(normalizedSymbol)
                        + "&filter_entities=true"
                        + "&language=en"
                        + "&published_after=" + encode(publishedAfter)
                        + "&limit=" + requestedLimit
                        + "&api_token=" + encode(apiToken)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                throw new MarketDataException(
                        "Marketaux returned HTTP "
                                + response.statusCode()
                                + ". Check your token, plan limits, "
                                + "or provider availability."
                );
            }

            JsonNode root = objectMapper.readTree(
                    response.body()
            );

            if (root.has("error")) {
                throw new MarketDataException(
                        "Marketaux returned an API error: "
                                + root.path("error")
                                .path("message")
                                .asText("Unknown error.")
                );
            }

            return parseNews(
                    normalizedSymbol,
                    root,
                    requestedLimit
            )
                    .stream()
                    .filter(article ->
                            isRecent(
                                    article.getPublishedAt(),
                                    cutoff,
                                    now
                            )
                    )
                    .toList();

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new MarketDataException(
                    "Marketaux request was interrupted.",
                    e
            );

        } catch (IOException e) {

            throw new MarketDataException(
                    "Unable to retrieve Marketaux news.",
                    e
            );
        }
    }

    // Package-private so JUnit can test parsing
    // without making real API requests.
    List<NewsArticle> parseNews(
            String symbol,
            JsonNode root,
            int limit) {

        if (root == null || root.isNull()) {
            throw new MarketDataException(
                    "Marketaux returned an empty response."
            );
        }

        JsonNode data = root.path("data");

        if (!data.isArray()) {
            throw new MarketDataException(
                    "Marketaux response is missing news data."
            );
        }

        List<NewsArticle> articles = new ArrayList<>();

        for (JsonNode articleNode : data) {

            if (articles.size() >= limit) {
                break;
            }

            String title = textOrNull(articleNode, "title");
            String url = textOrNull(articleNode, "url");

            if (title == null || url == null) {
                continue;
            }

            JsonNode matchingEntity = findEntity(
                    articleNode,
                    symbol
            );

            // Independently verify that the article
            // identifies the requested ticker.
            if (matchingEntity == null) {
                continue;
            }

            BigDecimal sentimentScore = decimalOrNull(
                    matchingEntity.path("sentiment_score")
            );

            String sentimentLabel = labelForSentiment(
                    sentimentScore
            );

            String summary = textOrNull(
                    articleNode,
                    "description"
            );

            if (summary == null) {
                summary = textOrNull(
                        articleNode,
                        "snippet"
                );
            }

            articles.add(
                    new NewsArticle(
                            title,
                            url,
                            textOrNull(articleNode, "source"),
                            textOrNull(articleNode, "published_at"),
                            summary,
                            sentimentScore,
                            sentimentLabel
                    )
            );
        }

        return List.copyOf(articles);
    }

    private JsonNode findEntity(
            JsonNode articleNode,
            String symbol) {

        JsonNode entities = articleNode.path("entities");

        if (!entities.isArray()) {
            return null;
        }

        for (JsonNode entity : entities) {

            String entitySymbol = textOrNull(
                    entity,
                    "symbol"
            );

            if (entitySymbol != null
                    && entitySymbol.equalsIgnoreCase(symbol)) {
                return entity;
            }
        }

        return null;
    }

    /*
     * Application-defined display labels.
     * Marketaux supplies a numeric score, not these
     * particular categorical labels.
     */
    private String labelForSentiment(BigDecimal score) {

        if (score == null) {
            return null;
        }

        if (score.compareTo(
                new BigDecimal("0.35")) >= 0) {
            return "Bullish";
        }

        if (score.compareTo(
                new BigDecimal("0.15")) >= 0) {
            return "Somewhat-Bullish";
        }

        if (score.compareTo(
                new BigDecimal("-0.15")) > 0) {
            return "Neutral";
        }

        if (score.compareTo(
                new BigDecimal("-0.35")) > 0) {
            return "Somewhat-Bearish";
        }

        return "Bearish";
    }

    private String textOrNull(
            JsonNode node,
            String field) {

        JsonNode value = node.path(field);

        if (value.isMissingNode() || value.isNull()) {
            return null;
        }

        String text = value.asText().trim();

        return text.isBlank() ? null : text;
    }

    private BigDecimal decimalOrNull(JsonNode value) {

        if (value == null
                || value.isMissingNode()
                || value.isNull()) {
            return null;
        }

        try {
            return new BigDecimal(value.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    boolean isRecent(
            String publishedAt,
            Instant cutoff,
            Instant now) {

        if (publishedAt == null
                || publishedAt.isBlank()) {
            return false;
        }

        try {
            Instant published =
                    Instant.parse(publishedAt);

            return !published.isBefore(cutoff)
                    && !published.isAfter(
                    now.plus(
                            5,
                            ChronoUnit.MINUTES
                    )
            );

        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private String normalizeSymbol(String symbol) {

        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException(
                    "Symbol cannot be blank."
            );
        }

        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    private String encode(String value) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }
}
