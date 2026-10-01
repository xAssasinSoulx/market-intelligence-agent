package com.marketintel.ui;

public final class TerminalTheme {

    private TerminalTheme() {
    }

    public static final String RESET = "\u001B[0m";

    public static final String BOLD = "\u001B[1m";
    public static final String DIM = "\u001B[2m";

    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    public static final String BRIGHT_BLACK = "\u001B[90m";
    public static final String BRIGHT_RED = "\u001B[91m";
    public static final String BRIGHT_GREEN = "\u001B[92m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";
    public static final String BRIGHT_BLUE = "\u001B[94m";
    public static final String BRIGHT_MAGENTA = "\u001B[95m";
    public static final String BRIGHT_CYAN = "\u001B[96m";
    public static final String BRIGHT_WHITE = "\u001B[97m";

    private static final boolean ANSI_ENABLED =
            System.getenv("NO_COLOR") == null;

    public static String color(
            String text,
            String color) {

        if (!ANSI_ENABLED) {
            return text;
        }

        return color
                + text
                + RESET;
    }

    public static String bold(String text) {
        return color(text, BOLD);
    }

    public static String cyan(String text) {
        return color(text, BRIGHT_CYAN);
    }

    public static String green(String text) {
        return color(text, BRIGHT_GREEN);
    }

    public static String red(String text) {
        return color(text, BRIGHT_RED);
    }

    public static String yellow(String text) {
        return color(text, BRIGHT_YELLOW);
    }

    public static String blue(String text) {
        return color(text, BRIGHT_BLUE);
    }

    public static String magenta(String text) {
        return color(text, BRIGHT_MAGENTA);
    }

    public static String dim(String text) {
        return color(text, DIM);
    }

    /**
     * Switch to the terminal's alternate screen buffer.
     *
     * This gives the application its own clean screen.
     * When the program exits, the user's original shell
     * contents are restored.
     */
    public static void enterAlternateScreen() {

        if (!ANSI_ENABLED) {
            return;
        }

        System.out.print(
                "\u001B[?1049h"
        );

        System.out.flush();

        clearScreen();
    }

    /**
     * Return to the normal terminal screen.
     */
    public static void exitAlternateScreen() {

        if (!ANSI_ENABLED) {
            return;
        }

        System.out.print(
                "\u001B[?1049l"
        );

        System.out.flush();
    }

    /**
     * Clear the current screen and move the cursor
     * back to the upper-left corner.
     */
    public static void clearScreen() {

        if (!ANSI_ENABLED) {
            return;
        }

        System.out.print(
                "\u001B[H"
                        + "\u001B[2J"
                        + "\u001B[3J"
        );

        System.out.flush();
    }
}