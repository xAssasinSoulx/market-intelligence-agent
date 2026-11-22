package com.marketintel.ui;

import com.marketintel.Main;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchCliTest {

    @TempDir
    Path directory;

    @Test
    void runsTheRepositoryTaskFileWithoutAPrompt() throws Exception {
        Path database = directory.resolve("sample.db");
        Capture capture = run(
                "--database", database.toString(),
                "-LLMAPI",
                "-input", "task.txt"
        );

        assertEquals(0, capture.code);
        assertTrue(capture.out.contains("Created local account guest."));
        assertTrue(capture.out.contains("Signed in as guest"));
        assertTrue(capture.out.contains("NVDA"));
        assertTrue(capture.out.contains("QQQ"));
        assertTrue(capture.out.contains("AAPL"));
        assertTrue(capture.out.contains("Logged out"));
        assertTrue(
                capture.out.contains("GEMINI_API_KEY")
                        || capture.out.contains("Gemini is configured")
        );
        assertFalse(capture.out.contains("> quote"));
        assertFalse(capture.out.contains("> #"));
        assertFalse(capture.out.contains(com.marketintel.bootstrap.BatchSession.GUEST_PASSWORD));
        assertFalse(capture.err.contains(com.marketintel.bootstrap.BatchSession.GUEST_PASSWORD));
    }

    @Test
    void secondGuestRunReusesTheAccount() {
        Path database = directory.resolve("reuse.db");
        String[] args = {
                "--database", database.toString(),
                "-c", "whoami"
        };

        Capture first = run(args);
        Capture second = run(args);

        assertEquals(0, first.code);
        assertEquals(0, second.code);
        assertTrue(first.out.contains("Created local account guest."));
        assertFalse(second.out.contains("Created local account"));
        assertTrue(second.out.contains("guest"));
    }

    @Test
    void registerThenSignsInWithThatAccount() {
        Path database = directory.resolve("grader.db");
        Capture created = run(
                "--database", database.toString(),
                "--user", "grader",
                "--password", "grader-demo",
                "--register",
                "-c", "whoami"
        );
        Capture again = run(
                "--database", database.toString(),
                "--user", "grader",
                "--password", "grader-demo",
                "-c", "whoami"
        );
        Capture rejected = run(
                "--database", database.toString(),
                "--user", "grader",
                "--password", "wrong-pass",
                "-c", "whoami"
        );

        assertEquals(0, created.code);
        assertTrue(created.out.contains("Created local account grader."));
        assertTrue(created.out.contains("Signed in as grader."));
        assertFalse(created.out.contains("demo account"));
        assertEquals(0, again.code);
        assertFalse(again.out.contains("Created local account"));
        assertEquals(2, rejected.code);
        assertTrue(rejected.err.contains("Invalid username or password."));
    }

    @Test
    void inlineCommandsAndABadCommandShareOneRun() throws Exception {
        Path database = directory.resolve("inline.db");
        Path task = directory.resolve("task.txt");
        Files.writeString(task, """
                portfolio add NVDA 10 STOCK
                # portfolio add SKIP 1 STOCK
                /frobnicate
                portfolio
                """, StandardCharsets.UTF_8);

        Capture capture = run(
                "--database", database.toString(),
                "-input", task.toString(),
                "-c", "watchlist add AAPL"
        );

        assertEquals(1, capture.code);
        assertTrue(capture.out.contains("NVDA added to portfolio."));
        assertTrue(capture.out.contains("Unknown command"));
        assertTrue(capture.out.contains("AAPL added to watchlist."));
        assertFalse(capture.out.contains("SKIP"));
    }

    @Test
    void logoutStopsTheRestOfTheFile() throws Exception {
        Path database = directory.resolve("stop.db");
        Path task = directory.resolve("task.txt");
        Files.writeString(task, """
                whoami
                logout
                portfolio add LATE 1 STOCK
                """, StandardCharsets.UTF_8);

        Capture capture = run(
                "--database", database.toString(),
                "--guest",
                "-input", task.toString()
        );

        assertEquals(0, capture.code);
        assertTrue(capture.out.contains("Logged out"));
        assertFalse(capture.out.contains("LATE"));
    }

    @Test
    void quoteWithoutASymbolDoesNotCallAProvider() {
        Path database = directory.resolve("usage.db");
        Capture capture = run(
                "--database", database.toString(),
                "-c", "quote"
        );

        assertEquals(1, capture.code);
        assertTrue(capture.out.contains("Usage: quote SYMBOL"));
    }

    @Test
    void missingQuoteProviderFailsLocally() {
        Assumptions.assumeTrue(
                System.getenv("FINNHUB_API_KEY") == null
                        && System.getenv("TWELVE_DATA_API_KEY") == null
                        && System.getenv("ALPHA_VANTAGE_API_KEY") == null,
                "A quote key is set, so this check would call a provider."
        );
        Path database = directory.resolve("offline-quote.db");
        Capture capture = run(
                "--database", database.toString(),
                "-c", "quote NVDA"
        );

        assertEquals(1, capture.code);
        assertTrue(capture.out.contains(
                "No tool is available for action: MARKET_QUOTE"
        ));
        assertTrue(capture.err.contains(
                "No quote provider is configured."
        ));
        assertFalse(capture.out.contains("https://"));
        assertFalse(capture.err.contains("https://"));
    }

    @Test
    void readsCommandsFromStandardInput() {
        Path database = directory.resolve("stdin.db");
        InputStream original = System.in;
        System.setIn(new ByteArrayInputStream(
                "whoami\n\n# skip\nportfolio\n".getBytes(StandardCharsets.UTF_8)
        ));
        try {
            Capture capture = run(
                    "--database", database.toString(),
                    "--batch"
            );
            assertEquals(0, capture.code);
            assertTrue(capture.out.contains("Signed in as guest"));
            assertTrue(capture.out.contains("PORTFOLIO"));
            assertFalse(capture.out.contains("> #"));
        } finally {
            System.setIn(original);
        }
    }

    @Test
    void missingFileAndEmptyFileAreUsageErrors() throws Exception {
        Path database = directory.resolve("empty.db");
        Capture missing = run(
                "--database", database.toString(),
                "-input", directory.resolve("missing.txt").toString()
        );
        Path empty = directory.resolve("empty.txt");
        Files.writeString(empty, "\n# only a comment\n", StandardCharsets.UTF_8);
        Capture blank = run(
                "--database", database.toString(),
                "-input", empty.toString()
        );

        assertEquals(2, missing.code);
        assertTrue(missing.err.contains("Input file not found"));
        assertEquals(2, blank.code);
        assertTrue(blank.err.contains("No commands to run."));
    }

    @Test
    void helpExitsBeforeOpeningADatabase() {
        Capture capture = run("--help");

        assertEquals(0, capture.code);
        assertTrue(capture.out.contains("-input"));
        assertTrue(capture.out.contains("task.txt"));
        assertTrue(capture.out.contains("target/Agent.jar"));
    }

    @Test
    void unknownArgumentsExitWithUsage() {
        Capture capture = run("--not-a-flag");

        assertEquals(2, capture.code);
        assertTrue(capture.err.contains("Unknown option: --not-a-flag"));
        assertTrue(capture.err.contains("-input"));
    }

    private Capture run(String... args) {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
        try {
            int code = Main.run(args);
            return new Capture(
                    code,
                    out.toString(StandardCharsets.UTF_8),
                    err.toString(StandardCharsets.UTF_8)
            );
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    private record Capture(int code, String out, String err) {
    }
}