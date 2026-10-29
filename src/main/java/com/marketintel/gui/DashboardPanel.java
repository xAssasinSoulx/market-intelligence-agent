package com.marketintel.gui;

import com.marketintel.agent.ToolManager;
import com.marketintel.app.ResearchGateway;
import com.marketintel.app.SessionWorkspace;
import com.marketintel.model.ActionType;
import com.marketintel.model.MarketQuote;
import com.marketintel.model.PortfolioPosition;
import com.marketintel.model.ToolResult;
import com.marketintel.model.User;
import com.marketintel.services.PortfolioBook;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Launch screen. Each tile opens the screen that owns that function.
 * Missing functions stay marked N/A and do not navigate.
 */
public final class DashboardPanel extends JPanel {

    private final SessionWorkspace workspace;
    private final ResearchGateway gateway;
    private final ToolManager toolManager;
    private final boolean explanationsAvailable;
    private final Consumer<String> openSection;
    private final JLabel identity = UiTheme.fieldLabel("SIGN ON REQUIRED");
    private final JTextArea book = UiTheme.resultArea();
    private final JPanel board = new JPanel(new GridLayout(0, 3, 8, 8));
    private final JTextArea notes = UiTheme.resultArea();
    private int bookGeneration;

    public DashboardPanel(
            SessionWorkspace workspace,
            ResearchGateway gateway,
            ToolManager toolManager,
            boolean explanationsAvailable,
            Consumer<String> openSection) {

        this.workspace = workspace;
        this.gateway = gateway;
        this.toolManager = toolManager;
        this.explanationsAvailable = explanationsAvailable;
        this.openSection = openSection;

        setLayout(new BorderLayout(0, 8));
        setBackground(Color.BLACK);
        setBorder(UiTheme.padding(10));

        board.setBackground(Color.BLACK);
        book.setRows(7);
        book.setText("PORTFOLIO STATUS APPEARS AFTER SIGN-ON.\n");
        notes.setRows(4);
        notes.setText("F1-F5 SWITCH SCREENS. COMMAND LINE: QUOTE NVDA, WHY NVDA, PORT, HELP.\n");

        javax.swing.JScrollPane bookScroll = UiTheme.scroll(book);
        bookScroll.setPreferredSize(new java.awt.Dimension(100, 148));

        JPanel heading = new JPanel(new BorderLayout(0, 6));
        heading.setBackground(Color.BLACK);
        heading.add(identity, BorderLayout.NORTH);
        heading.add(bookScroll, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setBackground(Color.BLACK);
        body.add(heading, BorderLayout.NORTH);
        body.add(board, BorderLayout.CENTER);

        javax.swing.JScrollPane notesScroll = UiTheme.scroll(notes);
        notesScroll.setPreferredSize(new java.awt.Dimension(100, 150));

        add(new UiTheme.JPanelBar("HOME", "SESSION MONITOR"), BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        add(notesScroll, BorderLayout.SOUTH);
    }

    public void refresh() {
        board.removeAll();
        if (!workspace.isAuthenticated()) {
            bookGeneration++;
            identity.setText("SIGN ON REQUIRED");
            book.setText("");
            notes.setText("");
            board.revalidate();
            board.repaint();
            return;
        }

        User user = workspace.currentUser();
        int positions = workspace.portfolio().getPositions().size();
        int symbols = workspace.watchlist().getSymbols().size();
        identity.setText(
                "USER  " + user.getUsername()
                        + "     POSITIONS  " + positions
                        + "     MONITOR  " + symbols
        );

        tile("01", "QUOTE", state(available(ActionType.MARKET_QUOTE)), "Research", true);
        tile("02", "DES", state(available(ActionType.SECURITY_OVERVIEW)), "Research", true);
        tile("03", "COMPARE", state(available(ActionType.SECURITY_COMPARISON)), "Research", true);
        tile("04", "SECTOR", state(available(ActionType.ETF_SECTOR_EXPOSURE)), "Research", true);
        tile("05", "OVERLAP", state(available(ActionType.ETF_OVERLAP)), "Research", true);
        tile("06", "NEWS", state(available(ActionType.FINANCIAL_NEWS)), "Research", true);
        tile("07", "WHY", explanationsAvailable ? "AI LIVE" : "EVIDENCE", "Explain", true);
        boolean geography = available(ActionType.ETF_GEOGRAPHIC_EXPOSURE);
        tile("08", "GEO", state(geography), "Research", geography);
        tile("09", "PORT", positions + " POS", "Portfolio", true);
        tile("10", "MON", symbols + " SYM", "Watchlist", true);
        tile("11", "EXPOSURE", state(available(ActionType.PORTFOLIO_EXPOSURE)), "Portfolio",
                available(ActionType.PORTFOLIO_EXPOSURE));
        tile("12", "FOLLOW-UP", "N/A", null, false);

        board.revalidate();
        board.repaint();
        loadBook();
    }

    private void loadBook() {
        if (!workspace.isAuthenticated()) {
            return;
        }
        long userId = gateway.currentUserId();
        List<PortfolioPosition> positions = List.copyOf(workspace.portfolio().getPositions());
        int generation = ++bookGeneration;
        if (positions.isEmpty()) {
            book.setText("NO OPEN POSITIONS.\nADD ONE WITH PORT ADD NVDA 10 STOCK.\n");
            book.setCaretPosition(0);
            return;
        }
        book.setText("PRICING " + positions.size() + " POSITIONS...\n");
        new SwingWorker<PortfolioBook, Void>() {
            @Override
            protected PortfolioBook doInBackground() {
                Map<String, MarketQuote> quotes = new LinkedHashMap<>();
                for (PortfolioPosition position : positions) {
                    ToolResult result = gateway.execute(
                            userId,
                            ActionType.MARKET_QUOTE,
                            Map.of("symbol", position.getSymbol())
                    );
                    if (result.isSuccess() && result.getData() instanceof MarketQuote quote) {
                        quotes.put(position.getSymbol(), quote);
                    }
                }
                return PortfolioBook.value(positions, quotes);
            }

            @Override
            protected void done() {
                if (generation != bookGeneration || !workspace.isAuthenticated()) {
                    return;
                }
                try {
                    book.setText(formatBook(get()));
                    book.setCaretPosition(0);
                } catch (Exception exception) {
                    book.setText("PORTFOLIO PRICES COULD NOT BE LOADED.\n");
                }
            }
        }.execute();
    }

    private static String formatBook(PortfolioBook snapshot) {
        StringBuilder text = new StringBuilder();
        text.append("PORTFOLIO\n");
        text.append(String.format(
                "MARKET VALUE  %s     DAY  %s%n",
                money(snapshot.marketValue()),
                day(snapshot)
        ));
        text.append("\n");
        if (snapshot.priced().isEmpty()) {
            text.append("NO POSITION COULD BE PRICED.\n");
        }
        for (PortfolioBook.Row row : snapshot.priced()) {
            text.append(String.format(
                    "%-8s %8s  @ %10s  %8s  %12s%n",
                    row.symbol(),
                    plain(row.quantity()),
                    money(row.price()),
                    signed(row.percentChange()) + "%",
                    money(row.marketValue())
            ));
        }
        if (!snapshot.unpriced().isEmpty()) {
            text.append("\nUNPRICED  ");
            text.append(String.join("  ", snapshot.unpriced()));
            text.append("\n");
        }
        text.append("\nValue is quantity times the latest price. Day is the move from the previous close.\n");
        return text.toString();
    }

    private static String day(PortfolioBook snapshot) {
        String amount = signed(snapshot.dayChange());
        BigDecimal percent = snapshot.dayPercent();
        if (percent == null) {
            return amount;
        }
        return amount + "  (" + signed(percent) + "%)";
    }

    private static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String signed(BigDecimal value) {
        String text = value.setScale(2, RoundingMode.HALF_UP).toPlainString();
        if (value.signum() > 0) {
            return "+" + text;
        }
        return text;
    }

    public void showHelp(String help) {
        notes.setText(help == null ? "" : help);
        notes.setCaretPosition(0);
    }

    private void tile(
            String code,
            String name,
            String state,
            String section,
            boolean enabled) {

        JPanel cell = new JPanel(new GridLayout(3, 1));
        cell.setBackground(enabled ? new Color(0x120E04) : Color.BLACK);
        cell.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(enabled ? UiTheme.ACCENT : UiTheme.BORDER),
                UiTheme.padding(8)
        ));

        JLabel codeLabel = UiTheme.fieldLabel(code);
        JLabel nameLabel = UiTheme.heading(name);
        JLabel stateLabel = UiTheme.text(state, !enabled && "N/A".equals(state));
        if (state.equals("LIVE") || state.endsWith("POS") || state.endsWith("SYM")) {
            stateLabel.setForeground(UiTheme.SUCCESS);
        } else if ("N/A".equals(state) || state.endsWith("OFF")) {
            stateLabel.setForeground(UiTheme.MUTED);
        } else {
            stateLabel.setForeground(UiTheme.ACCENT);
        }

        cell.add(codeLabel);
        cell.add(nameLabel);
        cell.add(stateLabel);

        if (enabled && section != null) {
            cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            cell.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent event) {
                    openSection.accept(section);
                }
            });
        }
        board.add(cell);
    }

    private boolean available(ActionType action) {
        return toolManager.findTool(action).isPresent();
    }

    private static String state(boolean ready) {
        return ready ? "LIVE" : "OFF";
    }
}
