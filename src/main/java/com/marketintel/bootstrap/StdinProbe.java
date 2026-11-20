package com.marketintel.bootstrap;

import java.util.Locale;

/**
 * Detects a piped standard input without treating Maven's missing
 * {@link System#console()} as a pipe. {@code ./mvnw exec:java} often
 * has no console and must stay interactive.
 */
public final class StdinProbe {

    private StdinProbe() {
    }

    public static boolean isPiped() {
        String os = System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return false;
        }
        try {
            Process process = new ProcessBuilder("test", "-t", "0")
                    .redirectInput(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            return process.waitFor() != 0;
        } catch (Exception exception) {
            return false;
        }
    }
}
