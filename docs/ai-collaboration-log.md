# AI-Human Collaboration Log

## Interaction 001 - Initial Software Architecture

### Task
Design the Java architecture for the Market Intelligence Agent.

### Input / Instruction
Asked ChatGPT to help design a modular architecture based on the
EECS3311 project requirements and the proposed financial-agent concept.

### AI Contribution
The AI proposed:
- an AgentTool interface and ToolManager;
- provider abstractions for external APIs;
- repository abstractions for persistence;
- constructor-based dependency injection;
- separation between deterministic Java logic and LLM behavior.

### Human Contribution
The proposed architecture was reviewed and expanded to include:
- multi-user authentication;
- persistent user-specific memory;
- portfolios and watchlists;
- a Bloomberg-inspired terminal interface;
- SQLite persistence;
- separate PortfolioTool and PortfolioAnalysisTool;
- BigDecimal for financial calculations.

### Outcome
The revised architecture was incorporated into the finalized Stage 1
UML diagrams and selected as the baseline for Stage 2 implementation.

## Interaction 003 - Market Data Provider and Tool Architecture

### Task

Implement external market-data retrieval and integrate it with the
agent tool architecture.

### Input / Instruction

Asked ChatGPT to help implement the Stage 1 market-data design using
a provider abstraction and external financial API.

### AI Contribution

The AI proposed and helped implement:

- `MarketDataProvider`;
- `MarketDataApiClient`;
- `MarketDataException`;
- `MarketQuote`;
- `SecurityOverview`;
- provider-side rate limiting;
- `AgentTool`;
- `ToolManager`;
- `MarketDataTool`;
- `ToolRequest` and `ToolResult`;
- deterministic request validation;
- automated tests using a fake market-data provider.

### Human Contribution

The implementation was reviewed and executed locally. Real Alpha Vantage
requests were tested manually, API throttling behavior was identified,
and the manual test workflow was adjusted to avoid unnecessary API calls.

### Outcome

The application can retrieve real market quotes and security overview
data while preserving the Stage 1 separation between external providers,
deterministic tools, and higher-level agent logic.