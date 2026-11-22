package com.marketintel.bootstrap;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliOptionsTest {

    @Test
    void defaultsToTheInteractiveTerminal() {
        CliOptions empty = CliOptions.parse(new String[0]);
        CliOptions missing = CliOptions.parse(null);

        assertEquals(LaunchMode.CLI, empty.mode());
        assertEquals(LaunchMode.CLI, missing.mode());
        assertFalse(empty.batch());
        assertFalse(empty.explicitMode());
        assertFalse(missing.batch());
    }

    @Test
    void acceptsTheInteractiveSwitches() {
        assertEquals(
                LaunchMode.CLI,
                CliOptions.parse(new String[]{"--cli"}).mode()
        );
        assertEquals(
                LaunchMode.GUI,
                CliOptions.parse(new String[]{"--gui"}).mode()
        );
        assertTrue(CliOptions.parse(new String[]{"--cli"}).explicitMode());
    }

    @Test
    void acceptsTheProfessorBatchForm() {
        CliOptions options = CliOptions.parse(
                new String[]{"-LLMAPI", "-input", "task.txt"}
        );

        assertTrue(options.batch());
        assertTrue(options.llmApi());
        assertEquals("task.txt", options.inputPath());
        assertFalse(options.readStdin());
        assertEquals(LaunchMode.CLI, options.mode());
        assertTrue(options.commands().isEmpty());
    }

    @Test
    void readsStandardInputForDashAndBatch() {
        CliOptions dash = CliOptions.parse(new String[]{"-input", "-"});
        CliOptions batch = CliOptions.parse(new String[]{"--batch"});

        assertTrue(dash.batch());
        assertTrue(dash.readStdin());
        assertTrue(batch.batch());
        assertTrue(batch.readStdin());
    }

    @Test
    void collectsRepeatedInlineCommands() {
        CliOptions options = CliOptions.parse(new String[]{
                "-c", "portfolio",
                "--command", "whoami"
        });

        assertEquals(2, options.commands().size());
        assertEquals("portfolio", options.commands().get(0));
        assertEquals("whoami", options.commands().get(1));
        assertTrue(options.batch());
    }

    @Test
    void rejectsAWindowCombinedWithATaskFile() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> CliOptions.parse(new String[]{"--gui", "-input", "task.txt"})
        );

        assertTrue(exception.getMessage().contains("window"));
    }

    @Test
    void rejectsBothInterfaces() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CliOptions.parse(new String[]{"--gui", "--cli"})
        );
    }

    @Test
    void rejectsAMissingInputValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CliOptions.parse(new String[]{"-input"})
        );
    }

    @Test
    void doesNotEchoAPositionalSecret() {
        String secret = "do-not-echo-this-value";
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> CliOptions.parse(new String[]{secret})
        );

        assertFalse(exception.getMessage().contains(secret));
        assertTrue(exception.getMessage().contains("environment"));
    }

    @Test
    void resolvesTheDatabaseLocation() {
        assertEquals(
                ApplicationContext.DEFAULT_DATABASE_URL,
                CliOptions.databaseUrl(CliOptions.parse(null), key -> null)
        );
        assertEquals(
                "jdbc:sqlite:/tmp/desk.db",
                CliOptions.databaseUrl(
                        CliOptions.parse(new String[]{"--database", "/tmp/desk.db"}),
                        key -> null
                )
        );
        assertEquals(
                "jdbc:sqlite:memory:one",
                CliOptions.databaseUrl(
                        CliOptions.parse(new String[0]),
                        key -> "MI_DATABASE".equals(key)
                                ? "jdbc:sqlite:memory:one"
                                : null
                )
        );
    }

    @Test
    void demoAccountIsTheBatchDefault() {
        BatchSession.Resolved resolved = BatchSession.resolve(
                CliOptions.parse(new String[]{"-input", "task.txt"}),
                key -> null
        );

        assertTrue(resolved.guest());
        assertEquals(BatchSession.GUEST_USERNAME, resolved.username());
    }

    @Test
    void environmentAccountOverridesTheDemoUser() {
        Map<String, String> env = Map.of(
                "MI_USERNAME", "grader",
                "MI_PASSWORD", "grader-demo"
        );

        BatchSession.Resolved resolved = BatchSession.resolve(
                CliOptions.parse(new String[]{"-c", "whoami"}),
                env::get
        );

        assertFalse(resolved.guest());
        assertEquals("grader", resolved.username());
        assertEquals("grader-demo", resolved.password());
    }

    @Test
    void explicitUserBeatsTheGuestEnvironmentFlag() {
        BatchSession.Resolved resolved = BatchSession.resolve(
                CliOptions.parse(new String[]{
                        "--user", "grader",
                        "--password", "grader-demo",
                        "-c", "whoami"
                }),
                key -> "MI_GUEST".equals(key) ? "1" : null
        );

        assertFalse(resolved.guest());
        assertEquals("grader", resolved.username());
    }

    @Test
    void guestFlagBeatsAnEnvironmentAccount() {
        BatchSession.Resolved resolved = BatchSession.resolve(
                CliOptions.parse(new String[]{"--guest", "-c", "whoami"}),
                key -> switch (key) {
                    case "MI_USERNAME" -> "grader";
                    case "MI_PASSWORD" -> "grader-demo";
                    default -> null;
                }
        );

        assertTrue(resolved.guest());
    }
}
