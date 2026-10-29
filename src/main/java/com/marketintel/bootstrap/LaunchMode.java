package com.marketintel.bootstrap;

/**
 * Selects the process entry point. The command-line interface remains
 * the default so existing launches keep their current behavior.
 */
public enum LaunchMode {

    CLI,
    GUI;

    public static LaunchMode parse(String[] args) {

        if (args == null || args.length == 0) {
            return CLI;
        }

        if (args.length == 1 && "--cli".equals(args[0])) {
            return CLI;
        }

        if (args.length == 1 && "--gui".equals(args[0])) {
            return GUI;
        }

        throw new IllegalArgumentException(
                "Usage: java com.marketintel.Main [--cli | --gui]"
        );
    }
}
