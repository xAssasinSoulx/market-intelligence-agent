package com.marketintel.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandParserTest {

    private final CommandParser parser =
            new CommandParser();

    @Test
    void recognizesCommand() {

        assertTrue(
                parser.isCommand("/logout")
        );
    }

    @Test
    void naturalLanguageIsNotCommand() {

        assertFalse(
                parser.isCommand(
                        "What is NVDA trading at?"
                )
        );
    }

    @Test
    void parsesCommandName() {

        ParsedCommand command =
                parser.parse("/logout");

        assertEquals(
                "logout",
                command.getCommand()
        );
    }

    @Test
    void parsesCommandArguments() {

        ParsedCommand command =
                parser.parse(
                        "/compare XEQT VFV"
                );

        assertEquals(
                "compare",
                command.getCommand()
        );

        assertEquals(
                2,
                command.getArguments().size()
        );

        assertEquals(
                "XEQT",
                command.getArguments().get(0)
        );

        assertEquals(
                "VFV",
                command.getArguments().get(1)
        );
    }

    @Test
    void commandNameIsCaseInsensitive() {

        ParsedCommand command =
                parser.parse("/LoGoUt");

        assertEquals(
                "logout",
                command.getCommand()
        );
    }

    @Test
    void emptyCommandIsRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse("/")
        );
    }
}