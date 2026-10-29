# Stage 2 progress

The current record is [`stage2-status.md`](stage2-status.md). This file is the milestone log.

Updated October 29, 2026, Toronto time.

## Baseline

- Audit branch: `feature/llm-integration`.
- Java 21. Maven wrapper 3.9.16.
- `./mvnw test` before the Stage 2 window work: 87 tests, 0 failures, 1 skipped.
- `./mvnw test` on October 29 after the home-screen commit: 167 tests, 0 failures, 1 skipped.
- `PortfolioService` now lives under `services/`, matching its package.
- No API keys are in tracked source. `.env.example` contains placeholders. Real `.env` files stay ignored.
- Period 1 (October 20–26, 2026, Toronto) has no commits. The commit before this work is `71ab431` on October 18, 2026.

## Milestone A

Done in `3770181` (`refactor: share service wiring for both interfaces`).

Shared `ApplicationContext`. `--cli` is explicit. No arguments still start the terminal.

## Milestone B

Done across `fea8b85`, `f103405`, `cefbc92`, `3a162ce`, and `1f4db1c`.

Swing desk on the same services:

- Separate sign-on and account-creation screens. Passwords stay masked.
- Home shows portfolio market value, the day's move, and a chip per position.
- A colored tape scrolls index proxies, portfolio symbols, and headlines.
- Research, portfolio, watchlist, and movement explanation.
- `ResearchGateway` uses the signed-in user id.
- Quote, news, comparison, overlap, sector, geography, exposure, and explanation run off the Swing event thread.

## Milestone C

Done in `fcd3877` and `01ad1a8`.

`/portfolio exposure` and the Swing `EXPOSURE` action value positions as quantity times the latest price. Currencies stay in separate books. ETF positions without a company profile are valued in US dollars with a note. Missing prices are named.

## Milestone D

Partial, in `9716283`.

`/geography` and `GEO` call Finnhub `/etf/country` on `api.finnhub.io`. The free plan returns HTTP 403. The fallback sums headquarters countries for at most 25 of the largest Alpha Vantage holdings. The note states that limit. No country is invented.

## Milestone E

Not done. UC-16 still has no stored conversation and no follow-up resolution.

## Milestone F

Partly done in `bf2019e` and `cc386c7`.

Gemini failures keep the quote and the news and name a timeout when that is the cause. Finnhub quotes carry a last-trade time. Articles published after it are marked. Alpha Vantage quotes have no trade time, and the screens say so.

## Not done

- UC-16 conversation memory and follow-up
- Five documented design patterns with updated UML
- Stage 2 report PDF or Word file
- Demonstration videos
- Period 1 commits. That period is missed
- Period 3 commits. Not started

## Next

Implement contextual follow-up on top of the existing tools, with per-user memory and a clarification when the referent is ambiguous.
