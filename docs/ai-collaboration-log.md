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

## Interaction 004 - Stage 2 desk, exposure, and provider limits

### Task

Bring the working application forward through October 29, 2026: Finnhub quotes, portfolio exposure, country exposure on the free plans, a split sign-on screen, and a home summary with a scrolling tape.

### Input / Instruction

Continue the Stage 2 implementation on the existing Java services. Do not invent country weights. Do not show API keys in the window.

### AI Contribution

Cursor, using Grok 4.7, edited the Java sources and tests for:

- Finnhub quotes and overviews, with Alpha Vantage kept for ETF holdings;
- portfolio value as quantity times price, currencies kept apart;
- country exposure that tries Finnhub's fund report, then headquarters countries of the largest reported holdings;
- redaction of provider error text that repeats a key;
- separate sign-on and account-creation screens;
- a home market-value summary and a colored scrolling tape.

### Human Contribution

The desk was run locally. Finnhub returned HTTP 302 for the wrong country path, then HTTP 403 because the free plan does not include `/etf/country`. Alpha Vantage's daily-limit message included the key on screen. That key was rotated by the owner. The home layout was reviewed from screenshots and sent back for a tighter summary.

### Outcome

The behavior above is in `main` through commit `3a162ce`, with the tighter home layout on `feature/home-desk` (`1f4db1c`). Contextual follow-up is still absent. Token counts and monetary cost for this Cursor session are unavailable.

### Cost

Unavailable. This session did not record a token total or a dollar amount.