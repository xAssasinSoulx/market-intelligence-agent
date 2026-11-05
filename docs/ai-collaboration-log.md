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

## Interaction 005 - Country exposure and secret redaction

### Task

Keep a country breakdown on the free Finnhub plan, and stop provider
errors from showing API keys.

### Input / Instruction

The published Finnhub country report returned HTTP 403. An Alpha
Vantage rate-limit message then printed a live key into the window.
The instruction was to implement a free path and never show a key.

### AI Contribution

The agent corrected the country URL, treated HTTP 403 as a plan limit,
and added a headquarters lookup for at most 25 holdings.
`ProviderText.redact` strips keys and rate-limit notices before display.

### Human Contribution

The project owner found both failures in the running window, rejected
a paid country report, and rotated the exposed Alpha Vantage key.

### Outcome

`GEO` reports headquarters countries and names what was not assigned.
It does not invent a published fund allocation.

## Interaction 006 - Follow-up questions and Gemini speed

### Task

Make a sentence such as "why did microsoft fall" select Microsoft and
return a short, grounded answer.

### Input / Instruction

Keyword routing missed ordinary wording. A later request asked to let
Gemini interpret questions. Timeouts, an HTTP 404 on `gemini-2.5-flash`,
and a command line that treated "did" as a ticker were reported from
the running desk.

### AI Contribution

The agent added conversation memory, an allowlisted planner, a short
`generateBrief` call, the `gemini-3.5-flash-lite` default, and a command
parser that keeps a full "why" sentence as a question.

### Human Contribution

The project owner chose to keep portfolio exposure out of the model,
rejected paying for SentiSense Pro, and confirmed the Microsoft question
in the window after the parser fix.

### Outcome

Direct commands stay deterministic. A sentence can plan one allowed
action and attach a short Gemini note to the tool result. A missing
or rejected model reply falls back to keywords.

## Interaction 007 - Stage 2 diagrams and report

### Task

Update the UML to the implemented system and write the Stage 2 report.

### Input / Instruction

Update the UMLs and README, and create the report from the attached
Stage 2 instructions.

### AI Contribution

The agent rewrote the class, use-case, and sequence diagrams so that
Gemini is only on movement explanation and follow-up, and wrote
`docs/stage2-report.pdf`.

### Human Contribution

The project owner remains responsible for recording the demonstration
videos and for the weekly commits still required in Period 3.

### Outcome

The report describes the code that is in the repository, including
the features that are not finished: videos, Period 1 commits, and
Period 3.