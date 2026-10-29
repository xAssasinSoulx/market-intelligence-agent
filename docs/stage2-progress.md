# Stage 2 progress

The current record is [`stage2-status.md`](stage2-status.md). This file is the earlier milestone log.

Updated October 28, 2026, Toronto time, on branch `feature/gui`.

## Baseline

- Branch at audit start: `feature/llm-integration`, clean except the untracked Stage 2 brief.
- Java: 21.0.12.1. Maven wrapper: 3.9.16.
- `./mvnw clean test` before these changes: 87 tests, 0 failures, 1 skipped (`GeminiLiveSmokeTest` without `RUN_GEMINI_LIVE_TEST`).
- `PortfolioService` lived under `persistence/` while declaring `package com.marketintel.services`. The build already succeeded. The file is now under `services/`.
- No API keys were found in tracked source. `.env.example` contains placeholders and is not ignored. Real `.env` files stay ignored.
- Newest commit before this Stage 2 work: `71ab431` on October 18, 2026. Period 1 (October 20–26, 2026, Toronto) has no commits on any local or `origin` branch.

## Milestone A

Done in commit `3770181` (`refactor: share service wiring for both interfaces`).

- Shared `ApplicationContext` constructs authentication, persistence, and provider tools once.
- `--cli` is explicit. No arguments still start the terminal.
- Launch-mode parsing is unit tested.

## Milestone B

Done in `fea8b85` (`feat(app): bind research and portfolios to the signed-in user`) and `f103405` (`feat(gui): add a Swing window over the existing market services`).

Swing window on the same services:

- Register, login, and logout. Passwords use a masked field and are not copied into status text.
- Dashboard, research, portfolio, watchlist, and movement explanation.
- Research calls `ToolManager` through `ResearchGateway` for the signed-in user only.
- Geography, portfolio exposure, and follow-up are visible and disabled.
- Quote, news, comparison, overlap, sector, and explanation work runs in a `SwingWorker`.
- Logout clears research and explanation text.

Automated coverage added for session isolation, tool routing, missing tools, Gemini failure with evidence retained, and text formatting. A manual window checklist is in `docs/stage2-gui-verification.md`. No demonstration video has been recorded.

## Not done

- UC-15 portfolio exposure
- UC-06 geographic exposure
- UC-16 conversation memory and follow-up
- Five documented design patterns with updated UML
- Stage 2 report PDF or Word file
- Demonstration videos
- Five qualifying commits in each weekly period

## Next

Implement portfolio exposure from real quotes and ETF holdings, with offline tests and a command plus a Swing action. Do not treat share quantity as market value.
