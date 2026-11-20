package com.marketintel.bootstrap;

import java.util.ArrayList;
import java.util.List;

/**
 * Command-line options for the interactive desk and the batch runner.
 * The batch form matches {@code java -jar Agent.jar -LLMAPI -input task.txt}.
 */
public final class CliOptions {

    public static final String USAGE = """
            Usage:
              java -jar Agent.jar [--cli | --gui]
              java -jar Agent.jar -LLMAPI -input task.txt
              java -jar Agent.jar --user NAME --password SECRET -c "quote NVDA"

            Build the jar with: ./mvnw -DskipTests package
            The jar is target/Agent.jar. Main-Class is com.marketintel.Main.

            Options:
              --cli, -cli                 Interactive terminal. This is the default.
              --gui, -gui                 Swing window. Sign-in stays on that window.
              -input, --input <file>      Run commands from a file, then exit.
              -input -                    Read commands from standard input, then exit.
              --batch, -batch             Read commands from standard input, then exit.
              -c, --command <text>        Run one command, then exit. May be repeated.
              -LLMAPI, --LLMAPI, --llm    Request LLM features. The key is GEMINI_API_KEY.
              --guest, -guest             Use the local demo account guest / guest-demo.
              --user, -user <name>        Account for batch mode.
              --password, -password <v>   Password for --user. Prefer MI_PASSWORD.
              --register, -register       Create --user when that account does not exist.
              --database, -database <p>   SQLite file. Default: data/market-intelligence.db.
              --help, -h, -help           Show this help.

            Batch mode is selected by -input, -c, --batch, or a piped standard input.
            With no account flags and no MI_USERNAME, batch mode signs in as the
            local demo user so a task file runs without a prompt. Override that
            with --user and --password, or with MI_USERNAME and MI_PASSWORD.
            MI_GUEST=1 forces the demo account. MI_DATABASE selects the SQLite file.

            Lines in a task file are commands. A blank line is skipped.
            A line whose first character is # is a comment.
            The interactive verbs (quote, overview, news, why, portfolio, ...) work here.
            """;

    private final LaunchMode mode;
    private final boolean explicitMode;
    private final boolean batch;
    private final boolean readStdin;
    private final String inputPath;
    private final List<String> commands;
    private final boolean guest;
    private final String username;
    private final String password;
    private final boolean register;
    private final boolean llmApi;
    private final boolean help;
    private final String database;

    private CliOptions(
            LaunchMode mode,
            boolean explicitMode,
            boolean batch,
            boolean readStdin,
            String inputPath,
            List<String> commands,
            boolean guest,
            String username,
            String password,
            boolean register,
            boolean llmApi,
            boolean help,
            String database) {

        this.mode = mode;
        this.explicitMode = explicitMode;
        this.batch = batch;
        this.readStdin = readStdin;
        this.inputPath = inputPath;
        this.commands = List.copyOf(commands);
        this.guest = guest;
        this.username = username;
        this.password = password;
        this.register = register;
        this.llmApi = llmApi;
        this.help = help;
        this.database = database;
    }

    public static CliOptions parse(String[] args) {
        if (args == null) {
            args = new String[0];
        }

        LaunchMode mode = LaunchMode.CLI;
        boolean explicitMode = false;
        boolean sawCli = false;
        boolean sawGui = false;
        boolean batch = false;
        boolean readStdin = false;
        String inputPath = null;
        List<String> commands = new ArrayList<>();
        boolean guest = false;
        String username = null;
        String password = null;
        boolean register = false;
        boolean llmApi = false;
        boolean help = false;
        String database = null;

        for (int index = 0; index < args.length; index++) {
            String arg = args[index];
            switch (arg) {
                case "--help", "-h", "-help", "--usage" -> help = true;
                case "--cli", "-cli" -> {
                    sawCli = true;
                    explicitMode = true;
                    mode = LaunchMode.CLI;
                }
                case "--gui", "-gui" -> {
                    sawGui = true;
                    explicitMode = true;
                    mode = LaunchMode.GUI;
                }
                case "--batch", "-batch" -> {
                    batch = true;
                    readStdin = true;
                }
                case "--guest", "-guest" -> guest = true;
                case "--register", "-register" -> register = true;
                case "-LLMAPI", "--LLMAPI", "--llm", "-llm" -> llmApi = true;
                case "-input", "--input", "-i" -> {
                    if (inputPath != null) {
                        throw new IllegalArgumentException(
                                "Option -input was given more than once."
                        );
                    }
                    inputPath = requireValue(args, ++index, "-input");
                    batch = true;
                    if ("-".equals(inputPath)) {
                        readStdin = true;
                    }
                }
                case "-c", "--command", "-command" -> {
                    commands.add(requireValue(args, ++index, "-c"));
                    batch = true;
                }
                case "--user", "-user", "--username", "-username" ->
                        username = requireValue(args, ++index, "--user");
                case "--password", "-password" ->
                        password = requireValue(args, ++index, "--password");
                case "--database", "-database" ->
                        database = requireValue(args, ++index, "--database");
                default -> throw new IllegalArgumentException(unknown(arg));
            }
        }

        if (sawCli && sawGui) {
            throw new IllegalArgumentException(
                    "Pass only one of --cli or --gui."
            );
        }
        if (mode == LaunchMode.GUI && batch) {
            throw new IllegalArgumentException(
                    "The window cannot be combined with -input, -c, or --batch."
            );
        }
        if (guest && username != null) {
            throw new IllegalArgumentException(
                    "Use either --guest or --user, not both."
            );
        }
        if (password != null && username == null) {
            throw new IllegalArgumentException(
                    "--password requires --user."
            );
        }
        if (register && username == null) {
            throw new IllegalArgumentException(
                    "--register requires --user."
            );
        }

        return new CliOptions(
                mode,
                explicitMode,
                batch,
                readStdin,
                inputPath,
                commands,
                guest,
                username,
                password,
                register,
                llmApi,
                help,
                database
        );
    }

    /**
     * Batch mode that reads standard input. Used when the process is piped
     * and the user did not already choose a mode.
     */
    public CliOptions withStdinBatch() {
        return new CliOptions(
                LaunchMode.CLI,
                explicitMode,
                true,
                true,
                inputPath,
                commands,
                guest,
                username,
                password,
                register,
                llmApi,
                help,
                database
        );
    }

    public LaunchMode mode() {
        return mode;
    }

    public boolean explicitMode() {
        return explicitMode;
    }

    public boolean batch() {
        return batch;
    }

    public boolean readStdin() {
        return readStdin;
    }

    public String inputPath() {
        return inputPath;
    }

    public List<String> commands() {
        return commands;
    }

    public boolean guest() {
        return guest;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public boolean register() {
        return register;
    }

    public boolean llmApi() {
        return llmApi;
    }

    public boolean help() {
        return help;
    }

    public String database() {
        return database;
    }

    public static String databaseUrl(
            CliOptions options,
            java.util.function.Function<String, String> environment) {

        String selected = options.database();
        if (selected == null || selected.isBlank()) {
            String fromEnv = environment.apply("MI_DATABASE");
            if (fromEnv != null && !fromEnv.isBlank()) {
                selected = fromEnv.trim();
            }
        }
        if (selected == null || selected.isBlank()) {
            return ApplicationContext.DEFAULT_DATABASE_URL;
        }
        if (selected.startsWith("jdbc:")) {
            return selected;
        }
        return "jdbc:sqlite:" + selected;
    }

    private static String requireValue(
            String[] args,
            int index,
            String flag) {

        if (index >= args.length) {
            throw new IllegalArgumentException(
                    "Option " + flag + " needs a value."
            );
        }
        String value = args[index];
        if (value.startsWith("-") && !"-".equals(value)) {
            throw new IllegalArgumentException(
                    "Option " + flag + " needs a value."
            );
        }
        return value;
    }

    private static String unknown(String arg) {
        if (arg.startsWith("-")) {
            return "Unknown option: " + arg;
        }
        return "Unexpected argument. Put commands in the task file or after -c. "
                + "API keys belong in environment variables, not on the command line.";
    }
}
