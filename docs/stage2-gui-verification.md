# Graphical interface manual check

Automated tests do not open a window. Use this list on macOS after `./mvnw compile exec:java -Dexec.args="--gui"`.

No video is attached. Do not show API keys. A provider error must not print a key.

Updated October 29, 2026. Items already seen on this machine are marked. The rest are still open.

## Launch

- [x] The window opens without further terminal input after the process starts.
- [x] `F1` through `F5`, or `1 HOME` through `5 WHY`, switch screens.
- [ ] The command line runs `QUOTE NVDA`, `WHY NVDA`, `PORT ADD NVDA 2 STOCK`, and `HELP`.
- [x] `GEO QQQ` runs. On the free Finnhub plan it explains the missing fund report and, when Alpha Vantage holdings are available, shows headquarters countries.
- [ ] `./mvnw compile exec:java -Dexec.args="--cli"` still opens the terminal.
- [x] `./mvnw test` passes with no live keys. October 29 result: 167 tests, 0 failures, 1 skipped.

## Account

- [ ] Create an account with a short password and see a validation message. The password is not printed.
- [x] Sign-on does not show a confirmation field. Create account is a separate screen.
- [ ] A wrong password shows "Invalid username or password." and does not open the desk.
- [ ] Log out and confirm research text from the previous user is gone.
- [ ] Log in as a second user and confirm the portfolio and watchlist are empty.

## Home

- [x] After sign-on, home shows market value and the day's move for the signed-in portfolio.
- [x] Each priced position appears as its own chip. An unpriced symbol is named.
- [x] The tape scrolls quotes. Gains and losses use different colors.
- [ ] Headlines appear on the tape when `MARKETAUX_API_TOKEN` is set.

## Research

- [ ] Quote, overview, compare, sector, overlap, and news return provider data for a known symbol.
- [ ] An unknown or blank symbol shows an error, not a fabricated quote.
- [x] Geography does not invent a country when the fund report and the holding profiles are both missing.
- [ ] The window stays responsive while a request is running, and the action buttons stay disabled until it finishes.

## Portfolio and watchlist

- [ ] Add `NVDA`, quantity `2`, type `STOCK`. It remains after restarting the application and logging in again.
- [ ] Reject a blank symbol, a zero quantity, and a non-numeric quantity.
- [ ] Remove the position. Removing a missing symbol does not crash.
- [ ] Add and remove a watchlist symbol.
- [ ] `EXPOSURE` shows sector weights from market value, and names any position that has no price.

## Explanation

- [ ] Explain `NVDA` shows the quote, last-trade time or an explicit unknown, news links, and either a Gemini explanation or a clear unavailable message.
- [ ] The follow-up tile stays N/A.
- [ ] Text does not present the result as investment advice.

## Missing keys

- [ ] Start without `FINNHUB_API_KEY` and with `ALPHA_VANTAGE_API_KEY`. Quotes use the fallback. Overviews report the premium limit when that is what Alpha Vantage returns.
- [ ] Start without `GEMINI_API_KEY` but with quote and news keys. Explain still shows the evidence.
- [ ] A rate-limit message does not contain an API key.
