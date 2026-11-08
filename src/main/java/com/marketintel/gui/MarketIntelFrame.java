package com.marketintel.gui;

import com.marketintel.app.ResearchGateway;
import com.marketintel.app.SessionWorkspace;
import com.marketintel.bootstrap.ApplicationContext;
import com.marketintel.model.ActionType;
import com.marketintel.model.MarketQuote;
import com.marketintel.model.NewsArticle;
import com.marketintel.model.NewsResult;
import com.marketintel.model.ToolResult;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.text.DefaultEditorKit;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final JTextArea composer = composerField();
    private final JButton sendButton = UiTheme.button("Send", true);
    private final AuthenticationPanel authenticationPanel;
    private final DashboardPanel dashboardPanel;
    private final MarketResearchPanel researchPanel;
    private final PortfolioPanel portfolioPanel;
    private final WatchlistPanel watchlistPanel;
    private final AgentPanel agentPanel;
    private final MarketTape marketTape = new MarketTape();
    private final InflightQuotes inflightQuotes = new InflightQuotes();
    private final Map<String, JButton> navigation = new LinkedHashMap<>();
    private String activeSection = "Dashboard";
    private int tapeGeneration;
    private Timer tapeRefresh;

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
                context.getMarketExplanationService(),
                context.getFollowUp()
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
                gateway,
                inflightQuotes,
                context.getToolManager(),
                explanationsAvailable,
                openSection
        );
        researchPanel = new MarketResearchPanel(workspace, gateway, statusLabel);
        portfolioPanel = new PortfolioPanel(workspace, gateway, statusLabel);
        watchlistPanel = new WatchlistPanel(workspace, statusLabel);
        agentPanel = new AgentPanel(workspace, gateway, statusLabel, composer, sendButton);

        sectionPanel.setBackground(Color.BLACK);
        sectionPanel.add(dashboardPanel, "Dashboard");
        sectionPanel.add(researchPanel, "Research");
        sectionPanel.add(portfolioPanel, "Portfolio");
        sectionPanel.add(watchlistPanel, "Watchlist");
        sectionPanel.add(agentPanel, "Explain");

        JPanel desk = new JPanel(new BorderLayout());
        desk.setBackground(Color.BLACK);
        desk.add(marketTape, BorderLayout.NORTH);
        desk.add(sectionPanel, BorderLayout.CENTER);

        JPanel application = new JPanel(new BorderLayout(0, 0));
        application.setBackground(Color.BLACK);
        application.add(functionBar(), BorderLayout.NORTH);
        application.add(desk, BorderLayout.CENTER);
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
        addKey(keys, "Explain", "5  CHAT");

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

        composer.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send-message");
        composer.getActionMap().put("send-message", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                runCommand();
            }
        });
        composer.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK),
                DefaultEditorKit.insertBreakAction
        );
        sendButton.addActionListener(event -> runCommand());

        JLabel hint = UiTheme.text("Message the desk. Enter sends. Shift+Enter adds a line.", true);

        JScrollPane composerScroll = new JScrollPane(composer);
        composerScroll.setBorder(javax.swing.BorderFactory.createLineBorder(UiTheme.ACCENT_DARK));
        composerScroll.getViewport().setBackground(Color.BLACK);
        composerScroll.setPreferredSize(new Dimension(480, 58));

        JPanel fieldRow = new JPanel(new BorderLayout(8, 0));
        fieldRow.setBackground(Color.BLACK);
        fieldRow.add(composerScroll, BorderLayout.CENTER);
        fieldRow.add(sendButton, BorderLayout.EAST);

        bar.add(hint, BorderLayout.NORTH);
        bar.add(fieldRow, BorderLayout.CENTER);
        return bar;
    }

    private static JTextArea composerField() {
        JTextArea area = new JTextArea(2, 48);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(UiTheme.UI_FONT.deriveFont(15f));
        area.setBackground(Color.BLACK);
        area.setForeground(UiTheme.ACCENT);
        area.setCaretColor(UiTheme.ACCENT);
        area.setSelectionColor(UiTheme.SELECTED);
        area.setSelectedTextColor(UiTheme.TEXT);
        area.setMargin(new java.awt.Insets(8, 10, 8, 10));
        return area;
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
        composer.requestFocusInWindow();
        marketTape.start();
        loadTape();
        if (tapeRefresh == null) {
            tapeRefresh = new Timer(90_000, event -> refreshMarketStrip());
        }
        tapeRefresh.start();
    }

    /**
     * The tape and, while Home is showing, the holdings book.
     * Both stay on this timer. A faster poll is not added.
     */
    private void refreshMarketStrip() {
        loadTape();
        if ("Dashboard".equals(activeSection)) {
            dashboardPanel.reloadHoldings();
        }
    }

    private void loadTape() {
        if (!workspace.isAuthenticated()) {
            return;
        }
        long userId = gateway.currentUserId();
        List<String> symbols = new ArrayList<>(List.of("SPY", "QQQ", "DIA", "AAPL", "MSFT"));
        for (var position : workspace.portfolio().getPositions()) {
            String symbol = position.getSymbol().toUpperCase(Locale.ROOT);
            if (!symbols.contains(symbol) && symbols.size() < 10) {
                symbols.add(symbol);
            }
        }
        int generation = ++tapeGeneration;
        marketTape.setLine("LOADING MARKET TAPE");
        new SwingWorker<List<TapeLine.Piece>, Void>() {
            @Override
            protected List<TapeLine.Piece> doInBackground() {
                List<MarketQuote> quotes = new ArrayList<>();
                for (String symbol : symbols) {
                    ToolResult result = inflightQuotes.load(
                            InflightQuotes.quoteKey(userId, symbol),
                            () -> gateway.read(
                                    userId,
                                    ActionType.MARKET_QUOTE,
                                    Map.of("symbol", symbol)
                            )
                    );
                    if (result.isSuccess() && result.getData() instanceof MarketQuote quote) {
                        quotes.add(quote);
                    }
                }
                List<NewsArticle> articles = List.of();
                ToolResult news = gateway.read(
                        userId,
                        ActionType.FINANCIAL_NEWS,
                        Map.of("symbol", "SPY")
                );
                if (news.isSuccess() && news.getData() instanceof NewsResult result) {
                    articles = result.getArticles();
                }
                return TapeLine.pieces(quotes, articles);
            }

            @Override
            protected void done() {
                if (generation != tapeGeneration || !workspace.isAuthenticated()) {
                    return;
                }
                try {
                    marketTape.setPieces(get());
                } catch (Exception exception) {
                    marketTape.setLine("MARKET TAPE  ·  THE FEED COULD NOT BE LOADED");
                }
            }
        }.execute();
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

        String typed = composer.getText().trim();
        composer.setText("");
        if (typed.isEmpty()) {
            return;
        }
        DeskCommand command = DeskCommand.parse(typed);

        switch (command.action()) {
            case EMPTY -> {
            }
            case HELP -> {
                showSection("Explain");
                agentPanel.ask("what can you do");
            }
            case HOME -> showSection("Dashboard");
            case RESEARCH -> showSection("Research");
            case PORTFOLIO -> showSection("Portfolio");
            case EXPOSURE -> {
                showSection("Explain");
                agentPanel.ask(typed);
            }
            case WATCHLIST -> showSection("Watchlist");
            case EXPLAIN -> showSection("Explain");
            case LOGOUT -> logout();
            case QUOTE, OVERVIEW, COMPARE, SECTOR, GEOGRAPHY, OVERLAP, NEWS,
                    STATEMENTS, HISTORY, SENTIMENT, WHY, ASK, INVALID -> {
                showSection("Explain");
                agentPanel.ask(typed);
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
        tapeGeneration++;
        if (tapeRefresh != null) {
            tapeRefresh.stop();
        }
        marketTape.stop();
        workspace.logout();
        authenticationPanel.clear();
        researchPanel.clear();
        portfolioPanel.clear();
        watchlistPanel.clear();
        agentPanel.clear();
        dashboardPanel.refresh();
        composer.setText("");
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
                        + flag("STM", gateway.isAvailable(ActionType.FINANCIAL_STATEMENTS))
                        + "   "
                        + flag("HIST", gateway.isAvailable(ActionType.PRICE_HISTORY))
                        + "   "
                        + flag("SENT", gateway.isAvailable(ActionType.NEWS_SENTIMENT))
                        + "   "
                        + flag("AI", explanationsAvailable)
                        + "  "
        );
    }

    private static String flag(String name, boolean live) {
        return name + (live ? " LIVE" : " OFF");
    }
}
