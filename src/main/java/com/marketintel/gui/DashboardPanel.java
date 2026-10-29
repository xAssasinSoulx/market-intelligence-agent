package com.marketintel.gui;

import com.marketintel.agent.ToolManager;
import com.marketintel.app.SessionWorkspace;
import com.marketintel.model.ActionType;
import com.marketintel.model.User;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;

/**
 * Signed-in overview. Counts come from the same portfolio and watchlist
 * services the terminal uses. Missing providers are named, not hidden.
 */
public final class DashboardPanel extends JPanel {

    private final SessionWorkspace workspace;
    private final ToolManager toolManager;
    private final boolean explanationsAvailable;

    private final JLabel greeting = UiTheme.heading("Dashboard");
    private final JLabel counts = UiTheme.text("", true);
    private final JLabel capabilities = UiTheme.text("");

    public DashboardPanel(
            SessionWorkspace workspace,
            ToolManager toolManager,
            boolean explanationsAvailable) {

        this.workspace = workspace;
        this.toolManager = toolManager;
        this.explanationsAvailable = explanationsAvailable;

        setLayout(new BorderLayout());
        setBackground(UiTheme.BACKGROUND);
        setBorder(UiTheme.padding(24));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UiTheme.BACKGROUND);
        content.setOpaque(true);

        greeting.setAlignmentX(Component.LEFT_ALIGNMENT);
        counts.setAlignmentX(Component.LEFT_ALIGNMENT);
        capabilities.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(greeting);
        content.add(Box.createVerticalStrut(8));
        content.add(counts);
        content.add(Box.createVerticalStrut(18));
        content.add(UiTheme.text("What this session can reach", true));
        content.add(Box.createVerticalStrut(8));
        content.add(capabilities);
        content.add(Box.createVerticalStrut(18));
        content.add(UiTheme.text(
                "Use the sections on the left for research, positions, "
                        + "the watchlist, and movement explanations.",
                true
        ));

        add(content, BorderLayout.NORTH);
    }

    public void refresh() {
        if (!workspace.isAuthenticated()) {
            greeting.setText("Dashboard");
            counts.setText("");
            capabilities.setText("");
            return;
        }

        User user = workspace.currentUser();
        int positions = workspace.portfolio().getPositions().size();
        int symbols = workspace.watchlist().getSymbols().size();

        greeting.setText("Hello, " + user.getUsername());
        counts.setText(positions + " portfolio position"
                + (positions == 1 ? "" : "s")
                + "  ·  "
                + symbols + " watchlist symbol"
                + (symbols == 1 ? "" : "s"));

        capabilities.setText("<html>"
                + line("Market quotes and overviews", available(ActionType.MARKET_QUOTE))
                + line("Security comparison", available(ActionType.SECURITY_COMPARISON))
                + line("ETF sector exposure", available(ActionType.ETF_SECTOR_EXPOSURE))
                + line("ETF holding overlap", available(ActionType.ETF_OVERLAP))
                + line("Financial news", available(ActionType.FINANCIAL_NEWS))
                + line("Movement evidence", available(ActionType.MARKET_MOVEMENT))
                + line("Gemini explanation", explanationsAvailable)
                + line("ETF geographic exposure", false)
                + line("Portfolio exposure", false)
                + line("Contextual follow-up", false)
                + "</html>");
    }

    private boolean available(ActionType action) {
        return toolManager.findTool(action).isPresent();
    }

    private static String line(String name, boolean ready) {
        String state = ready ? "available" : "unavailable";
        return name + ": " + state + "<br>";
    }
}
