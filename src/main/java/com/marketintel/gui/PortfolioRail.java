package com.marketintel.gui;

import com.marketintel.app.ResearchGateway;
import com.marketintel.app.SessionWorkspace;
import com.marketintel.model.ActionType;
import com.marketintel.model.MarketQuote;
import com.marketintel.model.PortfolioPosition;
import com.marketintel.model.ToolResult;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holdings stay on screen while the center of the desk changes.
 * Prices fill in when a quote returns. A missing price stays named.
 */
public final class PortfolioRail extends JPanel {

    private final SessionWorkspace workspace;
    private final ResearchGateway gateway;
    private final JTextArea body = UiTheme.resultArea();
    private int generation;

    public PortfolioRail(SessionWorkspace workspace, ResearchGateway gateway) {
        this.workspace = workspace;
        this.gateway = gateway;
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(210, 0));
        setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, 0, 0, 1, UiTheme.ACCENT_DARK),
                UiTheme.padding(8)
        ));
        JLabel title = UiTheme.fieldLabel("PORTFOLIO");
        body.setEditable(false);
        body.setBorder(null);
        add(title, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        showPositions(List.of(), Map.of());
    }

    public void refresh() {
        if (!workspace.isAuthenticated()) {
            showPositions(List.of(), Map.of());
            return;
        }
        List<PortfolioPosition> positions = List.copyOf(workspace.portfolio().getPositions());
        showPositions(positions, Map.of());
        if (positions.isEmpty()) {
            return;
        }
        long userId = gateway.currentUserId();
        int ticket = ++generation;
        new SwingWorker<Map<String, String>, Void>() {
            @Override
            protected Map<String, String> doInBackground() {
                Map<String, String> prices = new LinkedHashMap<>();
                for (PortfolioPosition position : positions) {
                    ToolResult result = gateway.read(
                            userId,
                            ActionType.MARKET_QUOTE,
                            Map.of("symbol", position.getSymbol())
                    );
                    if (result.isSuccess() && result.getData() instanceof MarketQuote quote) {
                        prices.put(position.getSymbol(), quote.getPrice().toPlainString());
                    }
                }
                return prices;
            }

            @Override
            protected void done() {
                if (ticket != generation || !workspace.isAuthenticated()) {
                    return;
                }
                try {
                    showPositions(positions, get());
                } catch (Exception ignored) {
                    showPositions(positions, Map.of());
                }
            }
        }.execute();
    }

    private void showPositions(List<PortfolioPosition> positions, Map<String, String> prices) {
        if (positions.isEmpty()) {
            body.setText("No open positions.\n\nmain\nreturns home");
            return;
        }
        StringBuilder text = new StringBuilder();
        for (PortfolioPosition position : positions) {
            text.append(position.getSymbol());
            text.append("  ");
            text.append(position.getQuantity().stripTrailingZeros().toPlainString());
            text.append("  ");
            text.append(position.getAssetType());
            String price = prices.get(position.getSymbol());
            if (price != null) {
                text.append("\n$").append(price);
            }
            text.append("\n\n");
        }
        text.append("main\nreturns home");
        body.setText(text.toString());
        body.setCaretPosition(0);
    }
}
