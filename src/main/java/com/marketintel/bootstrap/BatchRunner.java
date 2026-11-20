package com.marketintel.bootstrap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs one or more terminal commands and then exits.
 * Output goes to standard output. Prompts are not used.
 */
public final class BatchRunner {

    private BatchRunner() {
    }

    public static int run(
            ApplicationContext context,
            CliOptions options) {

        final List<String> lines;
        try {
            lines = readCommands(options);
        } catch (IOException exception) {
            System.err.println(exception.getMessage());
            return 2;
        }

        if (lines.stream().allMatch(BatchRunner::ignored)) {
            System.err.println("No commands to run.");
            return 2;
        }

        final BatchSession.Login login;
        try {
            login = BatchSession.signIn(context, options);
        } catch (IllegalArgumentException | SecurityException exception) {
            System.err.println(exception.getMessage());
            return 2;
        }

        if (login.created()) {
            System.out.println(
                    "Created local account " + login.username() + "."
            );
        }
        if (login.demoAccount()) {
            System.out.println(
                    "Signed in as " + login.username()
                            + " (local demo account). "
                            + "Pass --user and --password to use another account."
            );
        } else {
            System.out.println(
                    "Signed in as " + login.username() + "."
            );
        }
        if (options.llmApi()) {
            if (context.isExplanationAvailable()) {
                System.out.println(
                        "LLM: Gemini is configured (-LLMAPI)."
                );
            } else {
                System.out.println(
                        "LLM: -LLMAPI was set, but GEMINI_API_KEY is unset. "
                                + "Movement notes stay unavailable."
                );
            }
        }

        return context.runBatch(lines);
    }

    static List<String> readCommands(CliOptions options)
            throws IOException {

        List<String> lines = new ArrayList<>();
        if (options.inputPath() != null
                && !"-".equals(options.inputPath())) {
            Path path = Path.of(options.inputPath());
            if (!Files.isRegularFile(path)) {
                throw new IOException(
                        "Input file not found: " + path.toAbsolutePath()
                );
            }
            lines.addAll(Files.readAllLines(path, StandardCharsets.UTF_8));
        } else if (options.readStdin()) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(System.in, StandardCharsets.UTF_8)
            );
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        lines.addAll(options.commands());
        return lines;
    }

    static boolean ignored(String line) {
        if (line == null) {
            return true;
        }
        String trimmed = line.trim();
        return trimmed.isEmpty() || trimmed.startsWith("#");
    }
}
