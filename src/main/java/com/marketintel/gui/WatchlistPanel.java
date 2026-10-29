package com.marketintel.gui;

import com.marketintel.app.SessionWorkspace;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;

/**
 * Per-user watchlist editing through WatchlistService.
 */
public final class WatchlistPanel extends JPanel {

    private final SessionWorkspace workspace;
    private final JLabel statusLabel;
    private final JTextField symbolField = UiTheme.textField(12);
    private final DefaultListModel<String> symbols = new DefaultListModel<>();
    private final JList<String> list = new JList<>(symbols);
    private final JLabel emptyLabel = UiTheme.text("MONITOR IS EMPTY", true);

    public WatchlistPanel(SessionWorkspace workspace, JLabel statusLabel) {
        this.workspace = workspace;
        this.statusLabel = statusLabel;
        UiTheme.uppercase(symbolField);

        setLayout(new BorderLayout(0, 8));
        setBackground(Color.BLACK);
        setBorder(UiTheme.padding(10));

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        form.setBackground(Color.BLACK);
        form.add(UiTheme.fieldLabel("SYMBOL"));
        form.add(symbolField);

        JButton add = UiTheme.button("ADD", true);
        JButton remove = UiTheme.button("DEL");
        JButton refresh = UiTheme.button("RFRSH");
        add.addActionListener(event -> addSymbol());
        remove.addActionListener(event -> removeSymbol());
        refresh.addActionListener(event -> refresh());
        symbolField.addActionListener(event -> addSymbol());
        form.add(add);
        form.add(remove);
        form.add(refresh);

        UiTheme.styleList(list);
        list.addListSelectionListener(event -> {
            String selected = list.getSelectedValue();
            if (selected != null) {
                symbolField.setText(selected);
            }
        });

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setBackground(Color.BLACK);
        center.add(emptyLabel, BorderLayout.NORTH);
        center.add(UiTheme.scroll(list), BorderLayout.CENTER);

        add(new UiTheme.JPanelBar("MON", "WATCHLIST MONITOR"), BorderLayout.NORTH);
        add(form, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);
    }

    public void refresh() {
        symbols.clear();
        if (!workspace.isAuthenticated()) {
            emptyLabel.setText("SIGN ON TO VIEW A MONITOR");
            return;
        }

        workspace.watchlist().getSymbols().forEach(symbols::addElement);
        emptyLabel.setText(symbols.isEmpty()
                ? "MONITOR IS EMPTY"
                : " ");
    }

    public void clear() {
        symbolField.setText("");
        symbols.clear();
    }

    public void addFromDesk(String symbol) {
        symbolField.setText(symbol);
        addSymbol();
    }

    public void removeFromDesk(String symbol) {
        symbolField.setText(symbol);
        removeSymbol();
    }

    private void addSymbol() {
        if (!workspace.isAuthenticated()) {
            statusLabel.setText("Sign in again before editing a watchlist.");
            return;
        }

        try {
            workspace.addToWatchlist(symbolField.getText());
            refresh();
            statusLabel.setText("SYMBOL ADDED TO THE MONITOR");
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        } catch (RuntimeException exception) {
            statusLabel.setText("Unable to update the watchlist.");
        }
    }

    private void removeSymbol() {
        if (!workspace.isAuthenticated()) {
            statusLabel.setText("Sign in again before editing a watchlist.");
            return;
        }

        try {
            boolean removed = workspace.removeFromWatchlist(symbolField.getText());
            refresh();
            statusLabel.setText(removed
                    ? "SYMBOL REMOVED FROM THE MONITOR"
                    : "THAT SYMBOL IS NOT ON THE MONITOR");
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        } catch (RuntimeException exception) {
            statusLabel.setText("Unable to update the watchlist.");
        }
    }
}
