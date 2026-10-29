# Stage 2 feature matrix

Checked on October 29, 2026 against `main` and `feature/home-desk`. "Implemented" means the behavior runs through the shared services and has a terminal command or a Swing control, plus tests where the arithmetic or routing can be checked offline. It does not mean a video exists.

| ID | Stage 1 use case | Status | Evidence |
| --- | --- | --- | --- |
| UC-01 | Register account | Implemented | `AuthService.register`. Terminal flow. Swing "Create account" on its own screen. `AuthServiceTest`. |
| UC-02 | Log in | Implemented | `AuthService.authenticate`, `SessionManager.startSession`. The sign-on screen has no confirmation field. A failed login does not start a session. |
| UC-03 | Log out | Implemented | `SessionManager.endSession`. Not one of the ten nontrivial features. |
| UC-04 | Get market quote | Implemented | `MarketDataTool`, `/quote`, Swing Research. `FINNHUB_API_KEY`, with Alpha Vantage as fallback. |
| UC-05 | Get security overview | Implemented | `MarketDataTool` action `SECURITY_OVERVIEW`, `/overview`. Finnhub industry label is shown as the sector, and the overview says so. |
| UC-06 | Analyze ETF exposure | Partial | Sector: `/sector`, `ExposureCalculator`. Country: `/geography`, `GEO`. Fund report when Finnhub allows it. Otherwise headquarters countries of the largest holdings. `HoldingCountryResolver`, `ETFExposureToolTest`. |
| UC-07 | Compare securities | Implemented | `ComparisonTool`, `/compare`. |
| UC-08 | Analyze ETF holding overlap | Implemented | `ComparisonTool` action `ETF_OVERLAP`, `/overlap`. Limited to holdings the provider returns. |
| UC-09 | Retrieve financial news | Implemented | `NewsTool`, `MarketauxNewsClient`, `/news`. Needs `MARKETAUX_API_TOKEN`. |
| UC-10 | Explain recent market movement | Implemented, with limits | `MarketMovementTool`, `MarketExplanationService`, `/why`. Evidence remains if Gemini is missing or fails. Finnhub last-trade time is shown. Citation markers are not treated as proof. |
| UC-11 | Add portfolio position | Implemented | `PortfolioService.addPosition`, `/portfolio add`. SQLite, per user. |
| UC-12 | Remove portfolio position | Implemented | `PortfolioService.removePosition`, `/portfolio remove`. |
| UC-13 | View portfolio | Implemented | `PortfolioService.getPortfolio`, `/portfolio`, home summary, Swing portfolio screen. |
| UC-14 | Manage watchlist | Implemented | `WatchlistService` view, add, and remove. |
| UC-15 | Analyze portfolio exposure | Implemented | `PortfolioExposureCalculator`, `PortfolioAnalysisTool`, `/portfolio exposure`, Swing `EXPOSURE`. `PortfolioBook` prices the home summary. Tests cover missing prices and the day move. |
| UC-16 | Ask contextual follow-up | Missing | No conversation table. `AgentController`, `RequestPlanner`, `PromptBuilder`, and `ResponseComposer` are empty. The follow-up tile is N/A. |

## Nontrivial features currently working

These exclude register, login, logout, and exit.

1. Market quote
2. Security overview
3. Security comparison
4. ETF sector exposure
5. ETF country exposure, limited to the provider report or headquarters countries of reported holdings
6. ETF holding overlap
7. Financial news retrieval
8. Evidence-backed movement explanation
9. Persistent portfolio management
10. Portfolio exposure by market value
11. Persistent watchlist management

## Design patterns

Not yet claimed as five completed, documented Stage 2 patterns. Collaborations already in the code:

- `AgentTool` and `ToolManager` dispatch research operations.
- `FinnhubMarketDataClient`, `ETFDataApiClient`, and `MarketauxNewsClient` adapt vendor payloads to provider interfaces.
- `LLMProvider` is implemented by `GeminiLLMClient`.
- `ApplicationContext` constructs the shared services for both interfaces.

A later pass has to name five distinct course patterns against working methods, with tests and updated diagrams.
