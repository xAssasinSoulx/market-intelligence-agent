package com.marketintel.gui;

import com.marketintel.app.ResearchGateway;
import com.marketintel.app.SessionWorkspace;
import com.marketintel.model.ActionType;
import com.marketintel.model.AssetType;
import com.marketintel.model.PortfolioPosition;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Per-user portfolio editing and market-value exposure.
 * Exposure uses price times quantity through PortfolioAnalysisTool.
 */
public final class PortfolioPanel extends JPanel {

    private final SessionWorkspace workspace;
    private final ResearchGateway gateway;
    private final JLabel statusLabel;
    private final JTextField symbolField = UiTheme.textField(10);
    private final JTextField quantityField = UiTheme.textField(8);
    private final JComboBox<AssetType> assetTypeField =
            new JComboBox<>(AssetType.values());
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Symbol", "Quantity", "Asset type"},
            0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JLabel emptyLabel = UiTheme.text(
            "NO POSITIONS. ADD A SYMBOL, A QUANTITY ABOVE ZERO, AND A TYPE.",
            true
    );
    private final JTextArea exposureOutput = UiTheme.resultArea();
    private final JButton exposureButton = UiTheme.button("EXPOS");
    private int requestGeneration;

    public PortfolioPanel(
            SessionWorkspace workspace,
            ResearchGateway gateway,
            JLabel statusLabel) {

        this.workspace = workspace;
        this.gateway = gateway;
        this.statusLabel = statusLabel;
        UiTheme.uppercase(symbolField);

        setLayout(new BorderLayout(0, 8));
        setBackground(Color.BLACK);
        setBorder(UiTheme.padding(10));

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        form.setBackground(Color.BLACK);
        form.add(UiTheme.fieldLabel("SYMBOL"));
        form.add(symbolField);
        form.add(UiTheme.fieldLabel("QTY"));
        form.add(quantityField);
        form.add(UiTheme.fieldLabel("TYPE"));
        UiTheme.styleCombo(assetTypeField);
        form.add(assetTypeField);

        JButton add = UiTheme.button("ADD", true);
        JButton remove = UiTheme.button("DEL");
        JButton refresh = UiTheme.button("RFRSH");
        exposureButton.addActionListener(event -> loadExposure());

        add.addActionListener(event -> addPosition());
        remove.addActionListener(event -> removePosition());
        refresh.addActionListener(event -> refresh());
        form.add(add);
        form.add(remove);
        form.add(refresh);
        form.add(exposureButton);

        UiTheme.styleTable(table);
        table.getSelectionModel().addListSelectionListener(event -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                symbolField.setText(String.valueOf(tableModel.getValueAt(row, 0)));
            }
        });

        JScrollPane tableScroll = UiTheme.scroll(table);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setBackground(Color.BLACK);
        center.add(emptyLabel, BorderLayout.NORTH);
        center.add(tableScroll, BorderLayout.CENTER);
        exposureOutput.setRows(8);
        exposureOutput.setText(
                "EXPOS uses quantity times the latest price.\n"
                        + "Share count is not treated as market value.\n"
        );
        center.add(UiTheme.scroll(exposureOutput), BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setBackground(Color.BLACK);
        body.add(form, BorderLayout.NORTH);
        body.add(center, BorderLayout.CENTER);

        add(new UiTheme.JPanelBar("PORT", "PORTFOLIO HOLDINGS"), BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    public void refresh() {
        tableModel.setRowCount(0);
        if (!workspace.isAuthenticated()) {
            emptyLabel.setText("SIGN ON TO VIEW A PORTFOLIO");
            return;
        }

        for (PortfolioPosition position : workspace.portfolio().getPositions()) {
            tableModel.addRow(new Object[]{
                    position.getSymbol(),
                    position.getQuantity().stripTrailingZeros().toPlainString(),
                    position.getAssetType().name()
            });
        }

        emptyLabel.setText(tableModel.getRowCount() == 0
                ? "NO POSITIONS. ADD A SYMBOL, A QUANTITY ABOVE ZERO, AND A TYPE."
                : " ");
    }

    public void clear() {
        symbolField.setText("");
        quantityField.setText("");
        assetTypeField.setSelectedItem(AssetType.STOCK);
        tableModel.setRowCount(0);
        exposureOutput.setText("");
    }

    public void loadExposure() {
        if (!workspace.isAuthenticated()) {
            statusLabel.setText("SIGN ON BEFORE CALCULATING EXPOSURE");
            return;
        }
        long userId = gateway.currentUserId();
        int generation = ++requestGeneration;
        List<JComponent> controls = List.of(exposureButton);
        BackgroundWork.submit(
                statusLabel,
                controls,
                () -> exposureButton.setEnabled(true),
                "Pricing positions...",
                () -> ResultFormatter.portfolioExposure(gateway.execute(
                        userId,
                        ActionType.PORTFOLIO_EXPOSURE,
                        Map.of()
                )),
                text -> {
                    if (generation == requestGeneration) {
                        exposureOutput.setText(text == null ? "" : text);
                        exposureOutput.setCaretPosition(0);
                    }
                }
        );
    }

    public void addFromDesk(String symbol, String quantity, String type) {
        symbolField.setText(symbol);
        quantityField.setText(quantity);
        try {
            assetTypeField.setSelectedItem(AssetType.valueOf(type));
        } catch (IllegalArgumentException exception) {
            statusLabel.setText("Asset type must be STOCK, ETF, or OTHER.");
            return;
        }
        addPosition();
    }

    public void removeFromDesk(String symbol) {
        symbolField.setText(symbol);
        removePosition();
    }

    private void addPosition() {
        if (!workspace.isAuthenticated()) {
            statusLabel.setText("Sign in again before editing a portfolio.");
            return;
        }

        try {
            BigDecimal quantity = new BigDecimal(quantityField.getText().trim());
            workspace.addPosition(
                    symbolField.getText(),
                    quantity,
                    (AssetType) assetTypeField.getSelectedItem()
            );
            refresh();
            statusLabel.setText("POSITION SAVED");
        } catch (NumberFormatException exception) {
            statusLabel.setText("QUANTITY MUST BE A NUMBER GREATER THAN ZERO");
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        } catch (RuntimeException exception) {
            statusLabel.setText("UNABLE TO SAVE THE POSITION");
        }
    }

    private void removePosition() {
        if (!workspace.isAuthenticated()) {
            statusLabel.setText("Sign in again before editing a portfolio.");
            return;
        }

        try {
            boolean removed = workspace.removePosition(symbolField.getText());
            refresh();
            statusLabel.setText(removed
                    ? "POSITION REMOVED"
                    : "THAT SYMBOL IS NOT IN THE PORTFOLIO");
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        } catch (RuntimeException exception) {
            statusLabel.setText("UNABLE TO REMOVE THE POSITION");
        }
    }
}
