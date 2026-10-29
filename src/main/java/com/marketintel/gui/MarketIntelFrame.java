package com.marketintel.gui;

import com.marketintel.app.ResearchGateway;
import com.marketintel.app.SessionWorkspace;
import com.marketintel.bootstrap.ApplicationContext;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Main window. Authentication stays in the shared session manager, and
 * every panel reads that same session instead of keeping its own.
 */
public final class MarketIntelFrame extends JFrame {

    private final SessionWorkspace workspace;
    private final CardLayout rootCards = new CardLayout();
    private final JPanel rootCardsPanel = new JPanel(rootCards);
    private final CardLayout sectionCards = new CardLayout();
    private final JPanel sectionPanel = new JPanel(sectionCards);
    private final JLabel statusLabel = UiTheme.text("Sign in to continue.", true);
    private final AuthenticationPanel authenticationPanel;
    private final DashboardPanel dashboardPanel;
    private final MarketResearchPanel researchPanel;
    private final PortfolioPanel portfolioPanel;
    private final WatchlistPanel watchlistPanel;
    private final AgentPanel agentPanel;
    private final Map<String, JToggleButton> navigation = new LinkedHashMap<>();

    private MarketIntelFrame(ApplicationContext context) {
        super("Market Intelligence Agent");

        workspace = new SessionWorkspace(
                context.getAuthService(),
                context.getSessionManager(),
                context.getPortfolioService(),
                context.getWatchlistService()
        );

        ResearchGateway gateway = new ResearchGateway(
                context.getSessionManager(),
                context.getToolManager(),
                context.getMarketExplanationService()
        );

        authenticationPanel = new AuthenticationPanel(
                workspace,
                this::showApplication,
                statusLabel
        );
        dashboardPanel = new DashboardPanel(
                workspace,
                context.getToolManager(),
                context.isExplanationAvailable()
        );
        researchPanel = new MarketResearchPanel(workspace, gateway, statusLabel);
        portfolioPanel = new PortfolioPanel(workspace, statusLabel);
        watchlistPanel = new WatchlistPanel(workspace, statusLabel);
        agentPanel = new AgentPanel(workspace, gateway, statusLabel);

        sectionPanel.setBackground(UiTheme.BACKGROUND);
        sectionPanel.add(dashboardPanel, "Dashboard");
        sectionPanel.add(researchPanel, "Research");
        sectionPanel.add(portfolioPanel, "Portfolio");
        sectionPanel.add(watchlistPanel, "Watchlist");
        sectionPanel.add(agentPanel, "Explain");

        JPanel application = new JPanel(new BorderLayout());
        application.setBackground(UiTheme.BACKGROUND);
        application.add(sidebar(), BorderLayout.WEST);
        application.add(sectionPanel, BorderLayout.CENTER);

        rootCardsPanel.setBackground(UiTheme.BACKGROUND);
        rootCardsPanel.add(authenticationPanel, "auth");
        rootCardsPanel.add(application, "app");

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UiTheme.BACKGROUND);
        root.add(rootCardsPanel, BorderLayout.CENTER);
        root.add(statusBar(), BorderLayout.SOUTH);
        setContentPane(root);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 660));
        setSize(1180, 760);
        setLocationRelativeTo(null);
        showSection("Dashboard");
        rootCards.show(rootCardsPanel, "auth");
    }

    public static void launch(ApplicationContext context) {
        System.setProperty("apple.awt.application.name", "Market Intelligence");
        SwingUtilities.invokeLater(() -> {
            UiTheme.install();
            new MarketIntelFrame(context).setVisible(true);
        });
    }

    private JPanel sidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 16));
        sidebar.setBackground(UiTheme.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setBorder(UiTheme.padding(16));

        JPanel links = new JPanel(new GridLayout(0, 1, 0, 8));
        links.setBackground(UiTheme.SIDEBAR);
        ButtonGroup group = new ButtonGroup();
        for (String section : new String[]{
                "Dashboard", "Research", "Portfolio", "Watchlist", "Explain"
        }) {
            JToggleButton button = new JToggleButton(section);
            button.setFont(UiTheme.UI_FONT_BOLD);
            button.setFocusPainted(false);
            button.setBackground(UiTheme.SIDEBAR);
            button.setForeground(UiTheme.TEXT);
            button.addActionListener(event -> showSection(section));
            group.add(button);
            navigation.put(section, button);
            links.add(button);
        }

        JButton logout = UiTheme.button("Log out");
        logout.addActionListener(event -> logout());

        JPanel header = new JPanel(new GridLayout(0, 1));
        header.setBackground(UiTheme.SIDEBAR);
        header.add(UiTheme.text("MARKET INTEL", true));

        sidebar.add(header, BorderLayout.NORTH);
        sidebar.add(links, BorderLayout.CENTER);
        sidebar.add(logout, BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel statusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UiTheme.SIDEBAR);
        bar.setBorder(UiTheme.padding(8));
        bar.add(statusLabel, BorderLayout.WEST);
        return bar;
    }

    private void showApplication() {
        showSection("Dashboard");
        statusLabel.setText("Signed in as " + workspace.currentUser().getUsername());
        rootCards.show(rootCardsPanel, "app");
    }

    private void showSection(String section) {
        JToggleButton selected = navigation.get(section);
        if (selected != null) {
            selected.setSelected(true);
        }
        for (Map.Entry<String, JToggleButton> entry : navigation.entrySet()) {
            boolean active = entry.getKey().equals(section);
            entry.getValue().setBackground(active ? UiTheme.BUTTON : UiTheme.SIDEBAR);
        }

        switch (section) {
            case "Dashboard" -> dashboardPanel.refresh();
            case "Portfolio" -> portfolioPanel.refresh();
            case "Watchlist" -> watchlistPanel.refresh();
            default -> {
            }
        }
        sectionCards.show(sectionPanel, section);
    }

    private void logout() {
        workspace.logout();
        authenticationPanel.clear();
        researchPanel.clear();
        portfolioPanel.clear();
        watchlistPanel.clear();
        agentPanel.clear();
        dashboardPanel.refresh();
        statusLabel.setText("Signed out.");
        rootCards.show(rootCardsPanel, "auth");
    }
}
