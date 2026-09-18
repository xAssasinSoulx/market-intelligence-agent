package com.marketintel.agent;

import com.marketintel.model.ActionType;
import com.marketintel.model.ToolRequest;
import com.marketintel.model.ToolResult;

import com.marketintel.tools.AgentTool;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ToolManagerTest {

    @Test
    void registeredToolCanBeFound() {

        ToolManager manager =
                new ToolManager();

        manager.registerTool(
                new FakeTool()
        );

        assertTrue(
                manager.findTool(
                        ActionType.MARKET_QUOTE
                ).isPresent()
        );
    }

    @Test
    void executesSupportedTool() {

        ToolManager manager =
                new ToolManager();

        manager.registerTool(
                new FakeTool()
        );

        ToolResult result =
                manager.execute(
                        new ToolRequest(
                                1,
                                ActionType.MARKET_QUOTE,
                                Map.of(
                                        "symbol",
                                        "NVDA"
                                )
                        )
                );

        assertTrue(
                result.isSuccess()
        );

        assertEquals(
                "executed",
                result.getData()
        );
    }

    @Test
    void unsupportedActionReturnsFailure() {

        ToolManager manager =
                new ToolManager();

        manager.registerTool(
                new FakeTool()
        );

        ToolResult result =
                manager.execute(
                        new ToolRequest(
                                1,
                                ActionType.FINANCIAL_NEWS,
                                Map.of()
                        )
                );

        assertFalse(
                result.isSuccess()
        );
    }

    private static class FakeTool
            implements AgentTool {

        @Override
        public String getName() {
            return "fake";
        }

        @Override
        public boolean supports(
                ActionType action) {

            return action
                    == ActionType.MARKET_QUOTE;
        }

        @Override
        public ToolResult execute(
                ToolRequest request) {

            return ToolResult.success(
                    "executed"
            );
        }
    }
}