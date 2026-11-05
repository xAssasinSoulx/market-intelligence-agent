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

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Home screen. The book owns the center: value, day move, allocation,
 * and one row per position. The twelve tiles stay on one row and open
 * the screen that owns that function.
 */
public final class DashboardPanel extends JPanel {

    private final SessionWorkspace workspace;
    private final ResearchGateway gateway;
    private final ToolManager toolManager;
    private final boolean explanationsAvailable;
    private final Consumer<String> openSection;
    private final HoldingsRefresh refreshState = new HoldingsRefresh();
    private final JLabel identity = UiTheme.fieldLabel("SIGN ON REQUIRED");
    private final JLabel valueAmount = UiTheme.heading("—");
    private final JLabel dayAmount = UiTheme.heading("—");
    private final JLabel coverage = UiTheme.fieldLabel("");
    private final JTextArea bookNote = noteArea();
    private final AllocationBar allocationBar = new AllocationBar();
    private final JPanel rowStack = new JPanel(new GridLayout(0, 1, 0, 0));
    private final JScrollPane holdingsScroll;
    private final JPanel board = new JPanel(new GridLayout(1, 12, 4, 0));
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
        rowStack.setBackground(Color.BLACK);
        notes.setRows(2);
        notes.setText("F1-F5 SWITCH SCREENS.  QUOTE NVDA, WHY NVDA, PORT, HELP.\n");

        valueAmount.setFont(new Font(Font.MONOSPACED, Font.BOLD, 28));
        dayAmount.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
        valueAmount.setForeground(UiTheme.MUTED);
        dayAmount.setForeground(UiTheme.MUTED);
        coverage.setVisible(false);

        JPanel figures = new JPanel(new GridBagLayout());
        figures.setBackground(Color.BLACK);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.gridx = 0;
        constraints.gridy = 0;
        figures.add(figure("MARKET VALUE", valueAmount, coverage), constraints);
        constraints.gridx = 1;
        constraints.insets = new Insets(0, 28, 0, 0);
        figures.add(figure("DAY", dayAmount, null), constraints);
        constraints.gridx = 2;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 0, 0);
        JPanel glue = new JPanel();
        glue.setOpaque(false);
        figures.add(glue, constraints);

        JPanel summary = new JPanel(new BorderLayout(0, 2));
        summary.setBackground(Color.BLACK);
        summary.add(figures, BorderLayout.NORTH);
        summary.add(bookNote, BorderLayout.SOUTH);

        JPanel summaryBlock = new JPanel(new BorderLayout(0, 6));
        summaryBlock.setBackground(Color.BLACK);
        summaryBlock.add(identity, BorderLayout.NORTH);
        summaryBlock.add(summary, BorderLayout.CENTER);
        summaryBlock.add(allocationBar, BorderLayout.SOUTH);

        JPanel scroller = new JPanel(new BorderLayout());
        scroller.setBackground(Color.BLACK);
        scroller.add(rowStack, BorderLayout.NORTH);
        holdingsScroll = UiTheme.scroll(scroller);
        holdingsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        holdingsScroll.getVerticalScrollBar().setUnitIncrement(24);
        holdingsScroll.setMinimumSize(new Dimension(0, 72));

        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setBackground(Color.BLACK);
        body.add(summaryBlock, BorderLayout.NORTH);
        body.add(holdingsScroll, BorderLayout.CENTER);
        body.add(board, BorderLayout.SOUTH);

        JScrollPane notesScroll = UiTheme.scroll(notes);
        notesScroll.setPreferredSize(new Dimension(100, 52));

        add(new UiTheme.JPanelBar("HOME", "SESSION MONITOR"), BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        add(notesScroll, BorderLayout.SOUTH);
        allocationBar.setVisible(false);
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

    /**
     * Prices the book again on the tape cadence.
     * A book already on screen stays up until the new quotes return.
     */
    public void reloadHoldings() {
        if (!workspace.isAuthenticated()) {
            return;
        }
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
        String pricing = HomeDeskView.pricingNote(positions.size());
        if (refreshState.retainWhileLoading()) {
            bookNote.setText(pricing);
        } else {
            showBookMessage(pricing);
        }
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
                    if (refreshState.retainWhileLoading()) {
                        bookNote.setText("PORTFOLIO PRICES COULD NOT BE LOADED.");
                    } else {
                        showBookMessage("PORTFOLIO PRICES COULD NOT BE LOADED.");
                    }
                }
            }
        }.execute();
    }

    private void showBookMessage(String message) {
        refreshState.clear();
        valueAmount.setText("—");
        valueAmount.setForeground(UiTheme.MUTED);
        dayAmount.setText("—");
        dayAmount.setForeground(UiTheme.MUTED);
        coverage.setText("");
        coverage.setVisible(false);
        bookNote.setText(message);
        allocationBar.setSegments(List.of());
        allocationBar.setVisible(false);
        showRows(List.of());
    }

    private void showBook(PortfolioBook snapshot) {
        HomeDeskView.Model model = HomeDeskView.present(snapshot);
        refreshState.showedBook();
        valueAmount.setText(model.marketValue());
        valueAmount.setForeground(model.totalsBlank() ? UiTheme.MUTED : UiTheme.TEXT);
        dayAmount.setText(model.day());
        dayAmount.setForeground(model.totalsBlank() ? UiTheme.MUTED : toneColor(model.dayTone()));
        coverage.setText(model.coverage() == null ? "" : model.coverage());
        coverage.setVisible(model.coverage() != null);
        bookNote.setText(model.note());
        allocationBar.setSegments(model.segments());
        allocationBar.setVisible(!model.rows().isEmpty());
        showRows(model.rows());
    }

    private void showRows(List<HomeDeskView.Row> rows) {
        rowStack.removeAll();
        if (!rows.isEmpty()) {
            rowStack.add(columnHeader());
            for (HomeDeskView.Row row : rows) {
                rowStack.add(holdingRow(row));
            }
        }
        holdingsScroll.setVisible(!rows.isEmpty());
        rowStack.revalidate();
        rowStack.repaint();
    }

    private JPanel figure(String caption, JLabel amount, JLabel footer) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.BLACK);
        JLabel captionLabel = UiTheme.fieldLabel(caption);
        captionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        amount.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(captionLabel);
        panel.add(amount);
        if (footer != null) {
            footer.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(footer);
        }
        return panel;
    }

    private JPanel columnHeader() {
        JPanel line = new JPanel(new GridLayout(1, 5, 12, 0));
        line.setBackground(Color.BLACK);
        line.setBorder(UiTheme.padding(4));
        for (String name : List.of("SYMBOL", "WEIGHT", "VALUE", "DAY", "QUOTE")) {
            line.add(UiTheme.fieldLabel(name));
        }
        return line;
    }

    private JPanel holdingRow(HomeDeskView.Row row) {
        JPanel line = new JPanel(new GridLayout(1, 5, 12, 0));
        line.setBackground(Color.BLACK);
        line.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDER),
                UiTheme.padding(4)
        ));
        Color symbolColor = row.unpriced() ? UiTheme.MUTED : UiTheme.TEXT;
        Color plain = row.unpriced() ? UiTheme.MUTED : UiTheme.TEXT;
        line.add(colored(row.symbol(), symbolColor));
        line.add(colored(row.weight(), plain));
        line.add(colored(row.value(), plain));
        line.add(colored(row.dayDollars(), row.unpriced() ? plain : toneColor(row.dayTone())));
        line.add(colored(row.quotePercent(), row.unpriced() ? plain : toneColor(row.quoteTone())));
        return line;
    }

    private static JTextArea noteArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setRows(2);
        area.setFont(UiTheme.UI_FONT);
        area.setBackground(Color.BLACK);
        area.setForeground(UiTheme.MUTED);
        area.setBorder(null);
        return area;
    }

    private static JLabel colored(String text, Color color) {
        JLabel label = UiTheme.text(text);
        label.setForeground(color);
        return label;
    }

    private static Color toneColor(HomeDeskView.Tone tone) {
        return switch (tone) {
            case UP -> UiTheme.SUCCESS;
            case DOWN -> UiTheme.DANGER;
            case FLAT -> UiTheme.TEXT;
        };
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
        cell.setPreferredSize(new Dimension(64, 46));
        cell.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(enabled ? UiTheme.ACCENT : UiTheme.BORDER),
                UiTheme.padding(3)
        ));

        JLabel codeLabel = UiTheme.fieldLabel(code);
        JLabel nameLabel = UiTheme.text(name);
        JLabel stateLabel = UiTheme.text(state, !enabled && "N/A".equals(state));
        Font codeFont = new Font(Font.MONOSPACED, Font.PLAIN, 10);
        Font nameFont = new Font(Font.MONOSPACED, Font.BOLD, 11);
        codeLabel.setFont(codeFont);
        nameLabel.setFont(nameFont);
        stateLabel.setFont(codeFont);
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
