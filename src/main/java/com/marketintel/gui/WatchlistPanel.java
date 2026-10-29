package com.marketintel.gui;

import com.marketintel.app.SessionWorkspace;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
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
    private final JLabel emptyLabel = UiTheme.text("The watchlist is empty.", true);

    public WatchlistPanel(SessionWorkspace workspace, JLabel statusLabel) {
        this.workspace = workspace;
        this.statusLabel = statusLabel;

        setLayout(new BorderLayout(0, 12));
        setBackground(UiTheme.BACKGROUND);
        setBorder(UiTheme.padding(20));

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        form.setBackground(UiTheme.BACKGROUND);
        form.add(UiTheme.text("Symbol"));
        form.add(symbolField);

        JButton add = UiTheme.button("Add", true);
        JButton remove = UiTheme.button("Remove");
        JButton refresh = UiTheme.button("Refresh");
        add.addActionListener(event -> addSymbol());
        remove.addActionListener(event -> removeSymbol());
        refresh.addActionListener(event -> refresh());
        symbolField.addActionListener(event -> addSymbol());
        form.add(add);
        form.add(remove);
        form.add(refresh);

        list.setBackground(UiTheme.FIELD);
        list.setForeground(UiTheme.TEXT);
        list.setFont(UiTheme.MONO);
        list.setSelectionBackground(UiTheme.BUTTON);
        list.setSelectionForeground(UiTheme.TEXT);
        list.addListSelectionListener(event -> {
            String selected = list.getSelectedValue();
            if (selected != null) {
                symbolField.setText(selected);
            }
        });

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setBackground(UiTheme.BACKGROUND);
        center.add(emptyLabel, BorderLayout.NORTH);
        center.add(UiTheme.scroll(list), BorderLayout.CENTER);

        add(UiTheme.heading("Watchlist"), BorderLayout.NORTH);
        add(form, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);
    }

    public void refresh() {
        symbols.clear();
        if (!workspace.isAuthenticated()) {
            emptyLabel.setText("Sign in to view a watchlist.");
            return;
        }

        workspace.watchlist().getSymbols().forEach(symbols::addElement);
        emptyLabel.setText(symbols.isEmpty()
                ? "The watchlist is empty."
                : " ");
    }

    public void clear() {
        symbolField.setText("");
        symbols.clear();
    }

    private void addSymbol() {
        if (!workspace.isAuthenticated()) {
            statusLabel.setText("Sign in again before editing a watchlist.");
            return;
        }

        try {
            workspace.addToWatchlist(symbolField.getText());
            refresh();
            statusLabel.setText("Symbol added to the watchlist.");
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
                    ? "Symbol removed from the watchlist."
                    : "That symbol is not on the watchlist.");
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        } catch (RuntimeException exception) {
            statusLabel.setText("Unable to update the watchlist.");
        }
    }
}
