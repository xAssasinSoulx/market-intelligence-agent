package com.marketintel;

import com.marketintel.bootstrap.ApplicationContext;
import com.marketintel.bootstrap.BatchRunner;
import com.marketintel.bootstrap.BatchSession;
import com.marketintel.bootstrap.CliOptions;
import com.marketintel.bootstrap.LaunchMode;
import com.marketintel.bootstrap.StdinProbe;
import com.marketintel.gui.MarketIntelFrame;

public class Main {

    public static void main(String[] args) {
        int code = run(args);
        if (code != 0) {
            System.exit(code);
        }
    }

    /**
     * Runs the desk and returns a process status.
     * Zero is success. One means a batch command failed.
     * Two means the arguments or the sign-in could not be used.
     * Tests call this directly so a failure does not exit the JVM.
     */
    public static int run(String[] args) {
        final CliOptions parsed;
        try {
            parsed = CliOptions.parse(args);
        } catch (IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
            System.err.println(CliOptions.USAGE);
            return 2;
        }

        if (parsed.help()) {
            System.out.println(CliOptions.USAGE);
            return 0;
        }

        CliOptions options = parsed;
        if (!parsed.explicitMode()
                && !parsed.batch()
                && StdinProbe.isPiped()) {
            options = parsed.withStdinBatch();
        }

        ApplicationContext context = ApplicationContext.create(
                CliOptions.databaseUrl(options, System::getenv)
        );

        if (options.mode() == LaunchMode.GUI) {
            MarketIntelFrame.launch(context);
            return 0;
        }

        if (options.batch()) {
            return BatchRunner.run(context, options);
        }

        if (options.guest() || options.username() != null) {
            try {
                BatchSession.Login login =
                        BatchSession.signIn(context, options);
                if (login.created()) {
                    System.out.println(
                            "Created local account "
                                    + login.username()
                                    + "."
                    );
                }
            } catch (IllegalArgumentException | SecurityException exception) {
                System.err.println(exception.getMessage());
                return 2;
            }
        }

        context.createTerminalInterface().start();
        return 0;
    }
}
