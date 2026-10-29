
package com.marketintel.services;

import com.marketintel.model.MarketMovementContext;
import com.marketintel.model.MarketQuote;
import com.marketintel.model.NewsArticle;

import com.marketintel.providers.LLMException;
import com.marketintel.providers.LLMProvider;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MarketExplanationServiceTest {

    private MarketQuote sampleQuote() {

        return new MarketQuote(
                "NVDA",
                new BigDecimal("214.13"),
                new BigDecimal("217.23"),
                new BigDecimal("-1.4271")
        );
    }

    private NewsArticle sampleArticle() {

        return new NewsArticle(
                "NVIDIA and AI infrastructure",
                "https://example.com/nvda",
                "Example Financial News",
                "2026-10-18T14:00:00Z",
                "An article discussing AI infrastructure.",
                new BigDecimal("0.35"),
                "Bullish"
        );
    }

    private MarketMovementContext sampleContext() {

        return new MarketMovementContext(
                sampleQuote(),
                List.of(sampleArticle()),
                Instant.parse("2026-10-18T19:43:18Z"),
                null
        );
    }

    @Test
    void buildsEvidenceGroundedPrompt() {

        LLMProvider fakeProvider =
                prompt -> "Example explanation.";

        MarketExplanationService service =
                new MarketExplanationService(fakeProvider);

        String prompt =
                service.buildPrompt(sampleContext());

        assertTrue(prompt.contains("NVDA"));
        assertTrue(prompt.contains("214.13"));
        assertTrue(prompt.contains("217.23"));
        assertTrue(prompt.contains("-1.4271%"));
        assertTrue(prompt.contains("NVIDIA and AI infrastructure"));
        assertTrue(prompt.contains("https://example.com/nvda"));

        assertTrue(
                prompt.contains(
                        "Quote last-trade time (UTC): UNKNOWN"
                )
        );

        assertTrue(
                prompt.contains(
                        "Do not invent prices"
                )
        );
    }

    @Test
    void includesTheLastTradeTimeAndFlagsLaterArticles() {

        MarketQuote timedQuote = new MarketQuote(
                "NVDA",
                new BigDecimal("214.13"),
                new BigDecimal("217.23"),
                new BigDecimal("-1.4271"),
                Instant.parse("2026-10-18T20:00:00Z")
        );

        NewsArticle later = new NewsArticle(
                "After-hours commentary",
                "https://example.com/later",
                "Example Financial News",
                "2026-10-18T21:15:00Z",
                "Published after the close.",
                null,
                null
        );

        MarketMovementContext context =
                new MarketMovementContext(
                        timedQuote,
                        List.of(sampleArticle(), later),
                        Instant.parse("2026-10-18T22:00:00Z"),
                        null
                );

        MarketExplanationService service =
                new MarketExplanationService(prompt -> "ok");

        String prompt = service.buildPrompt(context);

        assertTrue(prompt.contains(
                "Quote last-trade time (UTC): 2026-10-18T20:00:00Z"
        ));
        assertTrue(prompt.contains(
                "2026-10-18T21:15:00Z (published AFTER the last trade)"
        ));
        assertFalse(prompt.contains(
                "2026-10-18T14:00:00Z (published AFTER the last trade)"
        ));
    }

    @Test
    void sendsPromptToLLMAndReturnsExplanation() {

        AtomicReference<String> capturedPrompt =
                new AtomicReference<>();

        LLMProvider fakeProvider = prompt -> {
            capturedPrompt.set(prompt);

            return "  NVIDIA declined according "
                    + "to the reported quote.  ";
        };

        MarketExplanationService service =
                new MarketExplanationService(fakeProvider);

        String result =
                service.explain(sampleContext());

        assertEquals(
                "NVIDIA declined according "
                        + "to the reported quote.",
                result
        );

        assertNotNull(capturedPrompt.get());

        assertTrue(
                capturedPrompt.get().contains("NVDA")
        );
    }

    @Test
    void handlesMissingNewsEvidence() {

        MarketMovementContext context =
                new MarketMovementContext(
                        sampleQuote(),
                        List.of(),
                        Instant.parse(
                                "2026-10-18T19:43:18Z"
                        ),
                        "Marketaux rate limit exceeded."
                );

        MarketExplanationService service =
                new MarketExplanationService(
                        prompt -> "Evidence is limited."
                );

        String prompt =
                service.buildPrompt(context);

        assertTrue(
                prompt.contains(
                        "No qualifying recent news articles"
                )
        );

        assertTrue(
                prompt.contains(
                        "Marketaux rate limit exceeded."
                )
        );
    }

    @Test
    void rejectsEmptyLLMOutput() {

        MarketExplanationService service =
                new MarketExplanationService(
                        prompt -> "   "
                );

        assertThrows(
                LLMException.class,
                () -> service.explain(sampleContext())
        );
    }

    @Test
    void rejectsNullContext() {

        MarketExplanationService service =
                new MarketExplanationService(
                        prompt -> "Example explanation."
                );

        assertThrows(
                NullPointerException.class,
                () -> service.explain(null)
        );
    }

    @Test
    void rejectsMissingLLMProvider() {

        assertThrows(
                NullPointerException.class,
                () -> new MarketExplanationService(null)
        );
    }
}
