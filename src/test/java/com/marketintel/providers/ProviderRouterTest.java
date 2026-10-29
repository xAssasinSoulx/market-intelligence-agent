package com.marketintel.providers;

import com.marketintel.model.ETFProfile;
import com.marketintel.model.MarketQuote;
import com.marketintel.model.NewsArticle;
import com.marketintel.model.SecurityOverview;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderRouterTest {

    @Test
    void usesTheFirstRegisteredQuoteAdapterAndSkipsTheFallback() {
        CountingProvider finnhub = new CountingProvider("Finnhub", DataCapability.QUOTE, DataCapability.NEWS);
        CountingProvider alpha = new CountingProvider(
                "Alpha Vantage",
                DataCapability.QUOTE,
                DataCapability.ETF_PROFILE
        );
        ProviderRouter router = new ProviderRouter();
        router.prefer(DataCapability.QUOTE, finnhub);
        router.prefer(DataCapability.QUOTE, alpha);

        MarketQuote quote = router.getQuote("NVDA");

        assertEquals("NVDA", quote.getSymbol());
        assertEquals(1, finnhub.quotes);
        assertEquals(0, alpha.quotes);
        assertEquals("USD", router.quoteCurrency().orElseThrow());
    }

    @Test
    void fallsThroughOnlyWhenTheEarlierAdapterWasNotRegistered() {
        CountingProvider alpha = new CountingProvider("Alpha Vantage", DataCapability.QUOTE);
        alpha.currency = Optional.empty();
        ProviderRouter router = new ProviderRouter();
        router.prefer(DataCapability.QUOTE, alpha);

        router.getQuote("IBM");

        assertEquals(1, alpha.quotes);
        assertTrue(router.quoteCurrency().isEmpty());
    }

    @Test
    void keepsNewsAndEtfPrioritiesIndependent() {
        CountingProvider marketaux = new CountingProvider("Marketaux", DataCapability.NEWS);
        CountingProvider finnhub = new CountingProvider("Finnhub", DataCapability.QUOTE, DataCapability.NEWS);
        CountingProvider alpha = new CountingProvider("Alpha Vantage", DataCapability.ETF_PROFILE);
        ProviderRouter router = new ProviderRouter();
        router.prefer(DataCapability.NEWS, marketaux);
        router.prefer(DataCapability.NEWS, finnhub);
        router.prefer(DataCapability.QUOTE, finnhub);
        router.prefer(DataCapability.ETF_PROFILE, alpha);

        router.getNews("NVDA", 3);
        router.getProfile("QQQ");

        assertEquals(1, marketaux.newsCalls);
        assertEquals(0, finnhub.newsCalls);
        assertEquals(1, alpha.etfCalls);
        assertTrue(router.serves(DataCapability.ETF_PROFILE));
        assertTrue(!router.serves(DataCapability.SENTIMENT));
    }

    @Test
    void refusesToRegisterAnAdapterForACapabilityItDoesNotServe() {
        CountingProvider finnhub = new CountingProvider("Finnhub", DataCapability.QUOTE);
        ProviderRouter router = new ProviderRouter();

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> router.prefer(DataCapability.FINANCIAL_STATEMENTS, finnhub)
        );

        assertTrue(error.getMessage().contains("Finnhub"));
    }

    @Test
    void namesTheMissingKeyWhenNothingIsRegistered() {
        ProviderRouter router = new ProviderRouter();

        MarketDataException error = assertThrows(
                MarketDataException.class,
                () -> router.getFinancialStatements("AAPL")
        );

        assertTrue(error.getMessage().contains("FMP_API_KEY"));
    }

    private static final class CountingProvider implements DataProvider {

        private final String name;
        private final java.util.EnumSet<DataCapability> capabilities;
        private int quotes;
        private int newsCalls;
        private int etfCalls;
        private Optional<String> currency = Optional.of("USD");

        private CountingProvider(String name, DataCapability... capabilities) {
            this.name = name;
            this.capabilities = java.util.EnumSet.copyOf(java.util.List.of(capabilities));
        }

        @Override
        public String providerName() {
            return name;
        }

        @Override
        public boolean supports(DataCapability capability) {
            return capabilities.contains(capability);
        }

        @Override
        public Optional<String> quoteCurrency() {
            return currency;
        }

        @Override
        public MarketQuote getQuote(String symbol) {
            quotes++;
            return new MarketQuote(symbol, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE);
        }

        @Override
        public List<NewsArticle> getNews(String symbol, int limit) {
            newsCalls++;
            return List.of();
        }

        @Override
        public ETFProfile getEtfProfile(String symbol) {
            etfCalls++;
            return new ETFProfile(symbol, null, null, List.of(), java.util.Map.of());
        }

        @Override
        public SecurityOverview getSecurityOverview(String symbol) {
            throw new MarketDataException("unused");
        }
    }
}
