package com.marketintel.gui;

import com.marketintel.app.ResearchGateway;
import com.marketintel.app.SessionWorkspace;
import com.marketintel.model.ActionType;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Quote, overview, comparison, sector and country exposure, overlap, and news.
 */
public final class MarketResearchPanel extends JPanel {

    private final SessionWorkspace workspace;
    private final ResearchGateway gateway;
    private final JLabel statusLabel;
    private final JTextField symbolField = UiTheme.textField(10);
    private final JTextField secondSymbolField = UiTheme.textField(10);
    private final JTextArea results = UiTheme.resultArea();
    private final List<JComponent> controls = new ArrayList<>();
    private int requestGeneration;

    public MarketResearchPanel(
            SessionWorkspace workspace,
            ResearchGateway gateway,
            JLabel statusLabel) {

        this.workspace = workspace;
        this.gateway = gateway;
        this.statusLabel = statusLabel;

        UiTheme.uppercase(symbolField);
        UiTheme.uppercase(secondSymbolField);

        setLayout(new BorderLayout(0, 8));
        setBackground(Color.BLACK);
        setBorder(UiTheme.padding(10));

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        form.setBackground(Color.BLACK);
        form.add(UiTheme.fieldLabel("SYMBOL"));
        form.add(symbolField);
        form.add(UiTheme.fieldLabel("SYMBOL 2"));
        form.add(secondSymbolField);

        addButton(form, "QUOTE", true, this::runQuote);
        addButton(form, "DES", false, this::runOverview);
        addButton(form, "CMP", false, this::runCompare);
        addButton(form, "SEC", false, this::runSector);
        addButton(form, "GEO", false, this::runGeography);
        addButton(form, "OVL", false, this::runOverlap);
        addButton(form, "NEWS", false, this::runNews);

        results.setText(
                "ENTER A SYMBOL, THEN PRESS A FUNCTION.\n"
                        + "CMP AND OVL USE BOTH SYMBOLS.\n"
                        + "GEO REPORTS COUNTRY WEIGHTS. IT DOES NOT GUESS A COUNTRY.\n"
                        + "OR TYPE QUOTE NVDA IN THE COMMAND LINE.\n"
        );
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.BLACK);
        header.add(new UiTheme.JPanelBar("DES", "MARKET RESEARCH"), BorderLayout.NORTH);
        header.add(form, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);
        add(UiTheme.scroll(results), BorderLayout.CENTER);

        controls.add(symbolField);
        controls.add(secondSymbolField);
    }

    public void clear() {
        requestGeneration++;
        symbolField.setText("");
        secondSymbolField.setText("");
        results.setText("");
    }

    public void loadQuote(String symbol) {
        symbolField.setText(symbol);
        runQuote();
    }

    public void loadOverview(String symbol) {
        symbolField.setText(symbol);
        runOverview();
    }

    public void loadComparison(String first, String second) {
        symbolField.setText(first);
        secondSymbolField.setText(second);
        runCompare();
    }

    public void loadSector(String symbol) {
        symbolField.setText(symbol);
        runSector();
    }

    public void loadGeography(String symbol) {
        symbolField.setText(symbol);
        runGeography();
    }

    public void loadOverlap(String first, String second) {
        symbolField.setText(first);
        secondSymbolField.setText(second);
        runOverlap();
    }

    public void loadNews(String symbol) {
        symbolField.setText(symbol);
        runNews();
    }

    private void addButton(
            JPanel form,
            String label,
            boolean primary,
            Runnable action) {

        JButton button = UiTheme.button(label, primary);
        button.addActionListener(event -> action.run());
        form.add(button);
        controls.add(button);
    }

    private void runQuote() {
        String symbol = symbolField.getText();
        runForCurrentUser("Loading quote...", userId -> ResultFormatter.quote(
                gateway.execute(
                        userId,
                        ActionType.MARKET_QUOTE,
                        Map.of("symbol", symbol)
                )
        ));
    }

    private void runOverview() {
        String symbol = symbolField.getText();
        runForCurrentUser("Loading overview...", userId -> ResultFormatter.overview(
                gateway.execute(
                        userId,
                        ActionType.SECURITY_OVERVIEW,
                        Map.of("symbol", symbol)
                )
        ));
    }

    private void runCompare() {
        String first = symbolField.getText();
        String second = secondSymbolField.getText();
        runForCurrentUser("Comparing securities...", userId -> ResultFormatter.comparison(
                gateway.execute(
                        userId,
                        ActionType.SECURITY_COMPARISON,
                        Map.of("firstSymbol", first, "secondSymbol", second)
                )
        ));
    }

    private void runSector() {
        String symbol = symbolField.getText();
        runForCurrentUser("Loading sector exposure...", userId -> ResultFormatter.sectorExposure(
                gateway.execute(
                        userId,
                        ActionType.ETF_SECTOR_EXPOSURE,
                        Map.of("symbol", symbol)
                )
        ));
    }

    private void runGeography() {
        String symbol = symbolField.getText();
        runForCurrentUser("Loading country exposure...", userId -> ResultFormatter.geographicExposure(
                gateway.execute(
                        userId,
                        ActionType.ETF_GEOGRAPHIC_EXPOSURE,
                        Map.of("symbol", symbol)
                )
        ));
    }

    private void runOverlap() {
        String first = symbolField.getText();
        String second = secondSymbolField.getText();
        runForCurrentUser("Calculating overlap...", userId -> ResultFormatter.overlap(
                gateway.execute(
                        userId,
                        ActionType.ETF_OVERLAP,
                        Map.of("firstSymbol", first, "secondSymbol", second)
                )
        ));
    }

    private void runNews() {
        String symbol = symbolField.getText();
        runForCurrentUser("Loading news...", userId -> ResultFormatter.news(
                gateway.execute(
                        userId,
                        ActionType.FINANCIAL_NEWS,
                        Map.of("symbol", symbol)
                )
        ));
    }

    private void runForCurrentUser(
            String busyMessage,
            java.util.function.LongFunction<String> task) {

        if (!workspace.isAuthenticated()) {
            show("Error: Sign in again before using this feature.\n");
            return;
        }

        long userId = gateway.currentUserId();
        int generation = ++requestGeneration;
        BackgroundWork.submit(
                statusLabel,
                controls,
                this::restoreControls,
                busyMessage,
                () -> task.apply(userId),
                text -> {
                    if (generation == requestGeneration) {
                        show(text);
                    }
                }
        );
    }

    private void restoreControls() {
        for (JComponent control : controls) {
            control.setEnabled(true);
        }
    }

    private void show(String text) {
        results.setText(text == null ? "" : text);
        results.setCaretPosition(0);
    }
}
