package com.marketintel.gui;

import com.marketintel.app.ResearchGateway;
import com.marketintel.app.SessionWorkspace;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.List;

/**
 * Direct movement explanation and contextual follow-up.
 * A follow-up such as "what about its sector" uses the last symbol
 * stored for this signed-in user.
 */
public final class AgentPanel extends JPanel {

    private final SessionWorkspace workspace;
    private final ResearchGateway gateway;
    private final JLabel statusLabel;
    private final JTextField symbolField = UiTheme.textField(12);
    private final JTextField followUpField = UiTheme.textField(28);
    private final JButton askButton = UiTheme.button("ASK");
    private final JTextArea results = UiTheme.resultArea();
    private final JButton explainButton = UiTheme.button("WHY <GO>", true);
    private int requestGeneration;

    public AgentPanel(
            SessionWorkspace workspace,
            ResearchGateway gateway,
            JLabel statusLabel) {

        this.workspace = workspace;
        this.gateway = gateway;
        this.statusLabel = statusLabel;

        setLayout(new BorderLayout(0, 8));
        setBackground(Color.BLACK);
        setBorder(UiTheme.padding(10));

        UiTheme.uppercase(symbolField);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        form.setBackground(Color.BLACK);
        form.add(UiTheme.fieldLabel("SYMBOL"));
        form.add(symbolField);
        form.add(explainButton);
        symbolField.addActionListener(event -> explain());
        explainButton.addActionListener(event -> explain());

        JPanel followUp = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        followUp.setBackground(Color.BLACK);
        followUp.add(UiTheme.fieldLabel("FOLLOW-UP"));
        followUp.add(followUpField);
        followUp.add(askButton);
        askButton.addActionListener(event -> ask(followUpField.getText()));
        followUpField.addActionListener(event -> ask(followUpField.getText()));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.BLACK);
        header.add(new UiTheme.JPanelBar("WHY", "MOVEMENT EXPLANATION"), BorderLayout.NORTH);
        header.add(form, BorderLayout.CENTER);
        header.add(followUp, BorderLayout.SOUTH);

        results.setText(
                "ENTER ONE SYMBOL AND PRESS WHY <GO>.\n"
                        + "THE DESK LOADS THE QUOTE AND RECENT NEWS, THEN ASKS GEMINI\n"
                        + "TO EXPLAIN ONLY THAT EVIDENCE.\n"
                        + "IF GEMINI IS OFF, THE EVIDENCE IS STILL SHOWN.\n"
                        + "COMMAND LINE: WHY NVDA\n"
                        + "FOLLOW-UP: what about its sector exposure\n"
        );

        add(header, BorderLayout.NORTH);
        add(UiTheme.scroll(results), BorderLayout.CENTER);
    }

    public void clear() {
        requestGeneration++;
        symbolField.setText("");
        followUpField.setText("");
        results.setText("");
    }

    public void explainSymbol(String symbol) {
        symbolField.setText(symbol);
        explain();
    }

    public void ask(String question) {
        if (!workspace.isAuthenticated()) {
            show("Error: Sign in again before asking a follow-up.\n");
            return;
        }
        followUpField.setText(question == null ? "" : question);
        long userId = gateway.currentUserId();
        int generation = ++requestGeneration;
        List<JComponent> controls = List.of(followUpField, askButton, symbolField, explainButton);
        BackgroundWork.submit(
                statusLabel,
                controls,
                () -> {
                    followUpField.setEnabled(true);
                    askButton.setEnabled(true);
                    symbolField.setEnabled(true);
                    explainButton.setEnabled(true);
                },
                "Planning the follow-up...",
                () -> gateway.ask(userId, question),
                text -> {
                    if (generation == requestGeneration) {
                        show(text);
                    }
                }
        );
    }

    private void explain() {
        if (!workspace.isAuthenticated()) {
            show("Error: Sign in again before requesting an explanation.\n");
            return;
        }

        String symbol = symbolField.getText();
        long userId = gateway.currentUserId();
        int generation = ++requestGeneration;
        List<JComponent> controls = List.of(symbolField, explainButton);

        BackgroundWork.submit(
                statusLabel,
                controls,
                () -> {
                    symbolField.setEnabled(true);
                    explainButton.setEnabled(true);
                },
                "Planning the movement question. Gemini can take up to 2 minutes.",
                () -> gateway.ask(userId, "why did " + symbol + " move"),
                text -> {
                    if (generation == requestGeneration) {
                        show(text);
                    }
                }
        );
    }

    private void show(String text) {
        results.setText(text == null ? "" : text);
        results.setCaretPosition(0);
    }
}
