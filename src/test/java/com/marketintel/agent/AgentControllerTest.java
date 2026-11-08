package com.marketintel.agent;

import com.marketintel.model.ActionType;
import com.marketintel.model.ExposureResult;
import com.marketintel.model.ToolRequest;
import com.marketintel.model.ToolResult;
import com.marketintel.persistence.DatabaseManager;
import com.marketintel.persistence.SQLiteConversationRepository;
import com.marketintel.services.ConversationMemory;
import com.marketintel.tools.AgentTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentControllerTest {

    @TempDir
    Path tempDirectory;

    @Test
    void followUpRunsTheSectorToolWithTheStoredSymbol() throws Exception {
        DatabaseManager database = new DatabaseManager(
                "jdbc:sqlite:" + tempDirectory.resolve("agent.db")
        );
        database.initialize();
        insertUser(database, 7);
        RecordingTool tool = new RecordingTool();
        ToolManager tools = new ToolManager();
        tools.registerTool(tool);
        AgentController controller = new AgentController(
                new ConversationMemory(new SQLiteConversationRepository(database)),
                new RequestPlanner(),
                tools,
                new ResponseComposer(),
                null
        );

        controller.handle(7, "Why did QQQ move?");
        String reply = controller.handle(7, "What about its sector exposure?");

        assertEquals(ActionType.ETF_SECTOR_EXPOSURE, tool.lastAction);
        assertEquals("QQQ", tool.lastSymbol);
        assertTrue(reply.contains("FOLLOW-UP"));
        assertTrue(reply.contains("60.00%"));
        assertEquals(
                "QQQ",
                new ConversationMemory(new SQLiteConversationRepository(database)).lastSymbol(7)
        );
    }

    @Test
    void rephrasesTheLastReplyWithoutRunningAnotherTool() throws Exception {
        DatabaseManager database = new DatabaseManager(
                "jdbc:sqlite:" + tempDirectory.resolve("talk.db")
        );
        database.initialize();
        insertUser(database, 8);
        RecordingTool tool = new RecordingTool();
        ToolManager tools = new ToolManager();
        tools.registerTool(tool);
        AgentController controller = new AgentController(
                new ConversationMemory(new SQLiteConversationRepository(database)),
                new RequestPlanner(),
                tools,
                new ResponseComposer(),
                null,
                prompt -> "The last lookup was a sector breakdown, in plain words."
        );

        controller.handle(8, "Why did QQQ move?");
        int callsAfterLookup = tool.calls;
        String reply = controller.handle(8, "say that more simply");

        assertEquals(1, callsAfterLookup);
        assertEquals(callsAfterLookup, tool.calls);
        assertTrue(reply.contains("plain words"));
        assertTrue(reply.contains("not investment advice"));
    }

    private static void insertUser(DatabaseManager database, long id) throws Exception {
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(
                    "INSERT INTO users (id, username, password_hash, created_at) VALUES ("
                            + id + ", 'user" + id + "', 'hash', '2026-10-29T00:00:00Z')"
            );
        }
    }

    private static final class RecordingTool implements AgentTool {
        private ActionType lastAction;
        private String lastSymbol;
        private int calls;

        @Override
        public String getName() {
            return "recording";
        }

        @Override
        public boolean supports(ActionType action) {
            return true;
        }

        @Override
        public ToolResult execute(ToolRequest request) {
            calls++;
            lastAction = request.getAction();
            lastSymbol = request.getArgument("symbol");
            if (request.getAction() == ActionType.MARKET_MOVEMENT) {
                return ToolResult.failure("movement evidence is not loaded in this test");
            }
            return ToolResult.success(new ExposureResult(
                    lastSymbol,
                    Map.of("Technology", new BigDecimal("0.60"))
            ));
        }
    }
}
