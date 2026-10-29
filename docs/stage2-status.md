# Stage 2 working status

Updated October 28, 2026, 10:30 p.m. Toronto time. Branch: `feature/gui`.

This is the current working record. It describes the code that is actually in the branch. It does not count a feature as done from a class name, and it does not claim videos, a finished report, or a live Gemini success from this session.

## In progress now

Gemini explanations. Quote and news already return. On October 28 the Swing desk ran `WHY AAPL` and showed the quote, retrieval time, and three news links, then:

`Unable to communicate with Gemini.`

That sentence was only used when the HTTP call threw an `IOException`. The real cause, including a timeout, was discarded. The client waited 60 seconds. `gemini-3.8-flash` thinks before it answers, and the evidence on screen was retrieved at 22:19:53 local while the failure was still visible at 22:21:33, which fits that limit.

The current code change:

- The request timeout is 120 seconds.
- A timeout is reported as `The request exceeded 120 seconds.`
- Any other connection failure appends the underlying message.
- The Explain screen says the request can take up to 2 minutes.
- The quote and news still remain on screen when Gemini fails.

This has not been retried live against the API in this session. Restart the desk after pulling the change, then run `WHY AAPL` again.

## Done and checked

- Java 21.0.12.1, Maven wrapper 3.9.16.
- Baseline before Stage 2 work: `./mvnw clean test`, 87 tests, 0 failures, 1 skipped.
- Latest full run before this Gemini change: 111 tests, 0 failures, 1 skipped. The skip is `GeminiLiveSmokeTest` unless `RUN_GEMINI_LIVE_TEST=1`.
- `PortfolioService` now lives in `src/main/java/com/marketintel/services/`, matching its package. The old path compiled, but the README's build-blocker claim was already false.
- `ApplicationContext` builds authentication, SQLite, and provider tools once for the terminal and the window.
- Launch: no arguments or `--cli` starts the terminal. `--gui` starts the Swing desk.
- The desk is amber on black, with `1 HOME` through `5 WHY`, `F1` through `F5`, and a command line (`QUOTE`, `DES`, `COMPARE`, `SECTOR`, `OVERLAP`, `NEWS`, `WHY`, `PORT`, `WATCH`, `HELP`).
- Research, portfolio, and watchlist actions use the signed-in user id. A second account does not see the first account's rows in `SessionWorkspaceTest`.
- Network and model calls run off the Swing event thread.
- Geography, portfolio exposure, and follow-up are visible and disabled. They do not return invented numbers.
- `.env.example` has placeholders only. `.gitignore` ignores `.env` and still allows `.env.example`. No API key was found in tracked source.
- A person used the desk on this machine: sign-on worked, `WHY AAPL` loaded Alpha Vantage and Marketaux evidence, and Gemini did not return text.

## Use cases

| ID | Name | Status |
| --- | --- | --- |
| UC-01 | Register | Implemented in terminal and Swing |
| UC-02 | Log in | Implemented. A wrong password does not open a session |
| UC-03 | Log out | Implemented. Not one of the ten nontrivial features |
| UC-04 | Market quote | Implemented. Needs `ALPHA_VANTAGE_API_KEY` |
| UC-05 | Security overview | Implemented |
| UC-06 | ETF exposure | Partial. Sector works. Country exposure does not |
| UC-07 | Compare securities | Implemented |
| UC-08 | ETF holding overlap | Implemented, limited to holdings the provider returns |
| UC-09 | Financial news | Implemented. Needs `MARKETAUX_API_TOKEN` |
| UC-10 | Explain a move | Partial. Evidence works. Gemini was reached and failed on the last live try |
| UC-11 | Add a position | Implemented |
| UC-12 | Remove a position | Implemented |
| UC-13 | View a portfolio | Implemented |
| UC-14 | Watchlist | Implemented |
| UC-15 | Portfolio exposure | Missing |
| UC-16 | Contextual follow-up | Missing |

Nine nontrivial features work: quote, overview, comparison, sector exposure, overlap, news, movement evidence, portfolio management, and watchlist management. Register, login, logout, and exit are not part of that count. Ten is not met until one more real feature exists. The three missing or partial Stage 1 use cases still have to be accounted for even after that.

## Outstanding

These are started or partially present. They are not finished.

- UC-10 live explanation. The failure text is more honest, and the wait is longer. A successful Gemini reply with this key and model has not been recorded.
- UC-06 sector path. Geographic exposure has an action name and a disabled `GEO` control, and no calculator.
- Design-pattern notes. Tool dispatch, provider adapters, and `LLMProvider` exist in the code. Five course patterns are not yet written up with participants, methods, and updated UML.
- The Swing manual checklist in `docs/stage2-gui-verification.md`. The `WHY AAPL` path was exercised. The rest of that list has not been checked off.
- Weekly commits for October 27–November 2. Five code commits are on `feature/gui`. A documentation-only commit does not count. `origin/main` already contains the desk through merge commit `ab26f7b`. The Gemini fix is on `feature/gui` as `bf2019e` and also exists locally on `main` as `5902de6`, which has not been pushed.
- `docs/stage2-commit-summary.md` is behind `feature/gui`. This file is the current record.

## Still missing

- UC-15 portfolio exposure. No market-value weights, no ETF look-through, no `/portfolio exposure`, and the Swing `EXPOS` control stays disabled. Share quantity must not be treated as market value.
- UC-06 geographic exposure. `ETFHolding` has no country field. Listing country and economic exposure are not distinguished because the provider data is not wired.
- UC-16 conversation memory and follow-up. No conversation table. `AgentController`, `RequestPlanner`, `PromptBuilder`, and `ResponseComposer` are empty. The follow-up field is disabled on purpose.
- Five distinct course design patterns documented against working collaborators, with tests where the pattern has behavior worth testing.
- Updated UML for the GUI, Gemini provider, and any new exposure or memory classes. The Stage 1 `.puml` files and PDFs are still the Stage 1 versions.
- `stage2-report.pdf` or `stage2-report.docx` with all seven required sections.
- Demonstration video links and timestamps for every implemented feature, covering both the terminal and the window.
- A clean human pass through unknown symbols, a missing provider, logout, and a second user.
- Period 1 commits. October 20–26 has none. The previous commit on every inspected branch is `71ab431` at `2026-10-18T16:19:35-04:00`. That gap cannot be repaired by later commits.
- Period 3 commits, November 3–9. Not started.

## Commits on `feature/gui`

Pushed to `https://github.com/xAssasinSoulx/market-intelligence-agent/tree/feature/gui`.

| Toronto time | Hash | Counts as a code commit | Subject |
| --- | --- | --- | --- |
| 2026-10-28 21:54 | `3770181a71225ac9e9eda009c9513fde0026fbe9` | Yes | Shared service wiring and the `PortfolioService` move |
| 2026-10-28 22:01 | `fea8b85f8e663ca1b73d159dd08a9ae1c91b7699` | Yes | Session-bound research and portfolios |
| 2026-10-28 22:01 | `f1034052dedb7e04a8fbc2f5e3812880d99d6e91` | Yes | First Swing window |
| 2026-10-28 22:02 | `ce42198` | No. Documentation only | Progress notes |
| 2026-10-28 22:15 | `cefbc92` | Yes | Amber command desk |
| 2026-10-28 22:29 | `bf2019ee02d73f653696543b5e30bc5cff3f31a8` | Yes | Gemini failures name a timeout or connection error |

`main` and `feature/llm-integration` do not contain these commits. Open the `feature/gui` branch on GitHub.

## Next implementation

Portfolio exposure, after the Gemini retry is confirmed or the new error text shows a different cause. The calculation has to use price times quantity, keep currencies separate, and weight ETF holdings by both the position weight and the holding weight. Unknown sectors stay unknown.
