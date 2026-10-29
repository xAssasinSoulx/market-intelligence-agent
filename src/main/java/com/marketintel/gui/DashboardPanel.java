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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
    private final JLabel valueAmount = UiTheme.heading("—");
    private final JLabel dayAmount = UiTheme.heading("—");
    private final JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
    private final JLabel bookNote = UiTheme.text("", true);
    private final JPanel board = new JPanel(new GridLayout(0, 4, 8, 8));
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
        notes.setRows(2);
        notes.setText("F1-F5 SWITCH SCREENS.  QUOTE NVDA, WHY NVDA, PORT, HELP.\n");

        valueAmount.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 28));
        dayAmount.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 22));
        chips.setBackground(Color.BLACK);

        JPanel figures = new JPanel(new GridLayout(1, 2, 16, 0));
        figures.setBackground(Color.BLACK);
        figures.add(figure("MARKET VALUE", valueAmount));
        figures.add(figure("DAY", dayAmount));

        JPanel bookPanel = new JPanel(new BorderLayout(0, 4));
        bookPanel.setBackground(Color.BLACK);
        bookPanel.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDER),
                UiTheme.padding(4)
        ));
        bookPanel.add(figures, BorderLayout.NORTH);
        bookPanel.add(chips, BorderLayout.CENTER);
        bookPanel.add(bookNote, BorderLayout.SOUTH);

        JPanel heading = new JPanel(new BorderLayout(0, 6));
        heading.setBackground(Color.BLACK);
        heading.add(identity, BorderLayout.NORTH);
        heading.add(bookPanel, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setBackground(Color.BLACK);
        body.add(heading, BorderLayout.NORTH);
        body.add(board, BorderLayout.CENTER);

        javax.swing.JScrollPane notesScroll = UiTheme.scroll(notes);
        notesScroll.setPreferredSize(new Dimension(100, 52));

        add(new UiTheme.JPanelBar("HOME", "SESSION MONITOR"), BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        add(notesScroll, BorderLayout.SOUTH);
    }

    public void refresh() {
        board.removeAll();
        if (!workspace.isAuthenticated()) {
            bookGeneration++;
            identity.setText("SIGN ON REQUIRED");
            showBookMessage("SIGN ON TO SEE THE PORTFOLIO.");
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
        tile("12", "FOLLOW-UP", "ASK", "Explain", true);

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
            showBookMessage("NO OPEN POSITIONS.  PORT ADD NVDA 10 STOCK.");
            return;
        }
        showBookMessage("PRICING " + positions.size() + " POSITIONS...");
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
                    showBook(get());
                } catch (Exception exception) {
                    showBookMessage("PORTFOLIO PRICES COULD NOT BE LOADED.");
                }
            }
        }.execute();
    }

    private void showBookMessage(String message) {
        valueAmount.setText("—");
        valueAmount.setForeground(UiTheme.MUTED);
        dayAmount.setText("—");
        dayAmount.setForeground(UiTheme.MUTED);
        chips.removeAll();
        bookNote.setText(message);
        chips.revalidate();
        chips.repaint();
    }

    private void showBook(PortfolioBook snapshot) {
        valueAmount.setText(money(snapshot.marketValue()));
        valueAmount.setForeground(UiTheme.TEXT);
        dayAmount.setText(day(snapshot));
        dayAmount.setForeground(tone(snapshot.dayChange()));
        chips.removeAll();
        for (PortfolioBook.Row row : snapshot.priced()) {
            chips.add(chip(row));
        }
        for (String symbol : snapshot.unpriced()) {
            chips.add(plainChip(symbol + "  UNPRICED"));
        }
        bookNote.setText(snapshot.priced().isEmpty()
                ? "NO POSITION COULD BE PRICED."
                : "Quantity times the latest price. Day is the move from the previous close.");
        chips.revalidate();
        chips.repaint();
    }

    private JPanel figure(String caption, JLabel amount) {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.setBackground(Color.BLACK);
        panel.add(UiTheme.fieldLabel(caption));
        panel.add(amount);
        return panel;
    }

    private JPanel chip(PortfolioBook.Row row) {
        JPanel chip = plainChip(
                row.symbol()
                        + "   "
                        + plain(row.quantity())
                        + "   "
                        + signed(row.percentChange())
                        + "%   "
                        + money(row.marketValue())
        );
        chip.setForeground(tone(row.percentChange()));
        for (java.awt.Component child : chip.getComponents()) {
            if (child instanceof JLabel label) {
                label.setForeground(tone(row.percentChange()));
            }
        }
        return chip;
    }

    private JPanel plainChip(String text) {
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        chip.setBackground(new Color(0x120E04));
        chip.setBorder(javax.swing.BorderFactory.createLineBorder(UiTheme.BORDER));
        JLabel label = UiTheme.text(text);
        chip.add(label);
        return chip;
    }

    private static Color tone(BigDecimal value) {
        return switch (value.signum()) {
            case 1 -> UiTheme.SUCCESS;
            case -1 -> UiTheme.DANGER;
            default -> UiTheme.TEXT;
        };
    }

    private static String day(PortfolioBook snapshot) {
        String amount = signedMoney(snapshot.dayChange());
        BigDecimal percent = snapshot.dayPercent();
        if (percent == null) {
            return amount;
        }
        return amount + "   (" + signed(percent) + "%)";
    }

    private static String signedMoney(BigDecimal value) {
        String text = money(value.abs());
        if (value.signum() < 0) {
            return "-" + text;
        }
        if (value.signum() > 0) {
            return "+" + text;
        }
        return text;
    }

    private static String money(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        format.setGroupingUsed(true);
        return format.format(value);
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String signed(BigDecimal value) {
        String text = value.setScale(2, RoundingMode.HALF_UP).toPlainString();
        if (value.signum() > 0 && !text.startsWith("+")) {
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
        cell.setPreferredSize(new Dimension(180, 84));
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
