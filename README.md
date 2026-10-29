# Market Intelligence Agent

A Java 21 application for multi-user market research, built for EECS 3311. The same SQLite accounts, portfolios, watchlists, and tool services back both a terminal interface and a Swing window. Quotes and security overviews come from Finnhub, ETF holdings from Alpha Vantage, news from Marketaux, and explanations from Gemini, each only when its environment variable is set.

## Requirements

- JDK 21
- The included Maven wrapper (`./mvnw`)
- Optional API keys, exported in the shell. See `.env.example` for names only. Do not commit real keys.

```bash
export FINNHUB_API_KEY="your-finnhub-key"
export ALPHA_VANTAGE_API_KEY="your-alpha-vantage-key"
export MARKETAUX_API_TOKEN="your-marketaux-token"
export GEMINI_API_KEY="your-gemini-key"
# optional
export GEMINI_MODEL="gemini-2.0-flash"
export MI_SCROLLBACK=1
```

`MI_SCROLLBACK=1` keeps terminal history instead of clearing the screen. The application reads environment variables directly. It does not load a `.env` file.

Finnhub's free plan serves quotes and company profiles for US listings at 60 requests per minute. Non-US symbols such as `XEQT.TO` are refused on that plan, so a TSX position stays unpriced and the exposure report says so. Its profile has a currency, an exchange, and an industry label, but no company description and no separate GICS sector; the overview says so. ETFs have no company profile on Finnhub, so an ETF position is placed in US dollars with a note. Alpha Vantage's free plan still serves ETF profiles and holdings, but its company overview is now a premium endpoint and the plan allows 25 requests per day. If only `ALPHA_VANTAGE_API_KEY` is set, quotes fall back to Alpha Vantage and overviews return the premium error.

## Run

Tests do not call live APIs. The Gemini smoke test runs only when `RUN_GEMINI_LIVE_TEST=1` and `GEMINI_API_KEY` are both set.

```bash
./mvnw clean test
./mvnw compile exec:java -Dexec.args="--cli"
./mvnw compile exec:java -Dexec.args="--gui"
```

With no arguments, the process starts the command-line interface. The window does not need an interactive terminal after launch. Both interfaces use `data/market-intelligence.db`.

Without `FINNHUB_API_KEY`, quote, overview, comparison, and portfolio exposure fall back to Alpha Vantage if that key is set, and are otherwise unavailable. Without `ALPHA_VANTAGE_API_KEY`, sector and overlap are unavailable, and ETF positions in a portfolio are reported as unknown. Without `MARKETAUX_API_TOKEN`, news is unavailable. Movement analysis needs quotes and news. Without `GEMINI_API_KEY`, movement analysis can still show the quote and news, and it says that the explanation is unavailable.

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
/portfolio exposure
/watchlist
/watchlist add NVDA
/watchlist remove NVDA
/logout
/exit
```

Portfolio asset types are `STOCK`, `ETF`, and `OTHER`.

## Graphical window

`./mvnw compile exec:java -Dexec.args="--gui"` opens an amber-on-black desk. It does not need an interactive terminal after launch. Sign on, then use the function keys or the command line at the bottom.

Function keys: `F1` home, `F2` research, `F3` portfolio, `F4` watchlist, `F5` movement explanation. The same screens are labeled `1 HOME` through `5 WHY`.

The command line accepts the same verbs as the terminal, with or without a leading slash:

```text
QUOTE NVDA
OVERVIEW NVDA
COMPARE NVDA MSFT
SECTOR QQQ
OVERLAP QQQ SPY
NEWS NVDA
WHY NVDA
PORT
PORT ADD NVDA 10 STOCK
PORT REMOVE NVDA
WATCH ADD NVDA
HELP
LOGOUT
```

`GEO` and `EXPOSURE` are recognized and report that those calculations are not implemented. Network and model calls run off the Swing event thread. Log out before another person uses the same window.

## Current limits

- ETF geographic exposure is not implemented.
- Portfolio exposure is not implemented. A position quantity is a share count, not a market value.
- Contextual follow-up questions are not implemented. `AgentController`, `RequestPlanner`, `PromptBuilder`, and `ResponseComposer` are still empty.
- Provider data can be delayed, incomplete, or rate-limited. News matches are filtered, but a ticker mention is not proof that an article caused a price move. The quote does not include a trading-session timestamp.
- Explanations are not investment advice.

## Layout

Java 21, Maven, SQLite JDBC, BCrypt, Jackson, and JUnit 5. Stage 1 diagrams are in `docs/uml` and `docs/Stage1-pdfs`.
