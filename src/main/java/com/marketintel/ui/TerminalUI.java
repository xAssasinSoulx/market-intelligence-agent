package com.marketintel.ui;

import com.marketintel.agent.ToolManager;

import com.marketintel.auth.AuthService;
import com.marketintel.auth.SessionManager;

import com.marketintel.model.ActionType;
import com.marketintel.model.AssetType;
import com.marketintel.model.ExposureResult;
import com.marketintel.model.MarketQuote;
import com.marketintel.model.OverlapResult;
import com.marketintel.model.Portfolio;
import com.marketintel.model.PortfolioPosition;
import com.marketintel.model.SecurityOverview;
import com.marketintel.model.ToolRequest;
import com.marketintel.model.ToolResult;
import com.marketintel.model.User;
import com.marketintel.model.Watchlist;

import com.marketintel.services.PortfolioService;
import com.marketintel.services.WatchlistService;

import java.io.Console;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.Map;
import java.util.Scanner;

public class TerminalUI
        implements UserInterface {

    private final AuthService authService;

    private final SessionManager sessionManager;

    private final CommandParser commandParser;

    private final PortfolioService portfolioService;

    private final WatchlistService watchlistService;

    private final ToolManager toolManager;

    private final Scanner scanner;

    private boolean running = true;

    public TerminalUI(
            AuthService authService,
            SessionManager sessionManager,
            CommandParser commandParser,
            PortfolioService portfolioService,
            WatchlistService watchlistService,
            ToolManager toolManager) {

        this.authService = authService;

        this.sessionManager =
                sessionManager;

        this.commandParser =
                commandParser;

        this.portfolioService =
                portfolioService;

        this.watchlistService =
                watchlistService;

        this.toolManager =
                toolManager;

        this.scanner =
                new Scanner(System.in);
    }

    @Override
    public void start() {

        TerminalTheme.enterAlternateScreen();

        try {

            while (running) {

                if (!sessionManager
                        .isAuthenticated()) {

                    showAuthenticationMenu();

                } else {

                    showAuthenticatedTerminal();
                }
            }

        } finally {

            /*
             * Always restore the user's normal terminal,
             * even if something inside the application
             * throws an exception.
             */
            TerminalTheme.exitAlternateScreen();

            System.out.println(
                    "Market Intelligence Agent closed."
            );
        }
    }

    private void showAuthenticationMenu() {

        TerminalTheme.clearScreen();

        printBanner();

        TerminalComponents.section(
                "AUTHENTICATION"
        );

        TerminalComponents.row(
                "1",
                "Log In"
        );

        TerminalComponents.row(
                "2",
                "Register"
        );

        TerminalComponents.row(
                "3",
                "Exit"
        );

        TerminalComponents.endSection();

        System.out.println();

        System.out.print(
                TerminalTheme.cyan("AUTH")
                        + TerminalTheme.dim(" ❯ ")
        );

        String input =
                scanner.nextLine()
                        .trim();

        switch (input) {

            case "1" ->
                    login();

            case "2" ->
                    register();

            case "3" ->
                    running = false;

            default -> {

                displayError(
                        "Invalid option. Enter 1, 2, or 3."
                );

                waitForEnter();
            }
        }
    }

    private void waitForEnter() {

        System.out.println();

        System.out.print(
                TerminalTheme.dim(
                        "Press Enter to continue..."
                )
        );

        scanner.nextLine();
    }

    private void register() {

        TerminalTheme.clearScreen();

        printBanner();

        System.out.println();

        System.out.println(
                "CREATE ACCOUNT"
        );

        printDivider();

        System.out.print(
                "Username: "
        );

        String username =
                scanner.nextLine()
                        .trim();

        String password =
                readPassword(
                        "Password: "
                );

        String confirmation =
                readPassword(
                        "Confirm password: "
                );

        if (!password.equals(
                confirmation)) {

            displayError(
                    "Passwords do not match."
            );

            return;
        }

        try {

            User user =
                    authService.register(
                            username,
                            password
                    );

            displayResponse(
                    "Account created "
                            + "successfully for "
                            + user.getUsername()
                            + "."
            );

        } catch (
                IllegalArgumentException e) {

            displayError(
                    e.getMessage()
            );

        } catch (
                RuntimeException e) {

            displayError(
                    "Unable to create account."
            );
        }
    }

    private void login() {

        TerminalTheme.clearScreen();

        printBanner();

        TerminalComponents.section(
                "USER LOGIN"
        );

        System.out.println();

        System.out.print(
                "Username: "
        );

        String username =
                scanner.nextLine()
                        .trim();

        String password =
                readPassword(
                        "Password: "
                );

        TerminalComponents.endSection();

        try {

            User user =
                    authService.authenticate(
                            username,
                            password
                    );

            sessionManager.startSession(
                    user
            );

            TerminalTheme.clearScreen();

            displayTerminalHeader();

            displayDashboard();

        } catch (SecurityException e) {

            displayError(
                    "Invalid username or password."
            );

            waitForEnter();

        } catch (RuntimeException e) {

            displayError(
                    "Unable to complete login."
            );

            waitForEnter();
        }
    }

    private void displayDashboard() {

        User user =
                sessionManager.getCurrentUser();

        Portfolio portfolio =
                portfolioService.getPortfolio(
                        user.getId()
                );

        Watchlist watchlist =
                watchlistService.getWatchlist(
                        user.getId()
                );

        TerminalComponents.section(
                "SESSION"
        );

        TerminalComponents.row(
                "USER",
                user.getUsername()
        );

        TerminalComponents.row(
                "STATUS",
                "CONNECTED"
        );

        TerminalComponents.row(
                "PORTFOLIO",
                portfolio.getPositions()
                        .size()
                        + " positions"
        );

        TerminalComponents.row(
                "WATCHLIST",
                watchlist.getSymbols()
                        .size()
                        + " symbols"
        );

        TerminalComponents.endSection();

        TerminalComponents.section(
                "QUICK COMMANDS"
        );

        TerminalComponents.row(
                "/quote",
                "/quote NVDA"
        );

        TerminalComponents.row(
                "/sector",
                "/sector QQQ"
        );

        TerminalComponents.row(
                "/overlap",
                "/overlap QQQ VGT"
        );

        TerminalComponents.row(
                "/help",
                "Show all commands"
        );

        TerminalComponents.endSection();
    }

    private void showAuthenticatedTerminal() {

        System.out.println();

        String username =
                sessionManager
                        .getCurrentUser()
                        .getUsername();

        System.out.print(
                TerminalTheme.green(
                        username
                )
                        + TerminalTheme.dim("@")
                        + TerminalTheme.cyan(
                        "MI"
                )
                        + TerminalTheme.dim(
                        " ❯ "
                )
        );

        String input =
                scanner.nextLine()
                        .trim();

        if (input.isBlank()) {
            return;
        }

        if (commandParser
                .isCommand(input)) {

            handleCommand(
                    input
            );

            return;
        }

        TerminalTheme.clearScreen();

        displayTerminalHeader();

        System.out.println();

        System.out.println(
                TerminalTheme.yellow(
                        "Natural-language agent "
                                + "functionality is not "
                                + "connected yet."
                )
        );
    }

    private void handleCommand(
            String input) {

        try {

            ParsedCommand parsed =
                    commandParser.parse(
                            input
                    );

            /*
             * New command = new screen.
             */
            TerminalTheme.clearScreen();

            displayTerminalHeader();

            switch (
                    parsed.getCommand()) {

                case "help" ->
                        displayHelp();

                case "whoami" ->
                        displayCurrentUser();

                case "portfolio" ->
                        handlePortfolioCommand(
                                parsed
                        );

                case "watchlist" ->
                        handleWatchlistCommand(
                                parsed
                        );

                case "quote" ->
                        handleQuoteCommand(
                                parsed
                        );

                case "overview" ->
                        handleOverviewCommand(
                                parsed
                        );

                case "sector" ->
                        handleSectorCommand(
                                parsed
                        );

                case "overlap" ->
                        handleOverlapCommand(
                                parsed
                        );

                case "logout" ->
                        logout();

                case "exit" ->
                        exitApplication();

                default ->
                        displayError(
                                "Unknown command: /"
                                        + parsed
                                        .getCommand()
                        );
            }

        } catch (
                IllegalArgumentException e) {

            TerminalTheme.clearScreen();

            displayTerminalHeader();

            displayError(
                    e.getMessage()
            );
        }
    }

    // ========================================================
    // Portfolio
    // ========================================================

    private void handlePortfolioCommand(
            ParsedCommand command) {

        long userId =
                sessionManager
                        .getCurrentUserId();

        if (command.getArguments()
                .isEmpty()) {

            displayPortfolio(
                    userId
            );

            return;
        }

        String action =
                command.getArguments()
                        .getFirst()
                        .toLowerCase();

        switch (action) {

            case "add" ->
                    addPortfolioPosition(
                            userId,
                            command
                    );

            case "remove" ->
                    removePortfolioPosition(
                            userId,
                            command
                    );

            default ->
                    displayError(
                            "Usage: /portfolio, "
                                    + "/portfolio add "
                                    + "SYMBOL QUANTITY TYPE, "
                                    + "or /portfolio remove "
                                    + "SYMBOL"
                    );
        }
    }

    private void displayPortfolio(
            long userId) {

        Portfolio portfolio =
                portfolioService
                        .getPortfolio(
                                userId
                        );

        TerminalComponents.section(
                "PORTFOLIO"
        );

        if (portfolio
                .getPositions()
                .isEmpty()) {

            TerminalComponents.row(
                    "STATUS",
                    "No positions"
            );

            TerminalComponents.endSection();

            return;
        }

        TerminalComponents.row(
                "SYMBOL",
                "QUANTITY • TYPE"
        );

        TerminalComponents.separator();

        for (PortfolioPosition position
                : portfolio.getPositions()) {

            String value =
                    position.getQuantity()
                            .stripTrailingZeros()
                            .toPlainString()
                            + " • "
                            + position
                            .getAssetType();

            TerminalComponents.row(
                    position.getSymbol(),
                    value
            );
        }

        TerminalComponents.endSection();
    }

    private void addPortfolioPosition(
            long userId,
            ParsedCommand command) {

        if (command.getArguments()
                .size() != 4) {

            displayError(
                    "Usage: /portfolio add "
                            + "SYMBOL QUANTITY TYPE"
            );

            return;
        }

        try {

            String symbol =
                    command.getArguments()
                            .get(1);

            BigDecimal quantity =
                    new BigDecimal(
                            command.getArguments()
                                    .get(2)
                    );

            AssetType assetType =
                    AssetType.valueOf(
                            command
                                    .getArguments()
                                    .get(3)
                                    .toUpperCase()
                    );

            portfolioService
                    .addPosition(
                            userId,
                            symbol,
                            quantity,
                            assetType
                    );

            displayResponse(
                    symbol.toUpperCase()
                            + " added to portfolio."
            );

        } catch (
                NumberFormatException e) {

            displayError(
                    "Quantity must be "
                            + "a valid number."
            );

        } catch (
                IllegalArgumentException e) {

            displayError(
                    e.getMessage()
            );
        }
    }

    private void removePortfolioPosition(
            long userId,
            ParsedCommand command) {

        if (command.getArguments()
                .size() != 2) {

            displayError(
                    "Usage: /portfolio "
                            + "remove SYMBOL"
            );

            return;
        }

        String symbol =
                command.getArguments()
                        .get(1);

        try {

            boolean removed =
                    portfolioService
                            .removePosition(
                                    userId,
                                    symbol
                            );

            if (removed) {

                displayResponse(
                        symbol.toUpperCase()
                                + " removed "
                                + "from portfolio."
                );

            } else {

                displayError(
                        symbol.toUpperCase()
                                + " is not in "
                                + "your portfolio."
                );
            }

        } catch (
                IllegalArgumentException e) {

            displayError(
                    e.getMessage()
            );
        }
    }

    // ========================================================
    // Watchlist
    // ========================================================

    private void handleWatchlistCommand(
            ParsedCommand command) {

        long userId =
                sessionManager
                        .getCurrentUserId();

        if (command.getArguments()
                .isEmpty()) {

            displayWatchlist(
                    userId
            );

            return;
        }

        String action =
                command.getArguments()
                        .getFirst()
                        .toLowerCase();

        switch (action) {

            case "add" ->
                    addWatchlistSymbol(
                            userId,
                            command
                    );

            case "remove" ->
                    removeWatchlistSymbol(
                            userId,
                            command
                    );

            default ->
                    displayError(
                            "Usage: /watchlist, "
                                    + "/watchlist add SYMBOL, "
                                    + "or /watchlist remove "
                                    + "SYMBOL"
                    );
        }
    }

    private void displayWatchlist(
            long userId) {

        Watchlist watchlist =
                watchlistService
                        .getWatchlist(
                                userId
                        );

        TerminalComponents.section(
                "WATCHLIST"
        );

        if (watchlist
                .getSymbols()
                .isEmpty()) {

            TerminalComponents.row(
                    "STATUS",
                    "No symbols"
            );

            TerminalComponents.endSection();

            return;
        }

        int index = 1;

        for (String symbol
                : watchlist.getSymbols()) {

            TerminalComponents.row(
                    String.format(
                            "%02d",
                            index
                    ),
                    symbol
            );

            index++;
        }

        TerminalComponents.endSection();
    }

    private void addWatchlistSymbol(
            long userId,
            ParsedCommand command) {

        if (command.getArguments()
                .size() != 2) {

            displayError(
                    "Usage: /watchlist "
                            + "add SYMBOL"
            );

            return;
        }

        String symbol =
                command.getArguments()
                        .get(1);

        try {

            watchlistService
                    .addSymbol(
                            userId,
                            symbol
                    );

            displayResponse(
                    symbol.toUpperCase()
                            + " added to watchlist."
            );

        } catch (
                IllegalArgumentException e) {

            displayError(
                    e.getMessage()
            );
        }
    }

    private void removeWatchlistSymbol(
            long userId,
            ParsedCommand command) {

        if (command.getArguments()
                .size() != 2) {

            displayError(
                    "Usage: /watchlist "
                            + "remove SYMBOL"
            );

            return;
        }

        String symbol =
                command.getArguments()
                        .get(1);

        try {

            boolean removed =
                    watchlistService
                            .removeSymbol(
                                    userId,
                                    symbol
                            );

            if (removed) {

                displayResponse(
                        symbol.toUpperCase()
                                + " removed "
                                + "from watchlist."
                );

            } else {

                displayError(
                        symbol.toUpperCase()
                                + " is not in "
                                + "your watchlist."
                );
            }

        } catch (
                IllegalArgumentException e) {

            displayError(
                    e.getMessage()
            );
        }
    }

    // ========================================================
    // Market Quote
    // ========================================================

    private void handleQuoteCommand(
            ParsedCommand command) {

        if (command.getArguments()
                .size() != 1) {

            displayError(
                    "Usage: /quote SYMBOL"
            );

            return;
        }

        String symbol =
                command.getArguments()
                        .getFirst();

        ToolRequest request =
                new ToolRequest(
                        sessionManager
                                .getCurrentUserId(),

                        ActionType.MARKET_QUOTE,

                        Map.of(
                                "symbol",
                                symbol
                        )
                );

        ToolResult result =
                toolManager.execute(
                        request
                );

        if (!result.isSuccess()) {

            displayError(
                    result.getErrorMessage()
            );

            return;
        }

        if (!(result.getData()
                instanceof MarketQuote quote)) {

            displayError(
                    "Unexpected market-data "
                            + "response."
            );

            return;
        }

        displayMarketQuote(
                quote
        );
    }

    private void displayMarketQuote(
            MarketQuote quote) {

        BigDecimal change =
                quote.getPercentChange();

        String changeText =
                change
                        .stripTrailingZeros()
                        .toPlainString()
                        + "%";

        if (change.compareTo(
                BigDecimal.ZERO
        ) > 0) {

            changeText =
                    TerminalTheme.green(
                            "▲ +"
                                    + changeText
                    );

        } else if (change.compareTo(
                BigDecimal.ZERO
        ) < 0) {

            changeText =
                    TerminalTheme.red(
                            "▼ "
                                    + changeText
                    );

        } else {

            changeText =
                    TerminalTheme.yellow(
                            "● "
                                    + changeText
                    );
        }

        TerminalComponents.section(
                "MARKET QUOTE"
        );

        TerminalComponents.row(
                "SYMBOL",
                quote.getSymbol()
        );

        TerminalComponents.row(
                "PRICE",
                quote.getPrice()
                        .stripTrailingZeros()
                        .toPlainString()
        );

        TerminalComponents.row(
                "PREV CLOSE",
                quote.getPreviousClose()
                        .stripTrailingZeros()
                        .toPlainString()
        );

        TerminalComponents.rawRow(
                "CHANGE",
                changeText
        );

        TerminalComponents.endSection();
    }

    // ========================================================
    // Security Overview
    // ========================================================

    private void handleOverviewCommand(
            ParsedCommand command) {

        if (command.getArguments()
                .size() != 1) {

            displayError(
                    "Usage: /overview SYMBOL"
            );

            return;
        }

        String symbol =
                command.getArguments()
                        .getFirst();

        ToolRequest request =
                new ToolRequest(
                        sessionManager
                                .getCurrentUserId(),

                        ActionType
                                .SECURITY_OVERVIEW,

                        Map.of(
                                "symbol",
                                symbol
                        )
                );

        ToolResult result =
                toolManager.execute(
                        request
                );

        if (!result.isSuccess()) {

            displayError(
                    result.getErrorMessage()
            );

            return;
        }

        if (!(result.getData()
                instanceof SecurityOverview
                overview)) {

            displayError(
                    "Unexpected security "
                            + "overview response."
            );

            return;
        }

        displaySecurityOverview(
                overview
        );
    }

    private void displaySecurityOverview(
            SecurityOverview overview) {

        TerminalComponents.section(
                "SECURITY OVERVIEW • "
                        + overview.getSymbol()
        );

        TerminalComponents.row(
                "NAME",
                overview.getName()
        );

        TerminalComponents.row(
                "EXCHANGE",
                overview.getExchange()
        );

        TerminalComponents.row(
                "CURRENCY",
                overview.getCurrency()
        );

        TerminalComponents.row(
                "SECTOR",
                overview.getSector()
        );

        TerminalComponents.row(
                "INDUSTRY",
                overview.getIndustry()
        );

        if (overview
                .getMarketCapitalization()
                != null) {

            TerminalComponents.row(
                    "MARKET CAP",
                    overview
                            .getMarketCapitalization()
                            .stripTrailingZeros()
                            .toPlainString()
            );
        }

        if (overview.getDescription()
                != null
                && !overview
                .getDescription()
                .isBlank()) {

            TerminalComponents.separator();

            TerminalComponents.wrappedRow(
                    "DESCRIPTION",
                    overview.getDescription()
            );
        }

        TerminalComponents.endSection();
    }

    // ========================================================
    // ETF Sector Exposure
    // ========================================================

    private void handleSectorCommand(
            ParsedCommand command) {

        if (command.getArguments()
                .size() != 1) {

            displayError(
                    "Usage: /sector ETF_SYMBOL"
            );

            return;
        }

        String symbol =
                command.getArguments()
                        .getFirst();

        ToolRequest request =
                new ToolRequest(
                        sessionManager
                                .getCurrentUserId(),

                        ActionType
                                .ETF_SECTOR_EXPOSURE,

                        Map.of(
                                "symbol",
                                symbol
                        )
                );

        ToolResult result =
                toolManager.execute(
                        request
                );

        if (!result.isSuccess()) {

            displayError(
                    result.getErrorMessage()
            );

            return;
        }

        if (!(result.getData()
                instanceof ExposureResult
                exposureResult)) {

            displayError(
                    "Unexpected ETF exposure "
                            + "response."
            );

            return;
        }

        displaySectorExposure(
                exposureResult
        );
    }

    private void displaySectorExposure(
            ExposureResult result) {

        TerminalComponents.section(
                "ETF SECTOR EXPOSURE • "
                        + result.getSymbol()
        );

        if (result.getCategories()
                .isEmpty()) {

            TerminalComponents.row(
                    "STATUS",
                    "No sector exposure data"
            );

            TerminalComponents.endSection();

            return;
        }

        result.getCategories()
                .entrySet()
                .stream()

                .sorted(
                        Map.Entry
                                .<String, BigDecimal>
                                        comparingByValue()
                                .reversed()
                )

                .forEach(
                        entry -> {

                            BigDecimal percent =
                                    entry.getValue()
                                            .multiply(
                                                    new BigDecimal(
                                                            "100"
                                                    )
                                            )
                                            .setScale(
                                                    2,
                                                    RoundingMode.HALF_UP
                                            );

                            String bar =
                                    createBar(
                                            percent
                                    );

                            TerminalComponents.row(
                                    truncate(
                                            entry.getKey(),
                                            15
                                    ),

                                    String.format(
                                            "%6s%% %s",
                                            percent,
                                            bar
                                    )
                            );
                        }
                );

        TerminalComponents.endSection();
    }

    private String createBar(
            BigDecimal percent) {

        int length =
                percent
                        .divide(
                                new BigDecimal("5"),
                                0,
                                RoundingMode.DOWN
                        )
                        .intValue();

        length =
                Math.max(
                        0,
                        Math.min(
                                20,
                                length
                        )
                );

        return TerminalTheme.cyan(
                "█".repeat(length)
        )
                + TerminalTheme.dim(
                "░".repeat(
                        20 - length
                )
        );
    }

    // ========================================================
    // ETF Overlap
    // ========================================================

    private void handleOverlapCommand(
            ParsedCommand command) {

        if (command.getArguments()
                .size() != 2) {

            displayError(
                    "Usage: /overlap "
                            + "ETF1 ETF2"
            );

            return;
        }

        String firstSymbol =
                command.getArguments()
                        .get(0);

        String secondSymbol =
                command.getArguments()
                        .get(1);

        ToolRequest request =
                new ToolRequest(
                        sessionManager
                                .getCurrentUserId(),

                        ActionType.ETF_OVERLAP,

                        Map.of(
                                "firstSymbol",
                                firstSymbol,

                                "secondSymbol",
                                secondSymbol
                        )
                );

        ToolResult result =
                toolManager.execute(
                        request
                );

        if (!result.isSuccess()) {

            displayError(
                    result.getErrorMessage()
            );

            return;
        }

        if (!(result.getData()
                instanceof OverlapResult
                overlapResult)) {

            displayError(
                    "Unexpected ETF overlap "
                            + "response."
            );

            return;
        }

        displayOverlapResult(
                overlapResult
        );
    }

    private void displayOverlapResult(
            OverlapResult result) {

        BigDecimal overlapPercent =
                result.getOverlapWeight()
                        .multiply(
                                new BigDecimal(
                                        "100"
                                )
                        )
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        TerminalComponents.section(
                "ETF HOLDING OVERLAP"
        );

        TerminalComponents.row(
                "FIRST ETF",
                result.getFirstSymbol()
        );

        TerminalComponents.row(
                "SECOND ETF",
                result.getSecondSymbol()
        );

        TerminalComponents.row(
                "OVERLAP",
                TerminalTheme.cyan(
                        overlapPercent
                                + "%"
                )
        );

        if (result
                .getCommonHoldings()
                .isEmpty()) {

            TerminalComponents.separator();

            TerminalComponents.row(
                    "STATUS",
                    "No common holdings"
            );

            TerminalComponents.endSection();

            return;
        }

        TerminalComponents.separator();

        TerminalComponents.row(
                "SYMBOL",
                "DESCRIPTION / SHARED WEIGHT"
        );

        TerminalComponents.separator();

        result.getCommonHoldings()
                .stream()
                .limit(15)
                .forEach(
                        holding -> {

                            BigDecimal percent =
                                    holding
                                            .getWeight()
                                            .multiply(
                                                    new BigDecimal(
                                                            "100"
                                                    )
                                            )
                                            .setScale(
                                                    2,
                                                    RoundingMode.HALF_UP
                                            );

                            String description =
                                    truncate(
                                            holding
                                                    .getDescription(),
                                            28
                                    );

                            String value =
                                    String.format(
                                            "%-28s %6s%%",
                                            description,
                                            percent
                                    );

                            TerminalComponents.row(
                                    holding.getSymbol(),
                                    value
                            );
                        }
                );

        TerminalComponents.endSection();
    }

    private String centerText(
            String text,
            int width) {

        if (text.length() >= width) {
            return text;
        }

        int totalPadding =
                width - text.length();

        int leftPadding =
                totalPadding / 2;

        int rightPadding =
                totalPadding - leftPadding;

        return " ".repeat(leftPadding)
                + text
                + " ".repeat(rightPadding);
    }

    private String padText(
            String text,
            int width) {

        if (text.length() >= width) {
            return text.substring(
                    0,
                    width
            );
        }

        return text
                + " ".repeat(
                width - text.length()
        );
    }

    private String truncate(
            String value,
            int maxLength) {

        if (value == null) {
            return "";
        }

        if (value.length()
                <= maxLength) {

            return value;
        }

        return value.substring(
                0,
                maxLength - 3
        ) + "...";
    }

    // ========================================================
    // Session
    // ========================================================

    private void logout() {

        String username =
                sessionManager
                        .getCurrentUser()
                        .getUsername();

        sessionManager.endSession();

        TerminalTheme.clearScreen();

        printBanner();

        System.out.println();

        System.out.println(
                TerminalTheme.green(
                        "✓ Logged out "
                                + username
                                + "."
                )
        );

        waitForEnter();
    }

    private void exitApplication() {

        if (sessionManager
                .isAuthenticated()) {

            sessionManager.endSession();
        }

        running = false;
    }

    private void displayCurrentUser() {

        User user =
                sessionManager
                        .getCurrentUser();

        TerminalComponents.section(
                "CURRENT SESSION"
        );

        TerminalComponents.row(
                "USERNAME",
                user.getUsername()
        );

        TerminalComponents.row(
                "USER ID",
                String.valueOf(
                        user.getId()
                )
        );

        TerminalComponents.row(
                "STATUS",
                TerminalTheme.green(
                        "● CONNECTED"
                )
        );

        TerminalComponents.row(
                "LOGIN",
                sessionManager
                        .getCurrentSession()
                        .getLoginTime()
                        .toString()
        );

        TerminalComponents.endSection();
    }

    // ========================================================
    // Help
    // ========================================================

    private void displayHelp() {

        TerminalComponents.section(
                "MARKET DATA"
        );

        TerminalComponents.row(
                "/quote SYMBOL",
                "Current market quote"
        );

        TerminalComponents.row(
                "/overview SYMBOL",
                "Company/security overview"
        );

        TerminalComponents.endSection();

        TerminalComponents.section(
                "ETF ANALYTICS"
        );

        TerminalComponents.row(
                "/sector ETF",
                "Sector exposure"
        );

        TerminalComponents.row(
                "/overlap A B",
                "ETF holdings overlap"
        );

        TerminalComponents.endSection();

        TerminalComponents.section(
                "PORTFOLIO"
        );

        TerminalComponents.row(
                "/portfolio",
                "View portfolio"
        );

        TerminalComponents.row(
                "/portfolio add",
                "Add position"
        );

        TerminalComponents.row(
                "/portfolio remove",
                "Remove position"
        );

        TerminalComponents.endSection();

        TerminalComponents.section(
                "WATCHLIST"
        );

        TerminalComponents.row(
                "/watchlist",
                "View watchlist"
        );

        TerminalComponents.row(
                "/watchlist add",
                "Add symbol"
        );

        TerminalComponents.row(
                "/watchlist remove",
                "Remove symbol"
        );

        TerminalComponents.endSection();

        TerminalComponents.section(
                "SYSTEM"
        );

        TerminalComponents.row(
                "/whoami",
                "Current session"
        );

        TerminalComponents.row(
                "/logout",
                "Log out"
        );

        TerminalComponents.row(
                "/exit",
                "Exit terminal"
        );

        TerminalComponents.endSection();
    }

    // ========================================================
    // Input / Output
    // ========================================================

    private String readPassword(
            String prompt) {

        Console console =
                System.console();

        if (console != null) {

            char[] passwordChars =
                    console.readPassword(
                            prompt
                    );

            return new String(
                    passwordChars
            );
        }

        System.out.print(
                prompt
        );

        return scanner.nextLine();
    }

    @Override
    public void displayResponse(
            String response) {

        System.out.println();

        System.out.println(
                TerminalTheme.green(
                        "✓ "
                                + response
                )
        );
    }

    @Override
    public void displayError(
            String message) {

        System.out.println();

        System.out.println(
                TerminalTheme.red(
                        "✗ "
                                + message
                )
        );
    }

    private void printBanner() {

        System.out.println();

        System.out.println(
                TerminalTheme.cyan(
                        "╔══════════════════════════════════════════════════════════════╗"
                )
        );

        System.out.println(
                TerminalTheme.cyan("║")
                        + TerminalTheme.bold(
                        "                 MARKET INTELLIGENCE AGENT                  "
                )
                        + TerminalTheme.cyan("║")
        );

        System.out.println(
                TerminalTheme.cyan("║")
                        + TerminalTheme.dim(
                        "                       FICC TERMINAL                        "
                )
                        + TerminalTheme.cyan("║")
        );

        System.out.println(
                TerminalTheme.cyan(
                        "╚══════════════════════════════════════════════════════════════╝"
                )
        );

        System.out.println();

        System.out.println(
                TerminalTheme.dim(
                        "  Java 21 • SQLite • Agent Tool Architecture"
                )
        );
    }

    private void printDivider() {

        System.out.println(
                "------------------------------------------------------------"
        );
    }

    private void displayTerminalHeader() {

        final int width = 62;

        User user =
                sessionManager.getCurrentUser();

        String title =
                "MARKET INTELLIGENCE AGENT";

        String session =
                user.getUsername()
                        + "                    "
                        + "● CONNECTED";

        System.out.println();

        System.out.println(
                TerminalTheme.cyan(
                        "╔"
                                + "═".repeat(width)
                                + "╗"
                )
        );

        System.out.println(
                TerminalTheme.cyan("║")
                        + TerminalTheme.bold(
                        centerText(
                                title,
                                width
                        )
                )
                        + TerminalTheme.cyan("║")
        );

        System.out.println(
                TerminalTheme.cyan("║")
                        + padText(
                        "  " + session,
                        width
                )
                        + TerminalTheme.cyan("║")
        );

        System.out.println(
                TerminalTheme.cyan(
                        "╚"
                                + "═".repeat(width)
                                + "╝"
                )
        );
    }
}