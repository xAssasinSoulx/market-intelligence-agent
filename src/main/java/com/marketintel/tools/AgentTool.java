package com.marketintel.tools;

import com.marketintel.model.ActionType;
import com.marketintel.model.ToolRequest;
import com.marketintel.model.ToolResult;

public interface AgentTool {

    String getName();

    boolean supports(ActionType action);

    ToolResult execute(ToolRequest request);
}