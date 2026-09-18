package com.marketintel.agent;

import com.marketintel.model.ActionType;
import com.marketintel.model.ToolRequest;
import com.marketintel.model.ToolResult;

import com.marketintel.tools.AgentTool;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ToolManager {

    private final List<AgentTool> tools =
            new ArrayList<>();

    public void registerTool(
            AgentTool tool) {

        if (tool == null) {
            throw new IllegalArgumentException(
                    "Tool cannot be null."
            );
        }

        tools.add(tool);
    }

    public Optional<AgentTool> findTool(
            ActionType action) {

        return tools.stream()
                .filter(
                        tool ->
                                tool.supports(action)
                )
                .findFirst();
    }

    public ToolResult execute(
            ToolRequest request) {

        if (request == null) {

            return ToolResult.failure(
                    "Tool request cannot be null."
            );
        }

        return findTool(
                request.getAction()
        )
                .map(
                        tool ->
                                tool.execute(
                                        request
                                )
                )
                .orElseGet(
                        () ->
                                ToolResult.failure(
                                        "No tool is available for action: "
                                                + request.getAction()
                                )
                );
    }
}