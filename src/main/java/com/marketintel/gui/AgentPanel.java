package com.marketintel.gui;

import com.marketintel.app.ResearchGateway;
import com.marketintel.app.SessionWorkspace;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;

/**
 * The conversation. The message box at the bottom of the window is the
 * only input. Each exchange stays on screen.
 */
public final class AgentPanel extends JPanel {

    private static final String WELCOME = """
            Desk
            Ask in the box below, the way you would ask a person.
            For example: why did Microsoft fall
            Or: say that more simply
            A desk command such as quote NVDA still works, and the reply stays in this conversation.
            """;

    private final SessionWorkspace workspace;
    private final ResearchGateway gateway;
    private final JLabel statusLabel;
    private final JTextArea composer;
    private final JButton sendButton;
    private final JTextArea transcript = UiTheme.resultArea();
    private int sessionGeneration;
    private boolean showingWelcome = true;

    public AgentPanel(
            SessionWorkspace workspace,
            ResearchGateway gateway,
            JLabel statusLabel,
            JTextArea composer,
            JButton sendButton) {

        this.workspace = workspace;
        this.gateway = gateway;
        this.statusLabel = statusLabel;
        this.composer = composer;
        this.sendButton = sendButton;

        setLayout(new BorderLayout(0, 8));
        setBackground(Color.BLACK);
        setBorder(UiTheme.padding(10));
        transcript.setText(WELCOME);
        add(new UiTheme.JPanelBar("CHAT", "CONVERSATION"), BorderLayout.NORTH);
        add(UiTheme.scroll(transcript), BorderLayout.CENTER);
    }

    public void clear() {
        sessionGeneration++;
        transcript.setText(WELCOME);
        transcript.setCaretPosition(0);
        showingWelcome = true;
    }

    public void explainSymbol(String symbol) {
        ask("why did " + symbol + " move");
    }

    public void ask(String question) {
        if (!workspace.isAuthenticated()) {
            append("Desk", "Sign in again before asking.");
            return;
        }
        String text = question == null ? "" : question.trim();
        if (text.isEmpty()) {
            return;
        }
        append("You", text);
        long userId = gateway.currentUserId();
        int generation = sessionGeneration;
        List<JComponent> controls = List.of(composer, sendButton);
        BackgroundWork.submit(
                statusLabel,
                controls,
                () -> {
                    composer.setEnabled(true);
                    sendButton.setEnabled(true);
                    composer.requestFocusInWindow();
                },
                "Writing the reply...",
                () -> gateway.ask(userId, text),
                reply -> {
                    if (generation == sessionGeneration) {
                        append("Desk", reply);
                    }
                }
        );
    }

    private void append(String speaker, String body) {
        String message = body == null ? "" : body.trim();
        if (message.isEmpty()) {
            return;
        }
        if (showingWelcome) {
            transcript.setText("");
            showingWelcome = false;
        }
        if (transcript.getDocument().getLength() > 0) {
            transcript.append("\n\n");
        }
        transcript.append(speaker);
        transcript.append("\n");
        transcript.append(message);
        transcript.append("\n");
        transcript.setCaretPosition(transcript.getDocument().getLength());
    }
}
