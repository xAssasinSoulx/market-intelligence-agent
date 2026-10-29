# Graphical interface manual check

Automated tests do not open a window. Use this list on macOS after `./mvnw compile exec:java -Dexec.args="--gui"`.

No video is attached. Record only after a person has seen each step succeed, and do not show API keys.

## Launch

- [ ] The window opens without typing in a terminal after the process starts.
- [ ] `./mvnw compile exec:java -Dexec.args="--cli"` still opens the terminal interface.
- [ ] `./mvnw clean test` passes with no live keys.

## Account

- [ ] Create an account with a short password and see a validation message. The password is not printed.
- [ ] Create a valid account, then log in.
- [ ] A wrong password shows "Invalid username or password." and does not open the dashboard.
- [ ] Log out and confirm research text from the previous user is gone.
- [ ] Log in as a second user and confirm the portfolio and watchlist are empty.

## Research

- [ ] Quote, overview, compare, sector, overlap, and news return provider data for a known symbol.
- [ ] An unknown or blank symbol shows an error, not a fabricated quote.
- [ ] Geography stays disabled.
- [ ] The window stays responsive while a request is running, and the action buttons stay disabled until it finishes.

## Portfolio and watchlist

- [ ] Add `NVDA`, quantity `2`, type `STOCK`. It remains after restarting the application and logging in again.
- [ ] Reject a blank symbol, a zero quantity, and a non-numeric quantity.
- [ ] Remove the position. Removing a missing symbol does not crash.
- [ ] Add and remove a watchlist symbol.
- [ ] Analyze exposure stays disabled.

## Explanation

- [ ] Explain `NVDA` shows the quote, retrieval time, unknown trading session, news links, and either a Gemini explanation or a clear unavailable message.
- [ ] The follow-up field stays disabled.
- [ ] Text does not present the result as investment advice.

## Missing keys

- [ ] Start without `ALPHA_VANTAGE_API_KEY` and confirm quote reports that the provider is unavailable.
- [ ] Start without `GEMINI_API_KEY` but with both data keys and confirm `/why` or Explain still shows evidence.
