# EECS 3311 Stage 2 | Cursor Agent Implementation Brief

> **Role:** You are the principal implementation and verification agent for an existing **Java 21, Maven, SQLite, Gemini-powered Market Intelligence Agent**. Your assignment is to bring the *actual repository* into compliance with the **EECS 3311 Course Project Stage 2: AI Human Collaborative Implementation** requirements, while preserving working features and documenting all changes honestly.
>
> **Start date for this brief:** October 28, 2026 (Toronto). **Submission deadline:** November 10, 2026.
>
> **Repository:** `https://github.com/xAssasinSoulx/market-intelligence-agent`
>
> **Do not start by rewriting the project. Audit the checked-out branch and source code first.** The historical findings below are starting points, not a substitute for inspecting the current working tree.

---

## 0. Operating instructions: how to work

1. Open the actual repository workspace in Cursor. Inspect `git status`, `git branch --show-current`, `git log --all`, `pom.xml`, `README.md`, `.gitignore`, `src`, `docs/Stage1-Design.pdf`, `docs/uml`, and existing tests. Read the actual Stage 1 specifications and do not rely on this brief alone for omitted details. Read any additional project instructions attached by the user.
2. **Immediately run a baseline build:** `./mvnw clean test` and `./mvnw clean compile`. Record the actual outcome. If this fails, fix the genuine issue first rather than silently bypassing tests. The repository was observed with `src/main/java/com/marketintel/persistence/PortfolioService.java` declaring `package com.marketintel.services;`; check whether this misplaced source path needs moving to `src/main/java/com/marketintel/services/PortfolioService.java`. Verify before changing.
3. Make an evidence-based checklist of each required Stage 2 deliverable and all 16 Stage 1 use cases. Mark each **Implemented / Partial / Missing / Changed** with supporting files, tests and runnable steps. Never claim completion from class names or interfaces alone.
4. Work incrementally in feature branches and small, substantive commits. Do not write over uncommitted user changes. Do not force-push, rewrite shared history, merge to `main`, or perform irreversible cleanup without authorization. Request approval when an operation needs credentials, involves destructive changes, or needs human-recorded video.
5. Use the existing services, providers and business logic for **both GUI and CLI**. Avoid duplicate financial calculations, authentication logic, provider calls or portfolio state. Prefer constructor injection and clear interfaces. Preserve Java 21 compatibility and the existing Maven wrapper.
6. Before every commit, run applicable offline tests and compilation. Keep all Maven tests independent of paid/live APIs; live smoke tests must be explicitly opt-in. Make genuinely meaningful commits with descriptive messages. Never fabricate, artificially split trivial changes, or backdate commits to satisfy a grading rule.
7. **Do the work.** Don't stop at proposals, mockups, unused classes, TODO methods, placeholder responses, or a report claiming features exist. If a task is blocked, clearly report why and deliver as much working code and verified evidence as possible.
8. After each meaningful milestone, report: files changed, user-visible functionality, commands run, passing/failing tests, commits/branch, remaining issues, and the next highest-priority task. Continue to the next task unless blocked or the user has asked you to stop.
9. Distinguish code that has actually been compiled and tested from code merely written. Keep a project status/tracking file in `docs/` so the user can review progress at any point.
10. Treat **all secrets, user profiles, portfolios, conversation history and financial data** carefully. Never upload or log keys, session credentials, private account data or unredacted sensitive content. Do not send private portfolio or conversation data to third-party LLM APIs by default without an explicit privacy decision from the user.

---

## 1. Authoritative Stage 2 rules and deadlines

The user's screenshots of the **five-page EECS3311 Course Project Stage 2 Instructions** specify the following. Treat the original assignment document as authoritative if a precise detail requires confirmation.

### 1.1 Dates

- Implementation period: **October 20 through November 10, 2026**.
- **Submission due: November 10, 2026**.
- GitHub weekly code-commit reporting periods, **Toronto local time**, with **at least 5 meaningful code commits in each**:
  - **Period 1: October 20–26, 2026**: minimum 5.
  - **Period 2: October 27–November 2, 2026**: minimum 5.
  - **Period 3: November 3–9, 2026**: minimum 5.
- A failed period incurs a **single five-point Stage 2 deduction**, not five points per missing commit or per deficient period, according to the provided screenshots. Extra commits in one period cannot compensate for another.
- A meaningful code commit must represent actual implementation or test progress, e.g. working functionality, defect repair, refactoring or relevant tests. **Documentation-only/formatting-only/empty commits or artificial splitting of a trivial change do not qualify.** Push commits to the public repository during the relevant reporting period.
- **IMPORTANT:** At the time of the October 28 audit, the newest commit visible on the inspected `feature/llm-integration` history was **October 18, 2026**. This suggests no qualifying commits for Period 1 in that branch's visible history. **Audit all branches and local/unpushed commits before making a final claim. Do not falsify dates or pretend late commits count.** Report the status honestly.

### 1.2 Functional requirements

- **Both a GUI and a CLI must work** and expose the application's major functionality.
- At least **10 distinct, nontrivial, meaningful features** must work. Login, logout, exit, about and change-password-like simple operations **do not count toward that minimum**.
- **All Stage 1 proposed features must be accounted for**. Meeting the minimum of 10 does not permit silently omitting other originally promised features. Any addition, replacement, removal or substantial modification must be disclosed and justified.
- Implement at least **five distinct design patterns covered by the course**. Each must solve a real design problem and be evident in functioning code. Repeated uses of the same pattern count as one distinct pattern. Naming a class after a pattern is insufficient.
- Integrate at least **one actual LLM model** into the working application.
- Demonstrate **meaningful AI-agent behavior** such as reasoning, planning, tool use, retrieval, memory, decision-making or multi-step task execution. **Simply sending a prompt to an LLM and printing its reply is insufficient.**
- No demo-only hard-coded results, fake backend integrations, unconnected GUI controls or empty method placeholders should be presented as complete.

### 1.3 Required Stage 2 report

Submit **`stage2-report.pdf` or `stage2-report.docx`**, in the **same public GitHub repository** (a report alone or source-code ZIP does not satisfy source submission). Report must describe the actual implementation and have these seven sections:

1. **Project overview and implementation status**: what the application does, intended users, AI/LLM integration, working functionality and honest limitations.
2. **Feature-to-implementation mapping**: every Stage 1 feature and every new feature, status/changes, implementation files/classes, their responsibilities, important methods, design patterns, and video link/timestamp; describe the path from user input to results; label deterministic vs AI vs hybrid features.
3. **Implemented design patterns**: at least five distinct patterns with the design problem, roles/classes/interfaces, source files and methods, why the implementation is genuinely that pattern, benefit and Stage 1 correspondence or changes. Include updated UML class diagrams, and update affected use-case/sequence diagrams when the Stage 1 versions are inaccurate.
4. **Changes and rationale**: consolidated **Original design → Implemented change → Reason → Impact** for all new/replaced/removed/substantially modified features, classes and patterns; trace affected use cases and methods.
5. **AI tools and collaboration log**: AI platform/tool, model, implementation tasks, example interactions/human reviews, **total token cost and monetary cost** for each, accurately marked as unknown/unavailable if not measurable.
6. **Development and verification summary**: weekly commit table with direct commit links, builds/tests run, defects discovered/fixed, remaining limitations. Stage 2 calls for a working application and basic verification; complete deterministic/behavioral testing and KUMA agent validation remain part of Stage 3.
7. **Demonstration index**: feature-by-feature list linking to a recording or clearly identified segment/timestamp.

### 1.4 Demonstration videos

Every implemented feature, including new or replacement ones, **must have a working application video demonstration**. Either one recording per feature or combined videos with clearly labeled feature segments and direct timestamps are allowed.

Each demo must identify the **feature ID and name**, show how it is invoked and required input, show it running in the actual application and its output, briefly explain its purpose and AI involvement if applicable, and ideally show relevant error/alternative behavior. **Collectively, the videos must demonstrate both GUI and CLI.** Slides, screenshots, mocked output or textual descriptions do not substitute for live application footage. Ensure links are viewable by instructors without access requests. **Never fake a recording or invent a link; if the agent cannot screen-record, prepare scripts and mark human recording as pending.** Avoid exposing API keys, passwords, private records or secrets in videos.

### 1.5 Security and reproducibility

- Never commit/push secrets, including Gemini, Alpha Vantage or Marketaux API keys.
- Commit a safe `.env.example` or equivalent with **placeholders only**, maintain `.gitignore` rules for private files, and document local configuration.
- `README.md` must cover prerequisites, dependencies, launch commands for **GUI and CLI**, API-key setup, and reproducible examples.
- Submit all source code, build/dependency files, safe example configuration and runtime/sample data needed to reproduce the app in the public repository.
- If keys were ever committed previously, merely adding `.gitignore` is insufficient: notify the user to revoke/rotate immediately and appropriately remove them from history with their approval.

---

## 2. Current project baseline: audit before changing

### 2.1 Known architecture

- **Language/build:** Java 21, Maven wrapper, JUnit 5, Jackson.
- **Persistence/security:** SQLite JDBC, BCrypt, session manager, per-user portfolio/watchlist repositories.
- **External providers:** Alpha Vantage for quotes/security and ETF data; Marketaux for recent financial news; Gemini REST API via `GeminiLLMClient`/`LLMProvider` for evidence-based explanations.
- **Existing UI:** Bloomberg-inspired ANSI `TerminalUI` with commands; `MI_SCROLLBACK=1` disables destructive fullscreen clearing and permits longer output.
- **Tool architecture:** `AgentTool`, `ToolManager`, `ToolRequest`, `ToolResult`, provider interfaces, validators and calculators.
- **Known working CLI paths from prior human runs:** `/quote`, `/overview`, `/compare`, `/sector`, `/overlap`, `/news`, `/why`, portfolio CRUD, watchlist CRUD and login/logout. **Re-verify**, because third-party free-tier APIs and source state can change.
- **Known placeholder classes as of inspected branch:** `agent/AgentController.java`, `RequestPlanner.java`, `PromptBuilder.java`, `ResponseComposer.java` were empty; Stage 1 UML anticipated functional versions. Either implement meaningful controller/planner/memory behavior or accurately explain any rearchitecture. Do not count empty classes as implemented.
- **Known stale documentation:** `README.md` still describes missing LLM integration and a historic build issue; update to current reality.
- **Known Stage 1 diagrams:** `docs/uml/use-case-diagram.puml`, `class-diagram-agent.puml`, `class-diagram-domain.puml`, `class-diagram-persistence.puml`, and sequence diagrams for market quote, ETF analysis, market movement, portfolio analysis and contextual follow-up.
- **No GUI identified** in inspected source tree; confirm by searching project before writing one.

### 2.2 Stage 1 feature inventory: account for all 16

Inspect Stage 1 report and diagrams to confirm exact wording and behavior; use this as the working crosswalk.

| ID | Stage 1 use case | Current known state / required verification |
|---|---|---|
| UC-01 | Register account | Appears implemented; confirm persistence, validation, GUI and CLI flow |
| UC-02 | Log in | Appears implemented; confirm session isolation and GUI/CLI |
| UC-03 | Log out | Appears implemented; do not count toward 10 meaningful features |
| UC-04 | Get market quote | Appears implemented, Alpha Vantage |
| UC-05 | Get security overview | Appears implemented |
| UC-06 | Analyze ETF exposure | **Sector exists; geographic exposure needs verification/implementation** |
| UC-07 | Compare securities | Appears implemented |
| UC-08 | Analyze ETF holding overlap | Appears implemented |
| UC-09 | Retrieve financial news | Appears implemented with Marketaux, relevance/freshness caveats |
| UC-10 | Explain recent market movement | Quote + news + Gemini explanation working; citation/timing quality caveats |
| UC-11 | Add portfolio position | Appears implemented |
| UC-12 | Remove portfolio position | Appears implemented |
| UC-13 | View portfolio | Appears implemented |
| UC-14 | Manage watchlist | Appears implemented (view/add/remove) |
| UC-15 | Analyze portfolio exposure | **Missing / needs real implementation** |
| UC-16 | Ask contextual follow-up question | **Missing / needs real conversational context and functional agent orchestration** |

**Do not count three aliases or minor CRUD buttons as three new meaningful features just to reach 10.** Produce a defensible table identifying the genuinely distinct nontrivial qualifying features and their actual operation, tests, and demos.

---

## 3. Execution plan, ordered by grading risk

### Milestone A: Build baseline, audit, and safe branch setup

1. Capture `git status`, `git branch --show-current`, `git log --all --date=iso`, Java version, Maven version and baseline `./mvnw clean test` result.
2. Read Stage 1 design and reconcile all use cases with code; create `docs/stage2-feature-matrix.md` and `docs/stage2-progress.md` showing gaps and evidence.
3. Audit all branches for October 20–26 commits, recognizing Toronto-local timestamps. Publish an honest commit-status checklist, not invented history.
4. Keep the existing `feature/llm-integration` work intact. If completed, merge via a reviewed PR **with user approval**, then branch `feature/gui`, `feature/portfolio-exposure`, `feature/conversation-memory`, or a similarly manageable branch from the appropriate current base. Avoid parallel branches that depend on unmerged code without integrating/rebasing intentionally.
5. Address any real source/package mismatch, baseline test failures, stale README entries, or secret exposures discovered before feature work.

**Acceptance:** The project compiles, tests have recorded results, current limitations are documented, and the agent knows the correct branch and feature gaps.

### Milestone B: Implement a real graphical interface (high priority)

**Recommended UI:** **Java Swing**, which works on Java 21 without a new framework dependency. If the actual project already uses another functioning GUI framework, evaluate before replacing. Make it polished, usable, and responsive, but prioritize demonstrable major functionality over decorative animation.

**Design:** Preserve the existing `TerminalUI` as a real CLI. Build a separate GUI entry point using shared application services. Recommended structure, adapt to repo after inspection:

```text
src/main/java/com/marketintel/
    Main.java                       # CLI default, --gui option, or documented launchers
    bootstrap/ApplicationContext.java # shared dependency construction where warranted
    gui/
        MarketIntelFrame.java        # main Swing window, navigation, session state
        AuthenticationPanel.java     # register/login/logout
        DashboardPanel.java          # overview and key operations
        MarketResearchPanel.java     # quote, overview, compare, ETF sector/geography/overlap, news
        PortfolioPanel.java          # positions, add/remove, portfolio exposure
        WatchlistPanel.java          # view/add/remove
        AgentPanel.java              # /why equivalent and contextual follow-up interface
        ...                          # shared UI formatting/controllers as needed
```

**Required GUI behavior:**

- Real registration/login/logout with existing `AuthService` and `SessionManager`; display clear validation errors without exposing password input.
- After login, display user-specific portfolio/watchlist and allow switching panels without dropping the authenticated session.
- Research panel can invoke actual market quote, overview, comparison, ETF sector/geographic exposure, overlap, news and movement analysis using the existing service/tool layer. Mark unimplemented functions unavailable until connected; do not present dummy results.
- Portfolio and watchlist panels can perform their full backed-by-SQLite operations, reflect updates immediately, handle empty states, invalid quantities/symbols and non-existent securities sensibly.
- AI panel can request evidence-grounded `/why SYMBOL` explanations with source links and uncertainty. Once UC-16 exists, support context-aware follow-ups here too.
- Network and LLM requests **must not block the Swing Event Dispatch Thread**. Use `SwingWorker` or executor-based background work with safe EDT updates, visible busy state, cancellable/unavailable behavior where reasonable, disabled duplicate requests, and proper exception presentation.
- Load and display actual user state from repositories. Enforce authentication/authorization in shared business services, not only by hiding UI buttons.
- Reuse consistent colours, readable text, charts/tables only where genuinely helpful, and appropriate macOS window behavior. Do not depend on an interactive terminal to launch Swing.
- Keep `--cli`/`--gui` invocation deterministic. Suggested commands after implementing entrypoint routing:

```bash
./mvnw clean test
./mvnw clean compile exec:java -Dexec.args="--cli"
./mvnw clean compile exec:java -Dexec.args="--gui"
```

  Confirm actual working commands in README rather than assuming the examples are correct.
- Add headless-safe tests for shared controllers/view-models and UI actions where practical; avoid tests that require manual window interaction in a headless CI environment. Document a separate GUI manual verification checklist and a human-recorded screencast plan.

**Acceptance:** Real GUI can launch on the user's Mac, user can log in, invoke multiple important existing use cases, observe actual results, and demonstrate those actions on video. CLI still works. No simulated data substituted for live functionality.

### Milestone C: Complete UC-15, portfolio exposure

1. Read Stage 1 sequence diagram `docs/uml/sequence-portfolio-analysis.puml` and inspect existing `PortfolioService`, `PortfolioRepository`, `PortfolioPosition`, `ETFDataProvider`, `MarketDataProvider`, `ExposureCalculator`, `ExposureResult` and any missing `PortfolioAnalysisTool`.
2. Implement actual per-user portfolio analytics, not a hard-coded chart. Prefer a model/tool such as `PortfolioAnalysisTool` registered for `ActionType.PORTFOLIO_EXPOSURE` and a service/calculator with unit-testable arithmetic.
3. If calculating market-value weights, use `BigDecimal`, calculate quantity × price using verified provider quotes and normalize by known total value. **Never silently use share quantity as portfolio market value**. Handle missing prices/unsupported currencies explicitly; do not incorrectly sum mixed-currency values.
4. For ETF look-through exposure, weight underlying ETF holdings by portfolio position market-value weight and holding weight; avoid double counting and clearly distinguish ETF look-through from direct-stock exposure. Classify unknown sector/country explicitly. Check provider holdings completeness and explain limitations. The Stage 1 sequence expects error handling where ETF holdings are unavailable.
5. Provide an actionable CLI command such as `/portfolio exposure` or `/exposure portfolio` **and a real GUI equivalent**, with user-specific output. Add offline tests for single/multiple holdings, sector aggregation, zero or absent positions, missing quotes, unknown classifications, precision, and different users.
6. If a full Stage 1-caliber portfolio sector exposure cannot be supported due data limitations, implement the strongest accurate subset, document the scope difference and its rationale, and do **not** falsely mark the Stage 1 requirement complete.

**Acceptance:** A portfolio with test positions yields mathematically validated exposure from real model/provider data; GUI and CLI demonstrate it; reports and UML agree with the implemented behavior.

### Milestone D: Finish UC-06 ETF geographic exposure

1. Inspect ETF holding country fields and provider capability. Add a `ETF_GEOGRAPHIC_EXPOSURE` path through the existing ETF tool/calculator, not a duplicate separate service unless justified.
2. Aggregate valid holdings by country; normalize weights correctly, classify missing/unknown country metadata, and distinguish **listing domicile** from **actual underlying economic exposure**. Do not claim geographic precision the provider does not support.
3. Add CLI `/geography ETF` (or consistent command alternative) and GUI action, offline tests and video scenario.

**Acceptance:** A real ETF holdings fixture with country metadata yields a credible geographic breakdown; unavailable source data produces a useful limitation/error, not fabricated allocations.

### Milestone E: Complete UC-16 and meaningful agent behavior

Stage 1 diagrams anticipate conversation persistence and orchestration. Existing `AgentController`, `RequestPlanner`, `PromptBuilder` and `ResponseComposer` may be placeholders. Implement only the architecture actually used, removing or explaining obsolete placeholders rather than leaving nonfunctional classes advertised as complete.

Required behavior:

- Store **per-user and per-conversation context** in SQLite or a defensible scoped memory implementation, with migration/setup code, lifecycle and authentication checks. Persistent conversational memory should survive app restart if Stage 1 promised it. Don't mix messages between different users or sessions.
- Support input like: `Why did NVDA move?` followed by `What about its sector exposure?` and, for a valid ETF context, `What about geographic exposure?`. Resolve the referenced symbol based on stored context; if there is no unambiguous referent, ask for clarification instead of inventing one.
- Provide a genuine multi-step agent pipeline: interpret/plan a supported action, select and invoke registered `AgentTool`s or services, validate arguments, gather results, combine evidence, and compose a grounded response. A deterministic planner for supported intents is acceptable and more testable than an unvalidated free-form LLM action planner; if LLM planning is used, parse and validate outputs against an allowlist.
- Reuse `LLMProvider`/`GeminiLLMClient` for explanations, keeping decision boundaries explicit. Do **not** allow an LLM to execute arbitrary code, SQL, shell commands, file writes or hidden requests based on untrusted article text. Reject unsupported tool/action names.
- Protect private portfolio details from third-party LLM submission by default; consider an explicit opt-in or render deterministic sensitive analytics locally. Persist only needed context, set sensible length limits, and do not write secrets into conversation history.
- Provide a working CLI natural-language input or `/ask ...` and GUI chat interaction. Test direct questions, follow-ups, ambiguous pronouns, multi-user isolation, persistence across restart, unavailable tools, injection attempts and empty context.

**Acceptance:** A real follow-up correctly references the preceding context, can invoke at least one actual tool, has working per-user memory, and is demonstrable in both interfaces. It is **not** merely prompt forwarding.

### Milestone F: Harden UC-10, AI response quality and provider resilience

- Preserve and show raw quote, news evidence, dates and references even if Gemini fails.
- Keep the distinction between the quote's **retrieval time** and the actual trading-session timestamp; don't assert causation from articles published after the price movement. If provider exposes verified session timestamps, incorporate them and test them; otherwise display unknown.
- Marketaux `symbols` matches can be loosely associated with a ticker. Prefer directly relevant articles, and accurately note when only broad sector context exists. Never conceal insufficient data.
- Validate that model-generated citation markers point only to provided `[1]...[N]`; a valid marker **does not prove claim support**, so preserve an uncertainty disclaimer and add behavioral/manual checks for unsupported claims.
- Never trust external news article bodies as instructions. Prevent prompt injection into tool execution; normalize/sanitize rendering for GUI and terminal. No investment buy/sell recommendations that pretend to be verified advice.
- Distinguish deterministic calculation tests from LLM outcome-quality tests. Keep paid/live requests optional.

**Acceptance:** No fabricated source links; request failure displays evidence and actionable error; model-generated interpretation openly states insufficient evidence where appropriate.

### Milestone G: Identify and implement five defensible design patterns

Before claiming any, audit which patterns were **actually covered in the course and explicitly discussed in Stage 1**. Identify existing code patterns and assess whether each solves a real, documented design problem. Candidate patterns, **not automatic credit**:

| Candidate | Potential concrete roles | Real problem solved / what to verify |
|---|---|---|
| **Command** | `AgentTool`, concrete `MarketDataTool`, `ETFExposureTool`, `NewsTool`, `MarketMovementTool`, `ToolManager` | Encapsulating operations and dispatch, not merely method renaming |
| **Adapter** | `MarketDataApiClient`/`MarketauxNewsClient` adapting external provider response shapes to internal `MarketDataProvider`/`NewsProvider` contracts | Insulating tools from vendor-specific formats and errors |
| **Strategy** | Interchangeable `LLMProvider` implementations and selection policy, or alternate provider behaviors | Real interchangeable algorithms chosen at runtime; a fake test-only class alone is insufficient justification |
| **Factory Method / Factory** | Validated API provider/tool creation from configuration, separated from GUI/CLI | Avoiding duplicate construction/conditionals, preserving type-safe dependencies |
| **Observer** | A meaningful portfolio/watchlist domain-change event listener updating multiple GUI views | Consistent UI state updates and decoupling, with subscribe/notify/unsubscribe behavior |
| **Other course-covered pattern** | Builder, Facade, Bridge, Iterator, etc. **only if justified and actually implemented** | Useful, nonredundant design problem. Do not add pattern theater. |

**Requirement:** At least five **distinct** genuine course-covered patterns. If some candidates do not qualify, select better justified patterns and implement/test them. Document pattern intent, participants, UML relationships, concrete methods, why it improves the system, and differences from Stage 1. Keep pattern code small and functional, not decorative.

**Acceptance:** A code reviewer can trace each of the five patterns to a working class collaboration and to a relevant problem; UML and tests back the claim.

### Milestone H: Tests, verification and release safety

- Preserve existing JUnit tests and add focused offline tests for GUI-facing controllers, provider parsing, tool routing, portfolio exposure calculations, geography, conversation memory and planner validation.
- Keep Gemini smoke test guarded by `RUN_GEMINI_LIVE_TEST=1`, and similarly guard other live-provider tests. Do not require live keys for `./mvnw clean test`.
- Add a compact smoke-test checklist that a reviewer can execute on macOS with CLI and GUI (fresh login, quote, portfolio, watchlist, ETF, news, `/why`, contextual follow-up, unknown symbol, provider failure).
- Include tests for no data, malformed response, rate limits, duplicate updates, data isolation, session expiry, absent credentials, and unsupported intent where relevant.
- If practical, configure GitHub Actions to run JDK 21 Maven tests on pull requests, **without secrets**. CI success cannot replace manual video demonstrations.
- Do not claim 100% test coverage or perfect groundedness unless actually measured.

**Acceptance:** `./mvnw clean test` and compile pass in a clean checkout; both UIs can be launched and demonstrated; documented limitations remain explicit.

### Milestone I: Synchronize UML and implementation

The current Stage 1 `.puml` describes classes that may not exist or may have changed signatures. **Update diagrams to match final executable code**:

- `docs/uml/class-diagram-agent.puml`
- `docs/uml/class-diagram-domain.puml`
- `docs/uml/class-diagram-persistence.puml`
- `docs/uml/use-case-diagram.puml` if use-case/actor relationships changed
- All materially affected Stage 1 sequence diagrams, especially market movement, ETF analysis, portfolio analysis and contextual follow-up.

Include the GUI boundary, actual AI service/provider classes, persistence, shared services, tool orchestration, realistic return types and five verified patterns. Use correct UML notation (inheritance/realization, composition, association/dependency) and actual method names. Keep diagrams legible; separate into multiple views if necessary. Render/export updated diagrams to a viewer-accessible format such as PDF/PNG using available PlantUML tooling and check that they render. Do not pass off old Stage 1 PDFs as updated UML.

**Acceptance:** Diagrams render without syntax errors and correspond to actual source files/methods, including documented Stage 1 deviations.

### Milestone J: Report, AI log, README and secrets

Create/maintain:

```text
docs/stage2-feature-matrix.md
# optional source for report writing:
docs/stage2-report.md
docs/stage2-report.pdf     # OR .docx; actual final deliverable must be present
# existing, expanded:
docs/ai-collaboration-log.md
docs/stage2-commit-summary.md
docs/stage2-demo-index.md
.env.example               # safe placeholder credentials only
README.md                  # accurate GUI + CLI setup and feature instructions
```

Report needs all **seven** sections defined above. Follow the rubric rather than converting this brief into an unverified narrative. For feature mapping, use rows with **ID/name, original Stage 1 reference, implemented/partial/replaced status, files/classes/responsibilities, important methods, patterns, deterministic/AI/hybrid classification, and video ID/timestamp**. Explain user input → interface → controller/tool → services/providers → result.

For changes, include honest examples backed by Git:

- Alpha Vantage news access became premium; replaced news adapter with Marketaux, preserving `NewsProvider` contract.
- News needed ticker relevance and seven-day freshness filtering to reduce unrelated/outdated results.
- Added `MarketMovementContext`, `MarketMovementTool`, `MarketExplanationService` and Gemini provider, with clear timing/causality limitations.
- Introduced scrollback mode to make long AI analyses readable in CLI.
- Added a Swing GUI and any new agent/memory/pattern components **only after they actually exist**.

For AI collaboration: record actual tools/models used (e.g., ChatGPT for Java architecture/debugging/UX/tests, Cursor for coding/refactoring, Gemini API for runtime analysis); specific tasks, prompts or conversation examples, human reviews/corrections, actual token and money consumption where available. If a platform's token/cost totals cannot be obtained, state **unavailable** rather than inventing numbers. Do not include secret values or private user data in prompts/logs. Update the existing AI collaboration log instead of discarding its history.

For credentials: `README.md` should explain env vars (`ALPHA_VANTAGE_API_KEY`, `MARKETAUX_API_TOKEN`, `GEMINI_API_KEY`, optional `GEMINI_MODEL`, optional `MI_SCROLLBACK`) and how to run without exposing them. `.env.example` must contain placeholders only; ensure `.gitignore` **allows `.env.example` specifically** if a broad `.env.*` pattern would otherwise ignore it. Verify `git status --ignored` and `git ls-files` for accidental secrets. Do not print tokens in diagnostics.

For the report: actually generate a working **PDF or Word file**, named `stage2-report`, and verify it opens and is committed to the same public repository. If no PDF/Word tool is installed, create the complete Markdown report source and clear export instructions, explicitly mark final conversion pending; don't pretend Markdown alone meets the submission requirement.

**Acceptance:** Every claim in the report has code, command, test, commit or video evidence. No obsolete README claims and no exposed secrets.

### Milestone K: Demonstration deliverables

Create a practical storyboard and index. Suggested combined recordings:

1. **GUI auth + dashboard + market research:** register/login, quote, security overview, compare, ETF exposure, overlap, news, error behavior.
2. **GUI portfolio + watchlist + exposure:** per-user add/view/remove, watchlist management and portfolio exposure.
3. **CLI research + analytics:** demonstrate the CLI independently; market data, ETFs, portfolio, watchlist.
4. **AI agent end-to-end:** `/why NVDA`, evidence+Gemini response, source references, missing-news fallback, follow-up context and tool selection. GUI + CLI examples as needed.

Use verified feature-specific timestamps, not guessed durations. Index **every implemented feature**, even if a feature is demonstrated within a combined video. Record in a screen resolution where text is readable. Avoid exposing keys/private data. If Cursor cannot create actual live videos, provide commands, demo scripts and a recording checklist for the user and explicitly flag pending videos.

**Acceptance:** Every implemented feature has an accessible video link/timestamp and the collection contains real GUI and CLI footage.

---

## 4. Repository and commit workflow

- Inspect existing remote branches and confirm their status. Work in appropriate `feature/` or `fix/` branches; merge via reviewed pull requests when user approves.
- Do not delete `main`, force-push or flatten the commit history merely to simplify delivery.
- Before implementation, record the current project baseline; after each task, commit **substantive code/test progress** once verified. Suggested categories: GUI authentication and navigation; GUI market research; GUI portfolio/watchlist integration; genuine exposure calculation; persistent conversation memory; planner/tool integration; unit/regression tests. **These are examples of real units of work, not a mandate to create artificial commits.**
- Weekly commits are judged in **Toronto local time**. Check **all Git refs** and their reachable commits, avoiding double counting duplicate SHA across branches. On a Mac, a helpful audit command is:

```bash
git log --all --no-merges --date=iso-strict \
  --format='%H | %cd | %s' --reverse
```

  Inspect each timestamp in America/Toronto and separate October 20–26, October 27–November 2, November 3–9. Include direct GitHub commit URLs in `docs/stage2-commit-summary.md`. If a reporting period is already missed, say so, and prioritize the remaining periods. Do not backdate or game the rubric.
- Prefer retaining feature commits in a merge commit or non-squash PR if the rubric needs to recognize individual code commits in repository history. Ensure changes are pushed within the required window, not merely made locally.
- Use descriptive commit messages, e.g. `feat(gui): connect quote and news tools to market research tab`, `feat(agent): persist per-user conversation context`, `test(portfolio): cover missing ETF holdings and mixed currencies`.
- Verify build and functional smoke checks before PR. Update status docs and report mappings as changes land.

---

## 5. Recommended work order from October 28

**This is a suggested priority schedule, not a claim that work has already been done.** Favor verified, usable deliverables over excess polishing.

| Date window | Priority and measurable outputs |
|---|---|
| **Oct 28–30** | Baseline audit; resolve compilation defects; working Swing GUI login/navigation, market research and portfolio/watchlist panels; real commits |
| **Oct 31–Nov 2** | GUI AI view, UC-15 exposure, practical integration tests; ensure five qualifying Period 2 commits are pushed by end Nov 2 Toronto time |
| **Nov 3–5** | UC-06 geography, UC-16 persistence/planning/follow-ups; finish 5 verified patterns; more substantive commits |
| **Nov 6–7** | Update UML; stabilize both interfaces; create report source, add reproducible README and safe config |
| **Nov 8–9** | Complete demos, links/timestamps, verify five qualifying Period 3 commits, generate final PDF/Word and run fresh-checkout review |
| **Nov 10** | Final checks, verify public GitHub repository, report and videos; submit by deadline |

If risks threaten the schedule, prioritize in this order: **both UIs work → all Stage 1 use cases accounted for → ten genuinely meaningful features → five demonstrably valid patterns → agent behavior → video coverage → complete required report and UML → extra polish**. Don't silently drop requirements; document honest blockers.

---

## 6. Final non-negotiable acceptance checklist

Before declaring Stage 2 ready, explicitly evaluate each item below with **PASS / FAIL / PARTIAL and evidence**:

- [ ] `./mvnw clean test` passes with no live API keys and opt-in smoke tests skipped.
- [ ] `./mvnw clean compile` succeeds on Java 21.
- [ ] CLI is runnable and exposes working major features.
- [ ] Swing GUI is runnable, usable and accesses the **same real services** for major features.
- [ ] Every UC-01 through UC-16 is implemented, or a documented Stage 1-to-Stage 2 deviation explains its replacement/limitation; no silent omissions.
- [ ] At least ten distinct nontrivial features are demonstrably functional, **excluding trivial account/session/exit operations**.
- [ ] At least five genuinely distinct course-covered design patterns are validated in code, diagrams and report.
- [ ] Actual Gemini LLM request succeeds when live credentials are supplied; fallback behavior works if unavailable.
- [ ] Agent performs meaningful multi-step planning/tool use/context-based behavior, beyond prompt forwarding.
- [ ] User accounts and conversation memory do not leak across sessions; passwords are BCrypt-hashed.
- [ ] Portfolio exposure and geographic ETF exposure produce real validated computations, or limitations are documented honestly.
- [ ] No fake fixed outputs, empty classes advertised as features, or dead GUI controls.
- [ ] All materially changed UML diagrams are accurate and render successfully.
- [ ] `stage2-report.pdf` or `stage2-report.docx` exists in the public repository and contains all seven required sections.
- [ ] All implemented features have accessible, labeled demonstration video segments, collectively including GUI and CLI.
- [ ] Weekly commits verified separately for **Oct 20–26, Oct 27–Nov 2, Nov 3–9** using real Toronto-local timestamps and direct links, with missing periods honestly disclosed.
- [ ] README documents clean setup, GUI/CLI launches, runtime configuration, permissions and examples.
- [ ] `.env.example` is safe and committed; real credentials are ignored, not present in code/history.
- [ ] Dependencies and required sample data/build files are committed; repository is public.
- [ ] Report tracks AI tools, actual model names, human corrections, and token/cost figures or explicitly unavailable figures.
- [ ] Final verification includes actual feature demos, test output, report file check, and review of GitHub commit visibility.

---

## 7. First agent response and progress protocol

**Before making changes, send a brief progress report with:**

1. Current checked-out branch and working-tree status.
2. Result of baseline `./mvnw clean test` and compile.
3. Which of the 16 use cases and Stage 2 requirements are currently missing/partial, based on actual files.
4. A proposed minimal shared-service GUI architecture and the initial files you will implement.
5. Real weekly commit status, explicitly calling out the apparently missing October 20–26 period if confirmed.

**Then immediately start the highest-priority implementation.** After each milestone, show actual modified files, tests, demo status and commits. Avoid repeatedly asking the user to restate project details that exist in the repository. When something cannot be completed in the coding environment (notably real human-recorded demos or private API provisioning), produce a concrete handoff checklist rather than claiming completion.

**Quality bar:** This is a university software design project. Optimize for real functionality, traceable architecture, maintainable Java, defensible design patterns, honest AI collaboration records and demonstrable correctness. A smaller honest feature set with good evidence is better than a polished collection of fake checkmarks, but all original requirements must still be accounted for.
