package com.marketintel.ui;

public final class TerminalComponents {

    private static final int WIDTH = 62;

    private TerminalComponents() {
    }

    public static void section(
            String title) {

        String decorated =
                " " + title + " ";

        int remaining =
                WIDTH
                        - decorated.length()
                        - 2;

        if (remaining < 0) {
            remaining = 0;
        }

        System.out.println();

        System.out.println(
                TerminalTheme.cyan(
                        "┌─"
                                + decorated
                                + "─".repeat(remaining)
                                + "┐"
                )
        );
    }

    public static void endSection() {

        System.out.println(
                TerminalTheme.cyan(
                        "└"
                                + "─".repeat(WIDTH)
                                + "┘"
                )
        );
    }

    public static void row(
            String label,
            String value) {

        String plain =
                String.format(
                        "│ %-15s %-42s │",
                        label,
                        value
                );

        System.out.println(
                TerminalTheme.cyan("│")
                        + plain.substring(
                        1,
                        plain.length() - 1
                )
                        + TerminalTheme.cyan("│")
        );
    }

    public static void separator() {

        System.out.println(
                TerminalTheme.cyan(
                        "├"
                                + "─".repeat(WIDTH)
                                + "┤"
                )
        );
    }

    public static void rawRow(
            String label,
            String value) {

        System.out.print(
                TerminalTheme.cyan("│")
        );

        System.out.printf(
                " %-15s ",
                label
        );

        System.out.print(
                value
        );

        System.out.println();
    }
}