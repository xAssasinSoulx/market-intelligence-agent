package com.marketintel.gui;

import com.marketintel.agent.ToolManager;
import com.marketintel.app.SessionWorkspace;
import com.marketintel.model.ActionType;
import com.marketintel.model.User;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.util.function.Consumer;

/**
 * Launch screen. Each tile opens the screen that owns that function.
 * Missing functions stay marked N/A and do not navigate.
 */
public final class DashboardPanel extends JPanel {

    private final SessionWorkspace workspace;
    private final ToolManager toolManager;
    private final boolean explanationsAvailable;
    private final Consumer<String> openSection;
    private final JLabel identity = UiTheme.fieldLabel("SIGN ON REQUIRED");
    private final JPanel board = new JPanel(new GridLayout(0, 3, 8, 8));
    private final JTextArea notes = UiTheme.resultArea();

    public DashboardPanel(
            SessionWorkspace workspace,
            ToolManager toolManager,
            boolean explanationsAvailable,
            Consumer<String> openSection) {

        this.workspace = workspace;
        this.toolManager = toolManager;
        this.explanationsAvailable = explanationsAvailable;
        this.openSection = openSection;

        setLayout(new BorderLayout(0, 8));
        setBackground(Color.BLACK);
        setBorder(UiTheme.padding(10));

        board.setBackground(Color.BLACK);
        notes.setRows(6);
        notes.setText("F1-F5 SWITCH SCREENS. COMMAND LINE: QUOTE NVDA, WHY NVDA, PORT, HELP.\n");

        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setBackground(Color.BLACK);
        body.add(identity, BorderLayout.NORTH);
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
            identity.setText("SIGN ON REQUIRED");
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
        tile("08", "GEO", "N/A", null, false);
        tile("09", "PORT", positions + " POS", "Portfolio", true);
        tile("10", "MON", symbols + " SYM", "Watchlist", true);
        tile("11", "EXPOSURE", state(available(ActionType.PORTFOLIO_EXPOSURE)), "Portfolio",
                available(ActionType.PORTFOLIO_EXPOSURE));
        tile("12", "FOLLOW-UP", "N/A", null, false);

        board.revalidate();
        board.repaint();
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
