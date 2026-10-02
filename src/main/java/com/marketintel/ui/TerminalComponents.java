package com.marketintel.ui;

public final class TerminalComponents {

    /*
     * Number of visible characters between
     * the left and right borders.
     */
    private static final int CONTENT_WIDTH = 62;

    private static final int LABEL_WIDTH = 22;

    private static final int VALUE_WIDTH =
            CONTENT_WIDTH
                    - LABEL_WIDTH
                    - 3;

    private static final String ANSI_REGEX =
            "\u001B\\[[;\\d]*m";

    private TerminalComponents() {
    }

    // ========================================================
    // Sections
    // ========================================================

    public static void section(
            String title) {

        String safeTitle =
                title == null
                        ? ""
                        : title;

        int maxTitleLength =
                CONTENT_WIDTH - 4;

        if (safeTitle.length()
                > maxTitleLength) {

            safeTitle =
                    safeTitle.substring(
                            0,
                            maxTitleLength - 3
                    )
                            + "...";
        }

        int remaining =
                CONTENT_WIDTH
                        - safeTitle.length()
                        - 3;

        String line =
                "┌─ "
                        + safeTitle
                        + " "
                        + "─".repeat(
                        Math.max(
                                0,
                                remaining
                        )
                )
                        + "┐";

        System.out.println();

        System.out.println(
                TerminalTheme.cyan(
                        line
                )
        );
    }

    public static void endSection() {

        System.out.println(
                TerminalTheme.cyan(
                        "└"
                                + "─".repeat(
                                CONTENT_WIDTH
                        )
                                + "┘"
                )
        );
    }

    public static void separator() {

        System.out.println(
                TerminalTheme.cyan(
                        "├"
                                + "─".repeat(
                                CONTENT_WIDTH
                        )
                                + "┤"
                )
        );
    }

    // ========================================================
    // Rows
    // ========================================================

    public static void row(
            String label,
            String value) {

        String safeLabel =
                label == null
                        ? ""
                        : label;

        String safeValue =
                value == null
                        ? ""
                        : value;

        safeLabel =
                fitPlainText(
                        safeLabel,
                        LABEL_WIDTH
                );

        safeValue =
                fitVisibleText(
                        safeValue,
                        VALUE_WIDTH
                );

        String paddedLabel =
                padRight(
                        safeLabel,
                        LABEL_WIDTH
                );

        String paddedValue =
                padRightVisible(
                        safeValue,
                        VALUE_WIDTH
                );

        System.out.println(
                TerminalTheme.cyan("│")
                        + " "
                        + paddedLabel
                        + " "
                        + paddedValue
                        + " "
                        + TerminalTheme.cyan("│")
        );
    }

    /*
     * Kept for compatibility with existing UI code.
     *
     * row() is now ANSI-aware, so rawRow() can simply
     * delegate to it.
     */
    public static void rawRow(
            String label,
            String value) {

        row(
                label,
                value
        );
    }

    public static void emptyRow() {

        row(
                "",
                ""
        );
    }

    // ========================================================
    // Wrapped text
    // ========================================================

    public static void wrappedRow(
            String label,
            String text) {

        if (text == null
                || text.isBlank()) {

            row(
                    label,
                    ""
            );

            return;
        }

        String[] words =
                text.trim()
                        .split("\\s+");

        StringBuilder current =
                new StringBuilder();

        boolean firstLine = true;

        for (String word : words) {

            if (current.isEmpty()) {

                current.append(
                        word
                );

                continue;
            }

            if (current.length()
                    + 1
                    + word.length()
                    <= VALUE_WIDTH) {

                current.append(" ")
                        .append(word);

            } else {

                row(
                        firstLine
                                ? label
                                : "",
                        current.toString()
                );

                firstLine = false;

                current =
                        new StringBuilder(
                                word
                        );
            }
        }

        if (!current.isEmpty()) {

            row(
                    firstLine
                            ? label
                            : "",
                    current.toString()
            );
        }
    }

    // ========================================================
    // Formatting Helpers
    // ========================================================

    private static String fitPlainText(
            String value,
            int width) {

        if (value.length()
                <= width) {

            return value;
        }

        if (width <= 3) {

            return value.substring(
                    0,
                    width
            );
        }

        return value.substring(
                0,
                width - 3
        ) + "...";
    }

    private static String fitVisibleText(
            String value,
            int width) {

        int visibleLength =
                visibleLength(
                        value
                );

        if (visibleLength <= width) {

            return value;
        }

        /*
         * If a coloured value is too long, strip ANSI
         * before truncation. Long coloured values are
         * uncommon, and this keeps layout deterministic.
         */
        String plain =
                stripAnsi(
                        value
                );

        return fitPlainText(
                plain,
                width
        );
    }

    private static String padRight(
            String value,
            int width) {

        int remaining =
                width
                        - value.length();

        if (remaining <= 0) {

            return value;
        }

        return value
                + " ".repeat(
                remaining
        );
    }

    private static String padRightVisible(
            String value,
            int width) {

        int remaining =
                width
                        - visibleLength(
                        value
                );

        if (remaining <= 0) {

            return value;
        }

        return value
                + " ".repeat(
                remaining
        );
    }

    private static int visibleLength(
            String value) {

        return stripAnsi(
                value
        ).length();
    }

    private static String stripAnsi(
            String value) {

        return value.replaceAll(
                ANSI_REGEX,
                ""
        );
    }
}