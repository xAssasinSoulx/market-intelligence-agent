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
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Quote, overview, comparison, sector exposure, overlap, and news.
 * Geographic exposure stays disabled until a real calculator is connected.
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

        setLayout(new BorderLayout(0, 12));
        setBackground(UiTheme.BACKGROUND);
        setBorder(UiTheme.padding(20));

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        form.setBackground(UiTheme.BACKGROUND);
        form.add(UiTheme.text("Symbol"));
        form.add(symbolField);
        form.add(UiTheme.text("Second symbol"));
        form.add(secondSymbolField);

        addButton(form, "Quote", true, () -> runQuote());
        addButton(form, "Overview", false, this::runOverview);
        addButton(form, "Compare", false, this::runCompare);
        addButton(form, "Sector", false, this::runSector);
        addButton(form, "Overlap", false, this::runOverlap);
        addButton(form, "News", false, this::runNews);

        JButton geography = UiTheme.button("Geography");
        geography.setEnabled(false);
        geography.setToolTipText(ResearchGateway.unavailableMessage(
                ActionType.ETF_GEOGRAPHIC_EXPOSURE
        ));
        form.add(geography);

        add(form, BorderLayout.NORTH);
        results.setText(
                "Enter a symbol, then choose an action.\n"
                        + "Compare and overlap use both symbols.\n"
                        + "Geography is unavailable until ETF country exposure is implemented.\n"
        );
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
