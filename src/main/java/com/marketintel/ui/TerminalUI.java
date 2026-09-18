package com.marketintel.ui;

import com.marketintel.auth.AuthService;
import com.marketintel.auth.SessionManager;
import com.marketintel.model.User;

import java.io.Console;
import java.util.Scanner;

public class TerminalUI implements UserInterface {

    private final AuthService authService;
    private final SessionManager sessionManager;
    private final CommandParser commandParser;
    private final Scanner scanner;

    private boolean running = true;

    public TerminalUI(
            AuthService authService,
            SessionManager sessionManager,
            CommandParser commandParser) {

        this.authService = authService;
        this.sessionManager = sessionManager;
        this.commandParser = commandParser;
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void start() {

        printBanner();

        while (running) {

            if (!sessionManager.isAuthenticated()) {
                showAuthenticationMenu();
            } else {
                showAuthenticatedTerminal();
            }
        }

        scanner.close();

        System.out.println();
        System.out.println("Terminal closed.");
    }

    private void showAuthenticationMenu() {

        System.out.println();
        System.out.println("1. Log In");
        System.out.println("2. Register");
        System.out.println("3. Exit");
        System.out.println();

        System.out.print("Select an option: ");

        String input = scanner.nextLine().trim();

        switch (input) {
            case "1" -> login();
            case "2" -> register();
            case "3" -> running = false;
            default -> displayError(
                    "Invalid option. Enter 1, 2, or 3."
            );
        }
    }

    private void register() {

        System.out.println();
        System.out.println("CREATE ACCOUNT");
        printDivider();

        System.out.print("Username: ");
        String username =
                scanner.nextLine().trim();

        String password =
                readPassword("Password: ");

        String confirmation =
                readPassword("Confirm password: ");

        if (!password.equals(confirmation)) {
            displayError("Passwords do not match.");
            return;
        }

        try {

            User user =
                    authService.register(
                            username,
                            password
                    );

            System.out.println();
            displayResponse(
                    "Account created successfully for "
                            + user.getUsername()
                            + "."
            );

        } catch (IllegalArgumentException e) {

            displayError(e.getMessage());

        } catch (RuntimeException e) {

            displayError(
                    "Unable to create account."
            );
        }
    }

    private void login() {

        System.out.println();
        System.out.println("USER LOGIN");
        printDivider();

        System.out.print("Username: ");

        String username =
                scanner.nextLine().trim();

        String password =
                readPassword("Password: ");

        try {

            User user =
                    authService.authenticate(
                            username,
                            password
                    );

            sessionManager.startSession(user);

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
                    "Invalid username or password."
            );

        } catch (RuntimeException e) {

            displayError(
                    "Unable to complete login."
            );
        }
    }

    private void showAuthenticatedTerminal() {

        System.out.println();
        System.out.print("MI > ");

        String input =
                scanner.nextLine().trim();

        if (input.isBlank()) {
            return;
        }

        if (commandParser.isCommand(input)) {
            handleCommand(input);
            return;
        }

        System.out.println();
        System.out.println(
                "[Agent functionality will be connected "
                        + "in a later milestone.]"
        );
    }

    private void handleCommand(String input) {

        try {

            ParsedCommand parsed =
                    commandParser.parse(input);

            switch (parsed.getCommand()) {

                case "help" ->
                        displayHelp();

                case "whoami" ->
                        displayCurrentUser();

                case "logout" ->
                        logout();

                case "exit" ->
                        exitApplication();

                default ->
                        displayError(
                                "Unknown command: /"
                                        + parsed.getCommand()
                        );
            }

        } catch (IllegalArgumentException e) {

            displayError(e.getMessage());
        }
    }

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

        if (sessionManager.isAuthenticated()) {
            sessionManager.endSession();
        }

        running = false;
    }

    private void displayCurrentUser() {

        User user =
                sessionManager.getCurrentUser();

        System.out.println();
        System.out.println("CURRENT USER");
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

    private void displayHelp() {

        System.out.println();
        System.out.println("AVAILABLE COMMANDS");
        printDivider();

        System.out.println(
                "/help       Show available commands"
        );

        System.out.println(
                "/whoami     Show current user"
        );

        System.out.println(
                "/logout     End the current session"
        );

        System.out.println(
                "/exit       Exit the application"
        );

        System.out.println();
        System.out.println(
                "Financial commands will be added "
                        + "in later milestones."
        );
    }

    private String readPassword(String prompt) {

        Console console =
                System.console();

        if (console != null) {

            char[] passwordChars =
                    console.readPassword(prompt);

            return new String(passwordChars);
        }

        /*
         * IntelliJ commonly does not expose System.console().
         * Fall back to normal scanner input during development.
         */
        System.out.print(prompt);

        return scanner.nextLine();
    }

    @Override
    public void displayResponse(String response) {

        System.out.println();
        System.out.println("[OK] " + response);
    }

    @Override
    public void displayError(String message) {

        System.out.println();
        System.out.println("[ERROR] " + message);
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