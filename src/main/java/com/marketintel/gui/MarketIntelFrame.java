package com.marketintel.gui;

import com.marketintel.app.ResearchGateway;
import com.marketintel.app.SessionWorkspace;
import com.marketintel.bootstrap.ApplicationContext;
import com.marketintel.model.ActionType;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Amber-on-black desk. Function keys and the command line call the same
 * services as the buttons on each screen.
 */
public final class MarketIntelFrame extends JFrame {

    private static final DateTimeFormatter CLOCK =
            DateTimeFormatter.ofPattern("ddMMMyy  HH:mm:ss", Locale.US);

    private final SessionWorkspace workspace;
    private final ResearchGateway gateway;
    private final boolean explanationsAvailable;
    private final java.awt.CardLayout rootCards = new java.awt.CardLayout();
    private final JPanel rootCardsPanel = new JPanel(rootCards);
    private final java.awt.CardLayout sectionCards = new java.awt.CardLayout();
    private final JPanel sectionPanel = new JPanel(sectionCards);
    private final JLabel statusLabel = UiTheme.text("SIGN ON TO CONTINUE", true);
    private final JLabel clockLabel = UiTheme.fieldLabel("");
    private final JLabel feedLabel = UiTheme.text("", true);
    private final JTextField commandField = UiTheme.textField(48);
    private final AuthenticationPanel authenticationPanel;
    private final DashboardPanel dashboardPanel;
    private final MarketResearchPanel researchPanel;
    private final PortfolioPanel portfolioPanel;
    private final WatchlistPanel watchlistPanel;
    private final AgentPanel agentPanel;
    private final Map<String, JButton> navigation = new LinkedHashMap<>();
    private String activeSection = "Dashboard";

    private MarketIntelFrame(ApplicationContext context) {
        super("Market Intelligence");

        workspace = new SessionWorkspace(
                context.getAuthService(),
                context.getSessionManager(),
                context.getPortfolioService(),
                context.getWatchlistService()
        );
        gateway = new ResearchGateway(
                context.getSessionManager(),
                context.getToolManager(),
                context.getMarketExplanationService()
        );
        explanationsAvailable = context.isExplanationAvailable();

        Consumer<String> openSection = this::showSection;
        authenticationPanel = new AuthenticationPanel(
                workspace,
                this::showApplication,
                statusLabel
        );
        dashboardPanel = new DashboardPanel(
                workspace,
                context.getToolManager(),
                explanationsAvailable,
                openSection
        );
        researchPanel = new MarketResearchPanel(workspace, gateway, statusLabel);
        portfolioPanel = new PortfolioPanel(workspace, gateway, statusLabel);
        watchlistPanel = new WatchlistPanel(workspace, statusLabel);
        agentPanel = new AgentPanel(workspace, gateway, statusLabel);

        sectionPanel.setBackground(Color.BLACK);
        sectionPanel.add(dashboardPanel, "Dashboard");
        sectionPanel.add(researchPanel, "Research");
        sectionPanel.add(portfolioPanel, "Portfolio");
        sectionPanel.add(watchlistPanel, "Watchlist");
        sectionPanel.add(agentPanel, "Explain");

        JPanel application = new JPanel(new BorderLayout(0, 0));
        application.setBackground(Color.BLACK);
        application.add(functionBar(), BorderLayout.NORTH);
        application.add(sectionPanel, BorderLayout.CENTER);
        application.add(commandBar(), BorderLayout.SOUTH);

        rootCardsPanel.setBackground(Color.BLACK);
        rootCardsPanel.add(authenticationPanel, "auth");
        rootCardsPanel.add(application, "app");

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.BLACK);
        root.add(masthead(), BorderLayout.NORTH);
        root.add(rootCardsPanel, BorderLayout.CENTER);
        root.add(footer(), BorderLayout.SOUTH);
        setContentPane(root);

        bindFunctionKeys();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setSize(1280, 820);
        setLocationRelativeTo(null);
        paintNavigation();
        rootCards.show(rootCardsPanel, "auth");

        Timer clock = new Timer(1000, event -> refreshClock());
        clock.setInitialDelay(0);
        clock.start();
    }

    public static void launch(ApplicationContext context) {
        System.setProperty("apple.awt.application.name", "Market Intelligence");
        SwingUtilities.invokeLater(() -> {
            UiTheme.install();
            MarketIntelFrame frame = new MarketIntelFrame(context);
            frame.setVisible(true);
        });
    }

    private JPanel masthead() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UiTheme.ACCENT);
        bar.setBorder(UiTheme.padding(6));

        JLabel title = new JLabel("  MI    MARKET INTELLIGENCE");
        title.setFont(UiTheme.UI_FONT_BOLD);
        title.setForeground(Color.BLACK);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        right.setOpaque(false);
        JLabel product = new JLabel("DESK");
        product.setFont(UiTheme.UI_FONT_BOLD);
        product.setForeground(Color.BLACK);
        clockLabel.setForeground(Color.BLACK);
        right.add(product);
        right.add(clockLabel);

        bar.add(title, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel functionBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.BLACK);
        bar.setBorder(javax.swing.BorderFactory.createMatteBorder(
                0, 0, 1, 0, UiTheme.ACCENT
        ));

        JPanel keys = new JPanel(new GridLayout(1, 5, 6, 0));
        keys.setBackground(Color.BLACK);
        keys.setBorder(UiTheme.padding(6));
        addKey(keys, "Dashboard", "1  HOME");
        addKey(keys, "Research", "2  DES");
        addKey(keys, "Portfolio", "3  PORT");
        addKey(keys, "Watchlist", "4  MON");
        addKey(keys, "Explain", "5  WHY");

        JButton logout = UiTheme.button("LOG OFF");
        logout.setForeground(UiTheme.DANGER);
        logout.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(UiTheme.DANGER),
                UiTheme.padding(5)
        ));
        logout.addActionListener(event -> logout());

        JPanel logoutWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        logoutWrap.setBackground(Color.BLACK);
        logoutWrap.add(logout);

        bar.add(keys, BorderLayout.CENTER);
        bar.add(logoutWrap, BorderLayout.EAST);
        return bar;
    }

    private void addKey(JPanel keys, String section, String label) {
        JButton button = new JButton(label);
        button.setFont(UiTheme.UI_FONT_BOLD);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        button.addActionListener(event -> showSection(section));
        navigation.put(section, button);
        keys.add(button);
    }

    private JPanel commandBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBackground(Color.BLACK);
        bar.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.ACCENT),
                UiTheme.padding(8)
        ));

        JLabel prompt = UiTheme.fieldLabel(">");
        UiTheme.uppercase(commandField);
        commandField.addActionListener(event -> runCommand());
        JButton go = UiTheme.button("<GO>", true);
        go.addActionListener(event -> runCommand());

        JLabel hint = UiTheme.text("QUOTE NVDA     WHY NVDA     PORT     HELP", true);

        JPanel fieldRow = new JPanel(new BorderLayout(8, 0));
        fieldRow.setBackground(Color.BLACK);
        fieldRow.add(prompt, BorderLayout.WEST);
        fieldRow.add(commandField, BorderLayout.CENTER);
        fieldRow.add(go, BorderLayout.EAST);

        bar.add(hint, BorderLayout.NORTH);
        bar.add(fieldRow, BorderLayout.CENTER);
        return bar;
    }

    private JPanel footer() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.BLACK);
        bar.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER),
                UiTheme.padding(6)
        ));
        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(feedLabel, BorderLayout.EAST);
        refreshFeeds();
        return bar;
    }

    private void bindFunctionKeys() {
        bind(KeyEvent.VK_F1, "Dashboard");
        bind(KeyEvent.VK_F2, "Research");
        bind(KeyEvent.VK_F3, "Portfolio");
        bind(KeyEvent.VK_F4, "Watchlist");
        bind(KeyEvent.VK_F5, "Explain");
    }

    private void bind(int keyCode, String section) {
        String name = "desk-" + section;
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(keyCode, 0), name);
        getRootPane().getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                if (workspace.isAuthenticated()) {
                    showSection(section);
                }
            }
        });
    }

    private void showApplication() {
        refreshFeeds();
        showSection("Dashboard");
        statusLabel.setText("SIGNED IN  " + workspace.currentUser().getUsername());
        rootCards.show(rootCardsPanel, "app");
        commandField.requestFocusInWindow();
    }

    private void showSection(String section) {
        activeSection = section;
        paintNavigation();
        switch (section) {
            case "Dashboard" -> dashboardPanel.refresh();
            case "Portfolio" -> portfolioPanel.refresh();
            case "Watchlist" -> watchlistPanel.refresh();
            default -> {
            }
        }
        sectionCards.show(sectionPanel, section);
    }

    private void paintNavigation() {
        for (Map.Entry<String, JButton> entry : navigation.entrySet()) {
            boolean active = entry.getKey().equals(activeSection);
            JButton button = entry.getValue();
            button.setBackground(active ? UiTheme.ACCENT : Color.BLACK);
            button.setForeground(active ? Color.BLACK : UiTheme.ACCENT);
            button.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createLineBorder(
                            active ? UiTheme.ACCENT : UiTheme.ACCENT_DARK
                    ),
                    UiTheme.padding(6)
            ));
        }
    }

    private void runCommand() {
        if (!workspace.isAuthenticated()) {
            statusLabel.setText("SIGN ON BEFORE USING THE COMMAND LINE");
            return;
        }

        String typed = commandField.getText();
        commandField.setText("");
        DeskCommand command = DeskCommand.parse(typed);

        switch (command.action()) {
            case EMPTY, INVALID, GEOGRAPHY ->
                    statusLabel.setText(command.message());
            case HELP -> {
                showSection("Dashboard");
                dashboardPanel.showHelp(command.message());
                statusLabel.setText("HELP");
            }
            case HOME -> showSection("Dashboard");
            case RESEARCH -> showSection("Research");
            case PORTFOLIO -> showSection("Portfolio");
            case EXPOSURE -> {
                showSection("Portfolio");
                portfolioPanel.loadExposure();
            }
            case WATCHLIST -> showSection("Watchlist");
            case EXPLAIN -> showSection("Explain");
            case LOGOUT -> logout();
            case QUOTE -> {
                showSection("Research");
                researchPanel.loadQuote(command.argument(0));
            }
            case OVERVIEW -> {
                showSection("Research");
                researchPanel.loadOverview(command.argument(0));
            }
            case COMPARE -> {
                showSection("Research");
                researchPanel.loadComparison(command.argument(0), command.argument(1));
            }
            case SECTOR -> {
                showSection("Research");
                researchPanel.loadSector(command.argument(0));
            }
            case OVERLAP -> {
                showSection("Research");
                researchPanel.loadOverlap(command.argument(0), command.argument(1));
            }
            case NEWS -> {
                showSection("Research");
                researchPanel.loadNews(command.argument(0));
            }
            case WHY -> {
                showSection("Explain");
                agentPanel.explainSymbol(command.argument(0));
            }
            case PORTFOLIO_ADD -> {
                showSection("Portfolio");
                portfolioPanel.addFromDesk(
                        command.argument(0),
                        command.argument(1),
                        command.argument(2)
                );
            }
            case PORTFOLIO_REMOVE -> {
                showSection("Portfolio");
                portfolioPanel.removeFromDesk(command.argument(0));
            }
            case WATCHLIST_ADD -> {
                showSection("Watchlist");
                watchlistPanel.addFromDesk(command.argument(0));
            }
            case WATCHLIST_REMOVE -> {
                showSection("Watchlist");
                watchlistPanel.removeFromDesk(command.argument(0));
            }
        }
    }

    private void logout() {
        workspace.logout();
        authenticationPanel.clear();
        researchPanel.clear();
        portfolioPanel.clear();
        watchlistPanel.clear();
        agentPanel.clear();
        dashboardPanel.refresh();
        commandField.setText("");
        statusLabel.setText("SIGNED OUT");
        rootCards.show(rootCardsPanel, "auth");
    }

    private void refreshClock() {
        clockLabel.setText(LocalDateTime.now().format(CLOCK).toUpperCase(Locale.US) + "  ");
    }

    private void refreshFeeds() {
        feedLabel.setText(
                flag("QUOTES", gateway.isAvailable(ActionType.MARKET_QUOTE))
                        + "   "
                        + flag("ETF", gateway.isAvailable(ActionType.ETF_SECTOR_EXPOSURE))
                        + "   "
                        + flag("NEWS", gateway.isAvailable(ActionType.FINANCIAL_NEWS))
                        + "   "
                        + flag("AI", explanationsAvailable)
                        + "  "
        );
    }

    private static String flag(String name, boolean live) {
        return name + (live ? " LIVE" : " OFF");
    }
}
