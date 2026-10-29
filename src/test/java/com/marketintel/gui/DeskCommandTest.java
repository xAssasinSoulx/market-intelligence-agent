package com.marketintel.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeskCommandTest {

    @Test
    void acceptsTerminalSlashForm() {
        DeskCommand command = DeskCommand.parse("/quote nvda");

        assertEquals(DeskCommand.Action.QUOTE, command.action());
        assertEquals("NVDA", command.argument(0));
    }

    @Test
    void opensDescriptionWhenDesHasASymbol() {
        DeskCommand command = DeskCommand.parse("des aapl");

        assertEquals(DeskCommand.Action.OVERVIEW, command.action());
        assertEquals("AAPL", command.argument(0));
    }

    @Test
    void opensResearchWhenDesHasNoSymbol() {
        assertEquals(DeskCommand.Action.RESEARCH, DeskCommand.parse("DES").action());
    }

    @Test
    void requiresBothSymbolsForAComparison() {
        DeskCommand command = DeskCommand.parse("COMPARE NVDA");

        assertEquals(DeskCommand.Action.INVALID, command.action());
        assertTrue(command.message().contains("COMPARE"));
    }

    @Test
    void parsesPortfolioAdd() {
        DeskCommand command = DeskCommand.parse("port add nvda 10 stock");

        assertEquals(DeskCommand.Action.PORTFOLIO_ADD, command.action());
        assertEquals("NVDA", command.argument(0));
        assertEquals("10", command.argument(1));
        assertEquals("STOCK", command.argument(2));
    }

    @Test
    void reportsUnimplementedCommandsInsteadOfGuessing() {
        DeskCommand geography = DeskCommand.parse("GEO QQQ");
        DeskCommand exposure = DeskCommand.parse("EXPOSURE");

        assertEquals(DeskCommand.Action.GEOGRAPHY, geography.action());
        assertTrue(geography.message().contains("not implemented"));
        assertEquals(DeskCommand.Action.EXPOSURE, exposure.action());
        assertEquals(null, exposure.message());
    }

    @Test
    void helpListsTheLiveCommands() {
        DeskCommand command = DeskCommand.parse("help");

        assertEquals(DeskCommand.Action.HELP, command.action());
        assertTrue(command.message().contains("QUOTE SYMBOL"));
        assertTrue(command.message().contains("WHY SYMBOL"));
    }
}
