package com.marketintel.bootstrap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LaunchModeTest {

    @Test
    void defaultsToCommandLine() {
        assertEquals(LaunchMode.CLI, LaunchMode.parse(new String[0]));
        assertEquals(LaunchMode.CLI, LaunchMode.parse(null));
    }

    @Test
    void acceptsExplicitModes() {
        assertEquals(LaunchMode.CLI, LaunchMode.parse(new String[]{"--cli"}));
        assertEquals(LaunchMode.GUI, LaunchMode.parse(new String[]{"--gui"}));
    }

    @Test
    void rejectsUnknownArguments() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> LaunchMode.parse(new String[]{"--gui", "--cli"})
        );

        assertEquals(
                "Usage: java com.marketintel.Main [--cli | --gui]",
                exception.getMessage()
        );
    }
}
