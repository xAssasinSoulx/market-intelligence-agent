# Market Intelligence Agent

A Java 21 research desk for signed-in users. One SQLite database holds accounts, portfolios, and watchlists. The terminal and the Swing window call the same services for quotes, ETF analysis, news, and movement explanations.

Quotes and company profiles come from Finnhub. ETF holdings come from Alpha Vantage. News comes from Marketaux. Written explanations come from Gemini. Each source is used only when its environment variable is set.

This is a market-research tool for EECS 3311. It does not give investment advice.

## Requirements

- JDK 21
- The Maven wrapper in this repository (`./mvnw`)

API keys are optional. The application reads them from the environment. It does not load a `.env` file. `.env.example` lists the names only. Do not commit real keys.

## Configuration

Export the keys you have before launch:

```bash
export FINNHUB_API_KEY="your-finnhub-key"
export ALPHA_VANTAGE_API_KEY="your-alpha-vantage-key"
export MARKETAUX_API_TOKEN="your-marketaux-token"
export GEMINI_API_KEY="your-gemini-key"
export GEMINI_MODEL="gemini-2.0-flash"   # optional
export MI_SCROLLBACK=1                    # optional
```

| Variable | Role |
| --- | --- |
| `FINNHUB_API_KEY` | Quotes, company profiles, and headquarters countries |
| `ALPHA_VANTAGE_API_KEY` | ETF profiles, holdings, sector weights, and overlap. Quote fallback when Finnhub is absent |
| `MARKETAUX_API_TOKEN` | Recent financial news |
| `GEMINI_API_KEY` | Movement explanation written from the quote and the news |
| `GEMINI_MODEL` | Model name. Omit it to use the client default |
| `MI_SCROLLBACK` | Set to `1` to keep terminal history. Otherwise each view clears the screen |
| `RUN_GEMINI_LIVE_TEST` | Set to `1`, with `GEMINI_API_KEY`, to run the live Gemini smoke test |

`./mvnw test` does not call live APIs.

### What each plan actually returns

Finnhub's free plan serves quotes and company profiles for US listings, at 60 requests per minute. A profile includes currency, exchange, headquarters country, and a Finnhub industry label. It has no company description and no separate GICS sector, and the overview says so. Non-US symbols such as `XEQT.TO` are refused, so a TSX position stays unpriced and the exposure report names it. An ETF has no company profile, so an ETF position is valued in US dollars with a note.

Alpha Vantage's free plan serves ETF profiles and holdings, with a limit of 25 requests per day. Its company overview is a premium endpoint. If only `ALPHA_VANTAGE_API_KEY` is set, quotes fall back to Alpha Vantage and overviews return that premium error. A daily-limit response is shown without the API key.

| Missing key | Result |
| --- | --- |
| No `FINNHUB_API_KEY` | Quotes, overviews, comparison, and portfolio exposure use Alpha Vantage when that key is set. Otherwise those actions are unavailable |
| No `ALPHA_VANTAGE_API_KEY` | Sector, overlap, and ETF holdings are unavailable. ETF positions in a portfolio are reported as unknown |
| No `MARKETAUX_API_TOKEN` | News is unavailable. Movement analysis needs both a quote and news |
| No `GEMINI_API_KEY` | Movement analysis still shows the quote and the news, and states that the explanation is unavailable |

## Run

```bash
./mvnw test
./mvnw compile exec:java -Dexec.args="--cli"
./mvnw compile exec:java -Dexec.args="--gui"
```

With no arguments, the process starts the command-line interface. The window does not need an interactive terminal after it opens. Both interfaces use `data/market-intelligence.db`.

## Command line

Sign in, then:

```text
/help
/whoami
/quote NVDA
/overview NVDA
/compare NVDA MSFT
/sector QQQ
/geography QQQ
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

| Command | Result |
| --- | --- |
| `/quote` | Last price, previous close, and percent change |
| `/overview` | Name, exchange, currency, and industry label |
| `/compare` | Difference between two percent changes |
| `/sector` | Provider sector weights for one ETF |
| `/geography` | Country weights for one ETF. See below |
| `/overlap` | Shared holdings of two ETFs |
| `/news` | Recent articles that mention the symbol |
| `/why` | Quote, news, and a Gemini explanation grounded in that evidence |
| `/portfolio exposure` | Sector weights of the signed-in portfolio by market value |

## Window

`./mvnw compile exec:java -Dexec.args="--gui"` opens the desk. Sign in, then use the function keys or the command line at the bottom.

| Key | Screen |
| --- | --- |
| F1 | Home |
| F2 | Research |
| F3 | Portfolio |
| F4 | Watchlist |
| F5 | Movement explanation |

The same screens are labeled `1 HOME` through `5 WHY`. Network and model calls run off the Swing event thread. Log out before another person uses the same window.

The command line accepts the terminal verbs, with or without a leading slash:

```text
QUOTE NVDA
OVERVIEW NVDA
COMPARE NVDA MSFT
SECTOR QQQ
GEO QQQ
OVERLAP QQQ SPY
NEWS NVDA
WHY NVDA
PORT
PORT ADD NVDA 10 STOCK
PORT REMOVE NVDA
EXPOSURE
WATCH ADD NVDA
HELP
LOGOUT
```

## Country and portfolio exposure

`/geography QQQ` and `GEO QQQ` ask Finnhub for the fund's published country weights. The free plan refuses that report. The command then reads the ETF's holdings from Alpha Vantage and the headquarters country of each company from Finnhub, for at most the 25 largest reported holdings. Both keys are required for that fallback.

The weights are headquarters countries of those holdings. A holding with no profile is `Unknown`. Weight the provider did not assign is left unassigned and named in the note. A single stock such as `NVDA` has no holdings list, so it produces no country breakdown.

`/portfolio exposure` and `EXPOSURE` value each signed-in position as quantity times the latest price. Currencies stay in separate books. A position with no price is excluded and named. ETF holdings are reported as look-through when the holding list is available.

## Limits

- Provider data can be delayed, incomplete, or rate-limited.
- A news match means the ticker was associated with the article. It does not show that the article caused the price move.
- Finnhub quotes include a last-trade time. Articles published after that time are marked. Alpha Vantage quotes have no trade time, and the screens say so.
- Contextual follow-up is not available. `AgentController`, `RequestPlanner`, `PromptBuilder`, and `ResponseComposer` are unused.
- Explanations are research notes, not a recommendation to buy or sell.

## Layout

Java 21, Maven, SQLite JDBC, BCrypt, Jackson, and JUnit 5.

```text
src/main/java/com/marketintel/
  auth/            accounts and sessions
  persistence/     SQLite repositories
  providers/       Finnhub, Alpha Vantage, Marketaux, Gemini
  services/        validation and exposure arithmetic
  tools/           one tool per research action
  gui/             Swing desk
  ui/              terminal
docs/uml/          Stage 1 diagrams
docs/Stage1-pdfs/  rendered Stage 1 diagrams
```
