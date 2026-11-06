package com.marketintel.app;

import com.marketintel.agent.AgentController;
import com.marketintel.agent.RequestPlanner;
import com.marketintel.agent.ResponseComposer;
import com.marketintel.agent.ToolManager;
import com.marketintel.auth.SessionManager;
import com.marketintel.model.ActionType;
import com.marketintel.model.MarketMovementContext;
import com.marketintel.model.MarketQuote;
import com.marketintel.model.NewsArticle;
import com.marketintel.model.ToolRequest;
import com.marketintel.model.ToolResult;
import com.marketintel.model.User;
import com.marketintel.persistence.DatabaseManager;
import com.marketintel.persistence.SQLiteConversationRepository;
import com.marketintel.providers.LLMException;
import com.marketintel.services.ConversationMemory;
import com.marketintel.services.MarketExplanationService;
import com.marketintel.tools.AgentTool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchGatewayTest {

    @Test
    void rejectsRequestsWhenNobodyIsSignedIn() {
        RecordingTool tool = new RecordingTool(ActionType.MARKET_QUOTE, ToolResult.success("ok"));
        ResearchGateway gateway = gateway(null, tool, null);

        ToolResult result = gateway.execute(
                7,
                ActionType.MARKET_QUOTE,
                Map.of("symbol", "NVDA")
        );

        assertFalse(result.isSuccess());
        assertEquals(0, tool.calls);
    }

    @Test
    void rejectsARequestWhoseUserNoLongerMatchesTheSession() {
        RecordingTool tool = new RecordingTool(ActionType.MARKET_QUOTE, ToolResult.success("ok"));
        ResearchGateway gateway = gateway(user(7), tool, null);

        ToolResult result = gateway.execute(
                8,
                ActionType.MARKET_QUOTE,
                Map.of("symbol", "NVDA")
        );

        assertFalse(result.isSuccess());
        assertEquals(0, tool.calls);
    }

    @Test
    void executesTheRegisteredToolForTheSignedInUser() {
        RecordingTool tool = new RecordingTool(
                ActionType.MARKET_QUOTE,
                ToolResult.success("quoted")
        );
        ResearchGateway gateway = gateway(user(7), tool, null);

        ToolResult result = gateway.execute(
                7,
                ActionType.MARKET_QUOTE,
                Map.of("symbol", "NVDA")
        );

        assertTrue(result.isSuccess());
        assertEquals("quoted", result.getData());
        assertEquals(7, tool.lastUserId);
    }

    @Test
    void reportsMissingCapabilitiesInsteadOfInventingData() {
        ResearchGateway gateway = gateway(user(7), null, null);

        ToolResult result = gateway.execute(
                7,
                ActionType.ETF_GEOGRAPHIC_EXPOSURE,
                Map.of("symbol", "QQQ")
        );

        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("FINNHUB_API_KEY"));
        assertTrue(result.getErrorMessage().contains("not a guessed allocation"));
        assertFalse(gateway.isAvailable(ActionType.ETF_GEOGRAPHIC_EXPOSURE));
    }

    @Test
    void keepsEvidenceWhenGeminiIsNotConfigured() {
        MarketMovementContext context = sampleContext();
        RecordingTool tool = new RecordingTool(
                ActionType.MARKET_MOVEMENT,
                ToolResult.success(context)
        );
        ResearchGateway gateway = gateway(user(7), tool, null);

        MovementReport report = gateway.explainMovement(7, "NVDA");

        assertTrue(report.hasEvidence());
        assertNull(report.getExplanation());
        assertTrue(report.getExplanationNote().contains("Gemini is not configured"));
    }

    @Test
    void keepsEvidenceWhenTheModelFails() {
        MarketMovementContext context = sampleContext();
        RecordingTool tool = new RecordingTool(
                ActionType.MARKET_MOVEMENT,
                ToolResult.success(context)
        );
        MarketExplanationService explanations = new MarketExplanationService(
                prompt -> {
                    throw new LLMException("model unavailable");
                }
        );
        ResearchGateway gateway = gateway(user(7), tool, explanations);

        MovementReport report = gateway.explainMovement(7, "NVDA");

        assertTrue(report.hasEvidence());
        assertNull(report.getExplanation());
        assertEquals("model unavailable", report.getExplanationNote());
    }

    @Test
    void returnsTheModelExplanationWithTheEvidence() {
        MarketMovementContext context = sampleContext();
        RecordingTool tool = new RecordingTool(
                ActionType.MARKET_MOVEMENT,
                ToolResult.success(context)
        );
        MarketExplanationService explanations = new MarketExplanationService(
                prompt -> "Evidence is limited. [1]"
        );
        ResearchGateway gateway = gateway(user(7), tool, explanations);

        MovementReport report = gateway.explainMovement(7, "NVDA");

        assertEquals("Evidence is limited. [1]", report.getExplanation());
        assertEquals("NVDA", report.getContext().getSymbol());
    }

    @Test
    void remembersAQuoteTheUserRequested(@TempDir Path directory) throws Exception {
        MemoryDesk desk = memoryDesk(directory);
        desk.sessions.startSession(user(7));

        ToolResult result = desk.gateway.execute(
                7,
                ActionType.MARKET_QUOTE,
                Map.of("symbol", "nvda")
        );

        assertTrue(result.isSuccess());
        assertEquals("NVDA", desk.memory.lastSymbol(7));
    }

    @Test
    void backgroundRefreshDoesNotReplaceTheFollowUpSymbol(@TempDir Path directory) throws Exception {
        MemoryDesk desk = memoryDesk(directory);
        desk.sessions.startSession(user(7));
        desk.gateway.execute(7, ActionType.MARKET_QUOTE, Map.of("symbol", "NVDA"));

        ToolResult quote = desk.gateway.read(
                7,
                ActionType.MARKET_QUOTE,
                Map.of("symbol", "AAPL")
        );
        ToolResult news = desk.gateway.read(
                7,
                ActionType.FINANCIAL_NEWS,
                Map.of("symbol", "SPY")
        );

        assertTrue(quote.isSuccess());
        assertTrue(news.isSuccess());
        assertEquals("NVDA", desk.memory.lastSymbol(7));

        desk.gateway.execute(7, ActionType.MARKET_QUOTE, Map.of("symbol", "MSFT"));
        assertEquals("MSFT", desk.memory.lastSymbol(7));
    }

    @Test
    void backgroundReadStillRequiresTheSignedInUser(@TempDir Path directory) throws Exception {
        MemoryDesk desk = memoryDesk(directory);

        ToolResult result = desk.gateway.read(
                7,
                ActionType.MARKET_QUOTE,
                Map.of("symbol", "NVDA")
        );

        assertFalse(result.isSuccess());
        assertEquals(0, desk.tool.calls);
        assertNull(desk.memory.lastSymbol(7));
    }

    private static MemoryDesk memoryDesk(Path directory) throws Exception {
        DatabaseManager database = new DatabaseManager(
                "jdbc:sqlite:" + directory.resolve("memory.db")
        );
        database.initialize();
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(
                    "INSERT INTO users (id, username, password_hash, created_at) "
                            + "VALUES (7, 'ada', 'hash', '2026-10-29T00:00:00Z')"
            );
        }
        RecordingTool tool = new RecordingTool(
                ToolResult.success("quoted"),
                ActionType.MARKET_QUOTE,
                ActionType.FINANCIAL_NEWS
        );
        ToolManager tools = new ToolManager();
        tools.registerTool(tool);
        SessionManager sessions = new SessionManager();
        ConversationMemory memory = new ConversationMemory(new SQLiteConversationRepository(database));
        AgentController followUp = new AgentController(
                memory,
                new RequestPlanner(),
                tools,
                new ResponseComposer(),
                null
        );
        return new MemoryDesk(
                sessions,
                memory,
                tool,
                new ResearchGateway(sessions, tools, null, followUp)
        );
    }

    private record MemoryDesk(
            SessionManager sessions,
            ConversationMemory memory,
            RecordingTool tool,
            ResearchGateway gateway) {
    }

    private static ResearchGateway gateway(
            User signedInUser,
            AgentTool tool,
            MarketExplanationService explanations) {

        SessionManager sessions = new SessionManager();
        if (signedInUser != null) {
            sessions.startSession(signedInUser);
        }

        ToolManager tools = new ToolManager();
        if (tool != null) {
            tools.registerTool(tool);
        }

        return new ResearchGateway(sessions, tools, explanations);
    }

    private static User user(long id) {
        return new User(id, "ada", "hash", LocalDateTime.now());
    }

    private static MarketMovementContext sampleContext() {
        return new MarketMovementContext(
                new MarketQuote(
                        "NVDA",
                        new BigDecimal("100.00"),
                        new BigDecimal("90.00"),
                        new BigDecimal("11.11")
                ),
                List.of(new NewsArticle(
                        "Chip demand",
                        "https://example.com/nvda",
                        "Wire",
                        "2026-10-28T12:00:00Z",
                        "A short summary.",
                        null,
                        null
                )),
                Instant.parse("2026-10-28T15:00:00Z"),
                null
        );
    }

    private static final class RecordingTool implements AgentTool {

        private final List<ActionType> actions;
        private final ToolResult result;
        private int calls;
        private long lastUserId;

        private RecordingTool(ActionType action, ToolResult result) {
            this(result, action);
        }

        private RecordingTool(ToolResult result, ActionType first, ActionType... rest) {
            ActionType[] supported = new ActionType[rest.length + 1];
            supported[0] = first;
            System.arraycopy(rest, 0, supported, 1, rest.length);
            this.actions = List.of(supported);
            this.result = result;
        }

        @Override
        public String getName() {
            return "recording";
        }

        @Override
        public boolean supports(ActionType candidate) {
            return actions.contains(candidate);
        }

        @Override
        public ToolResult execute(ToolRequest request) {
            calls++;
            lastUserId = request.getUserId();
            return result;
        }
    }
}
