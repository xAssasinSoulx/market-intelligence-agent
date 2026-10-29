package com.marketintel.gui;

import com.marketintel.app.ResearchGateway;
import com.marketintel.app.SessionWorkspace;
import com.marketintel.model.ActionType;
import com.marketintel.model.AssetType;
import com.marketintel.model.PortfolioPosition;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.math.BigDecimal;

/**
 * Per-user portfolio editing through PortfolioService. Exposure analysis
 * is shown as unavailable rather than as a placeholder chart.
 */
public final class PortfolioPanel extends JPanel {

    private final SessionWorkspace workspace;
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
            "No positions yet. Add a symbol, a quantity greater than zero, and an asset type.",
            true
    );

    public PortfolioPanel(SessionWorkspace workspace, JLabel statusLabel) {
        this.workspace = workspace;
        this.statusLabel = statusLabel;

        setLayout(new BorderLayout(0, 12));
        setBackground(UiTheme.BACKGROUND);
        setBorder(UiTheme.padding(20));

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        form.setBackground(UiTheme.BACKGROUND);
        form.add(UiTheme.text("Symbol"));
        form.add(symbolField);
        form.add(UiTheme.text("Quantity"));
        form.add(quantityField);
        form.add(UiTheme.text("Type"));
        styleCombo();
        form.add(assetTypeField);

        JButton add = UiTheme.button("Add position", true);
        JButton remove = UiTheme.button("Remove");
        JButton refresh = UiTheme.button("Refresh");
        JButton exposure = UiTheme.button("Analyze exposure");
        exposure.setEnabled(false);
        exposure.setToolTipText(ResearchGateway.unavailableMessage(
                ActionType.PORTFOLIO_EXPOSURE
        ));

        add.addActionListener(event -> addPosition());
        remove.addActionListener(event -> removePosition());
        refresh.addActionListener(event -> refresh());
        form.add(add);
        form.add(remove);
        form.add(refresh);
        form.add(exposure);

        table.setBackground(UiTheme.FIELD);
        table.setForeground(UiTheme.TEXT);
        table.setGridColor(UiTheme.BORDER);
        table.setSelectionBackground(UiTheme.BUTTON);
        table.setSelectionForeground(UiTheme.TEXT);
        table.setFont(UiTheme.MONO);
        table.setRowHeight(24);
        table.getTableHeader().setBackground(UiTheme.CARD);
        table.getTableHeader().setForeground(UiTheme.TEXT);
        table.getSelectionModel().addListSelectionListener(event -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                symbolField.setText(String.valueOf(tableModel.getValueAt(row, 0)));
            }
        });

        JScrollPane tableScroll = UiTheme.scroll(table);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setBackground(UiTheme.BACKGROUND);
        center.add(emptyLabel, BorderLayout.NORTH);
        center.add(tableScroll, BorderLayout.CENTER);
        center.add(UiTheme.text(
                "Exposure analysis is not implemented. Quantity is a share count, not market value.",
                true
        ), BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBackground(UiTheme.BACKGROUND);
        body.add(form, BorderLayout.NORTH);
        body.add(center, BorderLayout.CENTER);

        add(UiTheme.heading("Portfolio"), BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    public void refresh() {
        tableModel.setRowCount(0);
        if (!workspace.isAuthenticated()) {
            emptyLabel.setText("Sign in to view a portfolio.");
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
                ? "No positions yet. Add a symbol, a quantity greater than zero, and an asset type."
                : " ");
    }

    public void clear() {
        symbolField.setText("");
        quantityField.setText("");
        assetTypeField.setSelectedItem(AssetType.STOCK);
        tableModel.setRowCount(0);
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
            statusLabel.setText("Position saved.");
        } catch (NumberFormatException exception) {
            statusLabel.setText("Quantity must be a number greater than zero.");
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        } catch (RuntimeException exception) {
            statusLabel.setText("Unable to save the position.");
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
                    ? "Position removed."
                    : "That symbol is not in the portfolio.");
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        } catch (RuntimeException exception) {
            statusLabel.setText("Unable to remove the position.");
        }
    }

    private void styleCombo() {
        assetTypeField.setBackground(UiTheme.FIELD);
        assetTypeField.setForeground(UiTheme.TEXT);
        assetTypeField.setFont(UiTheme.UI_FONT);
    }
}
