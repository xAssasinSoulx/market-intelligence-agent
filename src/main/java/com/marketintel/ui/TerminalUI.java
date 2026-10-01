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

        printBanner();

        while (running) {

            if (!sessionManager
                    .isAuthenticated()) {

                showAuthenticationMenu();

            } else {

                showAuthenticatedTerminal();
            }
        }

        scanner.close();

        System.out.println();

        System.out.println(
                "Terminal closed."
        );
    }

    private void showAuthenticationMenu() {

        System.out.println();

        System.out.println(
                "1. Log In"
        );

        System.out.println(
                "2. Register"
        );

        System.out.println(
                "3. Exit"
        );

        System.out.println();

        System.out.print(
                "Select an option: "
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

            default ->
                    displayError(
                            "Invalid option. "
                                    + "Enter 1, 2, or 3."
                    );
        }
    }

    private void register() {

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

        System.out.println();

        System.out.println(
                "USER LOGIN"
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

        try {

            User user =
                    authService.authenticate(
                            username,
                            password
                    );

            sessionManager.startSession(
                    user
            );

            System.out.println();

            System.out.println(
                    "Authentication successful."
            );

            System.out.println(
                    "Welcome, "
                            + user.getUsername()
                            + "."
            );

        } catch (SecurityException e) {

            displayError(
                    "Invalid username "
                            + "or password."
            );

        } catch (RuntimeException e) {

            displayError(
                    "Unable to complete login."
            );
        }
    }

    private void showAuthenticatedTerminal() {

        System.out.println();

        System.out.print(
                "MI > "
        );

        String input =
                scanner.nextLine()
                        .trim();

        if (input.isBlank()) {
            return;
        }

        if (commandParser
                .isCommand(input)) {

            handleCommand(input);

            return;
        }

        System.out.println();

        System.out.println(
                "[Natural-language agent "
                        + "functionality will be "
                        + "connected in a later milestone.]"
        );
    }

    private void handleCommand(
            String input) {

        try {

            ParsedCommand parsed =
                    commandParser.parse(
                            input
                    );

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

        System.out.println();

        System.out.println(
                "PORTFOLIO"
        );

        printDivider();

        if (portfolio.getPositions()
                .isEmpty()) {

            System.out.println(
                    "Your portfolio is empty."
            );

            return;
        }

        System.out.printf(
                "%-10s %-15s %-10s%n",
                "SYMBOL",
                "QUANTITY",
                "TYPE"
        );

        printDivider();

        for (PortfolioPosition position
                : portfolio.getPositions()) {

            System.out.printf(
                    "%-10s %-15s %-10s%n",

                    position.getSymbol(),

                    position.getQuantity()
                            .stripTrailingZeros()
                            .toPlainString(),

                    position.getAssetType()
            );
        }
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

        System.out.println();

        System.out.println(
                "WATCHLIST"
        );

        printDivider();

        if (watchlist.getSymbols()
                .isEmpty()) {

            System.out.println(
                    "Your watchlist is empty."
            );

            return;
        }

        int index = 1;

        for (String symbol
                : watchlist.getSymbols()) {

            System.out.println(
                    index
                            + ". "
                            + symbol
            );

            index++;
        }
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

        System.out.println();

        System.out.println(
                "MARKET QUOTE"
        );

        printDivider();

        System.out.println(
                "Symbol         : "
                        + quote.getSymbol()
        );

        System.out.println(
                "Price          : "
                        + quote.getPrice()
                        .stripTrailingZeros()
                        .toPlainString()
        );

        System.out.println(
                "Previous Close : "
                        + quote.getPreviousClose()
                        .stripTrailingZeros()
                        .toPlainString()
        );

        System.out.println(
                "Change         : "
                        + quote.getPercentChange()
                        .stripTrailingZeros()
                        .toPlainString()
                        + "%"
        );
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

        System.out.println();

        System.out.println(
                "SECURITY OVERVIEW"
        );

        printDivider();

        System.out.println(
                "Symbol     : "
                        + overview.getSymbol()
        );

        System.out.println(
                "Name       : "
                        + overview.getName()
        );

        System.out.println(
                "Exchange   : "
                        + overview.getExchange()
        );

        System.out.println(
                "Currency   : "
                        + overview.getCurrency()
        );

        System.out.println(
                "Sector     : "
                        + overview.getSector()
        );

        System.out.println(
                "Industry   : "
                        + overview.getIndustry()
        );

        if (overview
                .getMarketCapitalization()
                != null) {

            System.out.println(
                    "Market Cap : "
                            + overview
                            .getMarketCapitalization()
                            .toPlainString()
            );
        }

        if (overview.getDescription()
                != null
                && !overview
                .getDescription()
                .isBlank()) {

            System.out.println();

            System.out.println(
                    overview.getDescription()
            );
        }
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

        System.out.println();

        System.out.println(
                "ETF SECTOR EXPOSURE - "
                        + result.getSymbol()
        );

        printDivider();

        if (result.getCategories()
                .isEmpty()) {

            System.out.println(
                    "No sector exposure data "
                            + "is available."
            );

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

                            System.out.printf(
                                    "%-30s %8s%%%n",
                                    entry.getKey(),
                                    percent
                            );
                        }
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

        System.out.println();

        System.out.println(
                "ETF HOLDING OVERLAP"
        );

        printDivider();

        System.out.println(
                "First ETF  : "
                        + result.getFirstSymbol()
        );

        System.out.println(
                "Second ETF : "
                        + result.getSecondSymbol()
        );

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

        System.out.println(
                "Overlap    : "
                        + overlapPercent
                        + "%"
        );

        System.out.println();

        if (result.getCommonHoldings()
                .isEmpty()) {

            System.out.println(
                    "No common holdings found."
            );

            return;
        }

        System.out.println(
                "TOP COMMON HOLDINGS"
        );

        printDivider();

        System.out.printf(
                "%-10s %-35s %10s%n",
                "SYMBOL",
                "DESCRIPTION",
                "SHARED"
        );

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

                            System.out.printf(
                                    "%-10s %-35s %9s%%%n",

                                    holding.getSymbol(),

                                    truncate(
                                            holding.getDescription(),
                                            35
                                    ),

                                    percent
                            );
                        }
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

        System.out.println();

        System.out.println(
                "Logged out "
                        + username
                        + "."
        );
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

        System.out.println();

        System.out.println(
                "CURRENT USER"
        );

        printDivider();

        System.out.println(
                "Username : "
                        + user.getUsername()
        );

        System.out.println(
                "User ID  : "
                        + user.getId()
        );

        System.out.println(
                "Login    : "
                        + sessionManager
                        .getCurrentSession()
                        .getLoginTime()
        );
    }

    // ========================================================
    // Help
    // ========================================================

    private void displayHelp() {

        System.out.println();

        System.out.println(
                "AVAILABLE COMMANDS"
        );

        printDivider();

        System.out.println(
                "/help"
                        + "                         "
                        + "Show available commands"
        );

        System.out.println(
                "/whoami"
                        + "                       "
                        + "Show current user"
        );

        System.out.println(
                "/quote SYMBOL"
                        + "                  "
                        + "Get market quote"
        );

        System.out.println(
                "/overview SYMBOL"
                        + "               "
                        + "Get security information"
        );

        System.out.println(
                "/sector ETF"
                        + "                    "
                        + "Analyze ETF sector exposure"
        );

        System.out.println(
                "/overlap ETF1 ETF2"
                        + "             "
                        + "Analyze ETF holding overlap"
        );

        System.out.println(
                "/portfolio"
                        + "                     "
                        + "View portfolio"
        );

        System.out.println(
                "/portfolio add SYMBOL QTY TYPE"
                        + " Add portfolio position"
        );

        System.out.println(
                "/portfolio remove SYMBOL"
                        + "       "
                        + "Remove portfolio position"
        );

        System.out.println(
                "/watchlist"
                        + "                     "
                        + "View watchlist"
        );

        System.out.println(
                "/watchlist add SYMBOL"
                        + "          "
                        + "Add watchlist symbol"
        );

        System.out.println(
                "/watchlist remove SYMBOL"
                        + "       "
                        + "Remove watchlist symbol"
        );

        System.out.println(
                "/logout"
                        + "                       "
                        + "End current session"
        );

        System.out.println(
                "/exit"
                        + "                         "
                        + "Exit application"
        );
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
                "[OK] "
                        + response
        );
    }

    @Override
    public void displayError(
            String message) {

        System.out.println();

        System.out.println(
                "[ERROR] "
                        + message
        );
    }

    private void printBanner() {

        System.out.println();

        System.out.println(
                "============================================================"
        );

        System.out.println(
                "                 MARKET INTELLIGENCE AGENT"
        );

        System.out.println(
                "                     TERMINAL v1.0"
        );

        System.out.println(
                "============================================================"
        );
    }

    private void printDivider() {

        System.out.println(
                "------------------------------------------------------------"
        );
    }
}