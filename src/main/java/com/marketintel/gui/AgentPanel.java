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
import java.awt.FlowLayout;
import java.util.List;

/**
 * Direct movement explanation, equivalent to the terminal /why command.
 * Follow-up questions stay disabled until conversation memory exists.
 */
public final class AgentPanel extends JPanel {

    private final SessionWorkspace workspace;
    private final ResearchGateway gateway;
    private final JLabel statusLabel;
    private final JTextField symbolField = UiTheme.textField(12);
    private final JTextField followUpField = UiTheme.textField(28);
    private final JTextArea results = UiTheme.resultArea();
    private final JButton explainButton = UiTheme.button("Explain movement", true);
    private int requestGeneration;

    public AgentPanel(
            SessionWorkspace workspace,
            ResearchGateway gateway,
            JLabel statusLabel) {

        this.workspace = workspace;
        this.gateway = gateway;
        this.statusLabel = statusLabel;

        setLayout(new BorderLayout(0, 12));
        setBackground(UiTheme.BACKGROUND);
        setBorder(UiTheme.padding(20));

        followUpField.setEnabled(false);
        followUpField.setToolTipText(
                "Contextual follow-up is not implemented. Ask with a symbol."
        );

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        form.setBackground(UiTheme.BACKGROUND);
        form.add(UiTheme.text("Symbol"));
        form.add(symbolField);
        form.add(explainButton);
        symbolField.addActionListener(event -> explain());
        explainButton.addActionListener(event -> explain());

        JPanel followUp = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        followUp.setBackground(UiTheme.BACKGROUND);
        followUp.add(UiTheme.text("Follow-up", true));
        followUp.add(followUpField);
        followUp.add(UiTheme.text(
                "Disabled until per-user conversation context exists.",
                true
        ));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UiTheme.BACKGROUND);
        header.add(UiTheme.heading("Movement explanation"), BorderLayout.NORTH);
        header.add(form, BorderLayout.CENTER);
        header.add(followUp, BorderLayout.SOUTH);

        results.setText(
                "Enter one symbol. The application loads the quote and recent news, "
                        + "then asks Gemini to explain only that evidence.\n"
                        + "If Gemini is unavailable, the evidence is still shown.\n"
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
                "Gathering evidence...",
                () -> ResultFormatter.movement(
                        gateway.explainMovement(userId, symbol)
                ),
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
