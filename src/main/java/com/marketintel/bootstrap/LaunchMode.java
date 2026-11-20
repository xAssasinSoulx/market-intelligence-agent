package com.marketintel.bootstrap;

/**
 * Selects the process entry point. The command-line interface remains
 * the default so existing launches keep their current behavior.
 * Argument parsing lives in {@link CliOptions}.
 */
public enum LaunchMode {

    CLI,
    GUI
}
