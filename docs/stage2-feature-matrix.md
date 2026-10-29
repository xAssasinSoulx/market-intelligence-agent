# Stage 2 feature matrix

Status checked against the working tree on `feature/gui` on October 28, 2026. "Implemented" means the behavior runs through the existing services and has either an automated test, a terminal command, or a Swing control that calls that service. It does not mean a live provider call was demonstrated in this session, and it does not mean a video exists.

| ID | Stage 1 use case | Status | Evidence |
| --- | --- | --- | --- |
| UC-01 | Register account | Implemented | `AuthService.register`, terminal register flow, Swing "Create account". `AuthServiceTest`, `SessionWorkspaceTest`. |
| UC-02 | Log in | Implemented | `AuthService.authenticate` and `SessionManager.startSession`. Terminal and Swing. Failed login does not start a session. |
| UC-03 | Log out | Implemented | `SessionManager.endSession`. Not counted as one of the ten nontrivial features. |
| UC-04 | Get market quote | Implemented | `MarketDataTool` action `MARKET_QUOTE`, terminal `/quote`, Swing Research "Quote". Needs `ALPHA_VANTAGE_API_KEY`. |
| UC-05 | Get security overview | Implemented | `MarketDataTool` action `SECURITY_OVERVIEW`, terminal `/overview`, Swing "Overview". |
| UC-06 | Analyze ETF exposure | Partial | Sector exposure is implemented (`/sector`, Swing "Sector", `ExposureCalculator`). Geographic exposure is not implemented. Swing Geography is disabled. `ActionType.ETF_GEOGRAPHIC_EXPOSURE` has no tool. |
| UC-07 | Compare securities | Implemented | `ComparisonTool`, `/compare`, Swing "Compare". |
| UC-08 | Analyze ETF holding overlap | Implemented | `ComparisonTool` action `ETF_OVERLAP`, `/overlap`, Swing "Overlap". Limited to holdings the provider returns. |
| UC-09 | Retrieve financial news | Implemented | `NewsTool` and `MarketauxNewsClient`, `/news`, Swing "News". Needs `MARKETAUX_API_TOKEN`. |
| UC-10 | Explain recent market movement | Implemented, with limits | `MarketMovementTool` plus `MarketExplanationService`. Terminal `/why`, Swing Explain. Evidence remains if Gemini is missing or fails. Trading-session time is shown as unknown. Citation markers are not treated as proof. |
| UC-11 | Add portfolio position | Implemented | `PortfolioService.addPosition`, `/portfolio add`, Swing Portfolio. SQLite, per user. |
| UC-12 | Remove portfolio position | Implemented | `PortfolioService.removePosition`, `/portfolio remove`, Swing Portfolio. |
| UC-13 | View portfolio | Implemented | `PortfolioService.getPortfolio`, `/portfolio`, Swing table. |
| UC-14 | Manage watchlist | Implemented | `WatchlistService` view, add, and remove. Terminal and Swing. |
| UC-15 | Analyze portfolio exposure | Missing | No calculator, tool, command, or enabled Swing action. The portfolio screen says so. |
| UC-16 | Ask contextual follow-up | Missing | No conversation table, planner, or follow-up resolution. Swing follow-up field is disabled. `AgentController`, `RequestPlanner`, `PromptBuilder`, and `ResponseComposer` are empty. |

## Nontrivial features currently working

These exclude register, login, logout, and exit.

1. Market quote
2. Security overview
3. Security comparison
4. ETF sector exposure
5. ETF holding overlap
6. Financial news retrieval
7. Evidence-backed movement explanation
8. Add, remove, and view a persistent portfolio (one feature: portfolio management)
9. Add, remove, and view a persistent watchlist (one feature: watchlist management)

That is nine distinct nontrivial features. Geographic exposure, portfolio exposure, and contextual follow-up are still required and are not counted.

## Design patterns

Not claimed as five completed Stage 2 patterns yet. Existing collaborations worth keeping:

- `AgentTool` plus `ToolManager` dispatch concrete research operations.
- `MarketDataApiClient`, `ETFDataApiClient`, and `MarketauxNewsClient` adapt vendor payloads to provider interfaces.
- `LLMProvider` is implemented by `GeminiLLMClient`.

A later milestone has to show five distinct course patterns in working code, with tests and updated diagrams. Empty agent classes are not patterns and are not features.
