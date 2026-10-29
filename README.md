# Market Intelligence Agent

A Java 21 application for multi-user market research, built for EECS 3311. The same SQLite accounts, portfolios, watchlists, and tool services back both a terminal interface and a Swing window. Quotes, ETF data, news, and Gemini explanations come from live providers when the matching environment variables are set.

## Requirements

- JDK 21
- The included Maven wrapper (`./mvnw`)
- Optional API keys, exported in the shell. See `.env.example` for names only. Do not commit real keys.

```bash
export ALPHA_VANTAGE_API_KEY="your-alpha-vantage-key"
export MARKETAUX_API_TOKEN="your-marketaux-token"
export GEMINI_API_KEY="your-gemini-key"
# optional
export GEMINI_MODEL="gemini-2.0-flash"
export MI_SCROLLBACK=1
```

`MI_SCROLLBACK=1` keeps terminal history instead of clearing the screen. The application reads environment variables directly. It does not load a `.env` file.

## Run

Tests do not call live APIs. The Gemini smoke test runs only when `RUN_GEMINI_LIVE_TEST=1` and `GEMINI_API_KEY` are both set.

```bash
./mvnw clean test
./mvnw compile exec:java -Dexec.args="--cli"
./mvnw compile exec:java -Dexec.args="--gui"
```

With no arguments, the process starts the command-line interface. The window does not need an interactive terminal after launch. Both interfaces use `data/market-intelligence.db`.

Without `ALPHA_VANTAGE_API_KEY`, quote, overview, comparison, sector, and overlap actions are unavailable. Without `MARKETAUX_API_TOKEN`, news is unavailable. Movement analysis needs both. Without `GEMINI_API_KEY`, movement analysis can still show the quote and news, and it says that the explanation is unavailable.

## Command-line actions

```text
/help
/whoami
/quote NVDA
/overview NVDA
/compare NVDA MSFT
/sector QQQ
/overlap QQQ SPY
/news NVDA
/why NVDA
/portfolio
/portfolio add NVDA 10 STOCK
/portfolio remove NVDA
/watchlist
/watchlist add NVDA
/watchlist remove NVDA
/logout
/exit
```

Portfolio asset types are `STOCK`, `ETF`, and `OTHER`.

## Graphical window

After login the window has five sections:

- **Dashboard** shows the signed-in user, position and watchlist counts, and which providers are configured.
- **Research** runs quote, overview, compare, sector exposure, overlap, and news through the existing tools. Geography is disabled because that calculation is not implemented.
- **Portfolio** adds, removes, and lists positions stored for the signed-in user. Exposure analysis is disabled.
- **Watchlist** adds, removes, and lists symbols for the signed-in user.
- **Explain** runs the same evidence-then-Gemini flow as `/why SYMBOL`. Follow-up questions are disabled until conversation memory exists.

Network and model calls run off the Swing event thread. Log out before another person uses the same window. Logging out clears the on-screen research text.

## Current limits

- ETF geographic exposure is not implemented.
- Portfolio exposure is not implemented. A position quantity is a share count, not a market value.
- Contextual follow-up questions are not implemented. `AgentController`, `RequestPlanner`, `PromptBuilder`, and `ResponseComposer` are still empty.
- Provider data can be delayed, incomplete, or rate-limited. News matches are filtered, but a ticker mention is not proof that an article caused a price move. The quote does not include a trading-session timestamp.
- Explanations are not investment advice.

## Layout

Java 21, Maven, SQLite JDBC, BCrypt, Jackson, and JUnit 5. Stage 1 diagrams are in `docs/uml` and `docs/Stage1-pdfs`. Stage 2 progress is tracked in `docs/stage2-progress.md` and `docs/stage2-feature-matrix.md`.
