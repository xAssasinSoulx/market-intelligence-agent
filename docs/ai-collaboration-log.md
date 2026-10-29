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

## Interaction 004 - Provider adapters after the Alpha Vantage refusal

### Task

Keep market data available without a paid Alpha Vantage plan, and make
the provider switch a visible design choice for Stage 2.

### Input / Instruction

Alpha Vantage declined the educational access request and offered a
discount on a paid plan. The instruction was to use free providers
instead, behind one shared data-provider interface with an adapter
class for each API.

### AI Contribution

The AI added `DataProvider` as the shared target and `ProviderRouter`
as the ordered registry. Adapters cover Finnhub, Financial Modeling
Prep, Twelve Data, SentiSense, Marketaux, and Alpha Vantage. Each
adapter reports only the capabilities its free plan is used for.
Registration order selects the provider. Tools keep depending on the
narrower role interfaces.

### Human Contribution

The provider set, the decision not to pay for Alpha Vantage, and the
requirement that the switch demonstrate a design pattern came from
the project owner. Free-plan call budgets still have to be confirmed
on each vendor site at signup.

### Outcome

Quotes, overviews, news, ETF holdings, statements, daily history, and
a sentiment score can be wired by environment variable. Unset keys
leave that capability unavailable. `./mvnw test` does not call the
live APIs.