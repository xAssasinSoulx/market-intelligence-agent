# Market Intelligence Agent

A Java 21 terminal application for multi-user financial research, developed for EECS3311 Software Design. It combines deterministic tools, Alpha Vantage data, and SQLite persistence. Natural-language and LLM integration remain future work.

## Current status

Source reviewed at [`f7a1dae`](https://github.com/xAssasinSoulx/market-intelligence-agent/commit/f7a1dae55ba8b80c8eb222587b424c70b173456e) (October 2, 2026 UTC). Recent commits add ETF tools, terminal layout improvements, security comparison, financial news, and ticker relevance filtering.

**Build blocker:** `Main.java` imports `com.marketintel.services.PortfolioService`, but the class declares `com.marketintel.persistence`. `PortfolioServiceTest` also references the services package without importing the persistence class. Resolve these references before building.

“Implemented” means the source contains the feature and terminal integration; it does not certify a passing build or successful live API calls.

| Capability | Status | Current scope |
| --- | --- | --- |
| Multi-user registration and authentication | Implemented | SQLite users, BCrypt passwords, login/logout sessions. |
| Persistent portfolios and watchlists | Implemented | Per-user view, add, and remove operations stored in SQLite. |
| Market quote retrieval | Implemented | `/quote`; Alpha Vantage quotes and price changes. |
| Security information retrieval | Implemented | `/overview`; provider-supplied security information. |
| ETF sector and geographic exposure | Partial | `/sector` displays sector weights; geographic exposure is missing. |
| ETF holding overlap | Implemented | `/overlap`; shared holdings and sum of the smaller weight for each shared symbol. |
| Security comparison | Implemented | `/compare`; two quotes and difference in percentage change. |
| Financial news retrieval | Implemented | `/news`; article metadata, summaries, and ticker sentiment, filtered for an explicit matching ticker in provider metadata. |
| Market-movement explanations | Missing | Quotes and news exist, but no explanation workflow. |
| Portfolio exposure analysis | Missing | Positions persist, but no portfolio-wide exposure calculation or command. |
| Persistent conversation history | Missing | No conversation repository or database table. |
| Context-aware follow-up questions | Missing | Agent orchestration classes are empty; natural-language input is not connected. |
| Bloomberg-inspired terminal interface | Implemented | Styled headers, tables, sections, authentication screens, and command views. |

## Setup

Requires JDK 21, Maven, and an Alpha Vantage API key for external-data commands. After resolving the build blocker:

```bash
export ALPHA_VANTAGE_API_KEY="your-key"
mvn test
mvn compile exec:java
```

The application creates `data/market-intelligence.db`. Without an API key, local account, portfolio, and watchlist functionality remains available, but external-data tools are not registered. Provider limits, endpoint access, and data availability apply; quotes are not guaranteed to be real-time. ETF overlap reflects only returned holdings. News filtering checks ticker metadata, not semantic relevance.

## Commands

```text
/help
/whoami
/quote NVDA
/overview NVDA
/sector QQQ
/overlap QQQ SPY
/compare NVDA MSFT
/news NVDA
/portfolio
/portfolio add NVDA 10 STOCK
/portfolio remove NVDA
/watchlist
/watchlist add NVDA
/watchlist remove NVDA
/logout
/exit
```

Portfolio asset types: `STOCK`, `ETF`, `OTHER`.

## Remaining work

- Resolve the package references and verify the build and tests on Java 21.
- Connect natural-language planning, prompt building, response composition, and an LLM provider.
- Add persistent conversation history and context-aware follow-ups.
- Implement geographic ETF exposure, portfolio-wide exposure, and market-movement explanations.
- Reconcile Stage 1 design documents with implemented workflows.

## Technology and design

Java 21, Maven, SQLite JDBC, BCrypt, Jackson, JUnit 5, and Alpha Vantage. Tests cover authentication, local data services, parsing, tool dispatch, providers, and calculations. Design materials are in [`docs/Stage1-Design.pdf`](docs/Stage1-Design.pdf), [`docs/uml`](docs/uml), and [`docs/Stage1-pdfs`](docs/Stage1-pdfs).
