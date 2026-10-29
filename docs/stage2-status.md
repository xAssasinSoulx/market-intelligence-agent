# Stage 2 working status

Updated October 29, 2026, Toronto time. Recorded from `main` at `00e4325`, plus the home-screen commit `1f4db1c` on `feature/home-desk`.

This file describes code that is in the repository. A class name is not a finished feature. Videos, the Stage 2 report, and a recorded live Gemini success are not claimed.

## Done and checked

- Java 21 and the Maven wrapper.
- `./mvnw test` on `feature/home-desk` after `1f4db1c`: 167 tests, 0 failures, 1 skipped. The skip is `GeminiLiveSmokeTest` unless `RUN_GEMINI_LIVE_TEST=1`.
- `ApplicationContext` builds authentication, SQLite, and provider tools once for the terminal and the window.
- Launch: no arguments or `--cli` starts the terminal. `--gui` starts the Swing desk.
- Sign-on and account creation are separate screens. The confirmation field is only on the create screen.
- The desk is amber on black, with `1 HOME` through `5 WHY`, `F1` through `F5`, and a command line.
- After sign-on, home shows market value and the day's move from the previous close, then one chip per position. A position with no price is named and left out of the total.
- A scrolling tape lists SPY, QQQ, DIA, AAPL, MSFT, the portfolio symbols, and a few SPY headlines. Gains are green and losses are red. The tape refreshes about once a minute and runs off the event thread for the network calls.
- Quotes and overviews use Finnhub when `FINNHUB_API_KEY` is set. Alpha Vantage remains the ETF holdings source and the quote fallback.
- Portfolio exposure is quantity times the latest price. Currencies stay in separate books. ETF holdings are look-through when the list is available.
- Country exposure asks Finnhub for the fund report. The free plan answers HTTP 403. The command then groups Alpha Vantage holdings by the headquarters country on the Finnhub company profile, for at most the 25 largest holdings. Unassigned weight is not filled in.
- Provider error text does not repeat an API key. Alpha Vantage's daily-limit notice is replaced with a short sentence.
- Research calls use the signed-in user id. Network and model calls run off the Swing event thread.
- `.env.example` has placeholders only. Real `.env` files stay ignored.

## Use cases

| ID | Name | Status |
| --- | --- | --- |
| UC-01 | Register | Implemented in the terminal and in Swing |
| UC-02 | Log in | Implemented. A wrong password does not open a session |
| UC-03 | Log out | Implemented. Not one of the ten nontrivial features |
| UC-04 | Market quote | Implemented. Finnhub for US listings. Alpha Vantage is the fallback |
| UC-05 | Security overview | Implemented. Finnhub profile: currency, exchange, headquarters country, industry label. No GICS sector and no description |
| UC-06 | ETF exposure | Partial. Sector weights are the provider allocation. Country weights are headquarters countries of the largest reported holdings when the fund report is refused |
| UC-07 | Compare securities | Implemented |
| UC-08 | ETF holding overlap | Implemented, limited to holdings the provider returns |
| UC-09 | Financial news | Implemented. Needs `MARKETAUX_API_TOKEN` |
| UC-10 | Explain a move | Partial. Evidence works. Finnhub quotes show a last-trade time, and later articles are marked. A successful Gemini reply has not been recorded in this update |
| UC-11 | Add a position | Implemented |
| UC-12 | Remove a position | Implemented |
| UC-13 | View a portfolio | Implemented |
| UC-14 | Watchlist | Implemented |
| UC-15 | Portfolio exposure | Implemented. Price times quantity. Missing prices are named |
| UC-16 | Contextual follow-up | Missing |

Eleven nontrivial features run: quote, overview, comparison, sector exposure, country exposure with the limit above, overlap, news, movement evidence, portfolio management, portfolio exposure, and watchlist management. Register, login, logout, and exit are not part of that count.

## Outstanding

- UC-10. The timeout message is specific and the wait is 120 seconds. A live Gemini answer with the current key has not been saved as evidence.
- UC-06. The free Finnhub plan does not serve `/etf/country`. The headquarters fallback is labeled as such. It is not the fund's published country exposure and it is not revenue by country.
- Five course design patterns are present as collaborations in the code and are not yet written up with participants, methods, and updated UML.
- The Swing checklist in `docs/stage2-gui-verification.md` has been updated. It has not been fully checked off by a person after the October 29 home-screen change.
- Demonstration videos and the seven-section Stage 2 report are not in the repository.

## Still missing

- UC-16 conversation memory and follow-up. `AgentController`, `RequestPlanner`, `PromptBuilder`, and `ResponseComposer` are empty. The follow-up tile stays marked N/A.
- Updated UML for the GUI, Finnhub client, portfolio book, and country path. The Stage 1 `.puml` files are still the Stage 1 versions.
- `stage2-report.pdf` or `stage2-report.docx`.
- Demonstration video links and timestamps for every implemented feature, covering the terminal and the window.
- Period 1 commits. October 20–26, 2026 has none. The previous commit before this Stage 2 work is `71ab431` at `2026-10-18T16:19:35-04:00`. Later commits do not repair that gap.
- Period 3 commits, November 3–9, 2026. Not started.

## Next implementation

Contextual follow-up: per-user conversation context, a deterministic plan for a supported action, and one real tool call from a follow-up such as "What about its sector exposure?"
