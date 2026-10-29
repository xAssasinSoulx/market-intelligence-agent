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
    void parsesGeographyAndLeavesPortfolioExposureAsNavigation() {
        DeskCommand geography = DeskCommand.parse("GEO qqq");
        DeskCommand missing = DeskCommand.parse("GEO");
        DeskCommand exposure = DeskCommand.parse("EXPOSURE");

        assertEquals(DeskCommand.Action.GEOGRAPHY, geography.action());
        assertEquals("QQQ", geography.argument(0));
        assertEquals(null, geography.message());
        assertEquals(DeskCommand.Action.INVALID, missing.action());
        assertTrue(missing.message().contains("GEO ETF"));
        assertEquals(DeskCommand.Action.EXPOSURE, exposure.action());
        assertEquals(null, exposure.message());
    }

    @Test
    void helpListsTheLiveCommands() {
        DeskCommand command = DeskCommand.parse("help");

        assertEquals(DeskCommand.Action.HELP, command.action());
        assertTrue(command.message().contains("QUOTE SYMBOL"));
        assertTrue(command.message().contains("GEO ETF"));
        assertTrue(command.message().contains("STATEMENTS SYMBOL"));
        assertTrue(command.message().contains("WHY SYMBOL"));
    }

    @Test
    void treatsAWhySentenceAsAQuestion() {
        DeskCommand command = DeskCommand.parse("why did microsoft fall");

        assertEquals(DeskCommand.Action.ASK, command.action());
        assertEquals("WHY", command.argument(0));
        assertEquals("MICROSOFT", command.argument(2));
    }

    @Test
    void stillAcceptsWhyWithOnlyASymbol() {
        DeskCommand command = DeskCommand.parse("WHY MSFT");

        assertEquals(DeskCommand.Action.WHY, command.action());
        assertEquals("MSFT", command.argument(0));
    }

    @Test
    void treatsASentenceAsAFollowUpQuestion() {
        DeskCommand command = DeskCommand.parse("how is nvidia doing");

        assertEquals(DeskCommand.Action.ASK, command.action());
        assertEquals("HOW", command.argument(0));
        assertEquals("NVIDIA", command.argument(2));
    }

    @Test
    void parsesTheProviderReportCommands() {
        assertEquals(
                DeskCommand.Action.STATEMENTS,
                DeskCommand.parse("/statements nvda").action()
        );
        assertEquals("QQQ", DeskCommand.parse("HIST qqq").argument(0));
        assertEquals(DeskCommand.Action.SENTIMENT, DeskCommand.parse("SENT AAPL").action());
    }
}
