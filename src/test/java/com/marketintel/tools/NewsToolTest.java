package com.marketintel.tools;

import com.marketintel.model.ActionType;
import com.marketintel.model.NewsArticle;
import com.marketintel.model.NewsResult;
import com.marketintel.model.ToolRequest;
import com.marketintel.model.ToolResult;

import com.marketintel.providers.NewsProvider;

import com.marketintel.services.RequestValidator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NewsToolTest {

    private NewsTool tool;

    @BeforeEach
    void setUp() {

        tool =
                new NewsTool(
                        new FakeNewsProvider(),
                        new RequestValidator()
                );
    }

    @Test
    void supportsFinancialNews() {

        assertTrue(
                tool.supports(
                        ActionType.FINANCIAL_NEWS
                )
        );
    }

    @Test
    void returnsNewsResult() {

        ToolRequest request =
                new ToolRequest(
                        1,
                        ActionType.FINANCIAL_NEWS,

                        Map.of(
                                "symbol",
                                "nvda"
                        )
                );

        ToolResult result =
                tool.execute(
                        request
                );

        assertTrue(
                result.isSuccess()
        );

        assertInstanceOf(
                NewsResult.class,
                result.getData()
        );

        NewsResult news =
                (NewsResult)
                        result.getData();

        assertEquals(
                "NVDA",
                news.getSymbol()
        );

        assertEquals(
                1,
                news.getArticles()
                        .size()
        );
    }

    @Test
    void missingSymbolIsRejected() {

        ToolRequest request =
                new ToolRequest(
                        1,
                        ActionType.FINANCIAL_NEWS,
                        Map.of()
                );

        ToolResult result =
                tool.execute(
                        request
                );

        assertFalse(
                result.isSuccess()
        );
    }

    private static class FakeNewsProvider
            implements NewsProvider {

        @Override
        public List<NewsArticle> getNews(
                String symbol,
                int limit) {

            return List.of(
                    new NewsArticle(
                            "Test article",
                            "https://example.com",
                            "Example News",
                            "20261001T200000",
                            "Test summary",
                            new BigDecimal(
                                    "0.25"
                            ),
                            "Somewhat-Bullish"
                    )
            );
        }
    }
}