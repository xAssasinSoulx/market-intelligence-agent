package com.marketintel.gui;

import com.marketintel.app.SessionWorkspace;
import com.marketintel.model.User;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.List;

/**
 * Registration and login. Password text is kept in masked fields and is
 * not copied into status messages.
 */
public final class AuthenticationPanel extends JPanel {

    private final SessionWorkspace workspace;
    private final Runnable onAuthenticated;
    private final JLabel statusLabel;

    private final JTextField usernameField = UiTheme.textField(24);
    private final JPasswordField passwordField = UiTheme.passwordField(24);
    private final JPasswordField confirmField = UiTheme.passwordField(24);
    private final JButton loginButton = UiTheme.button("LOGIN <GO>", true);
    private final JButton registerButton = UiTheme.button("CREATE");

    public AuthenticationPanel(
            SessionWorkspace workspace,
            Runnable onAuthenticated,
            JLabel statusLabel) {

        this.workspace = workspace;
        this.onAuthenticated = onAuthenticated;
        this.statusLabel = statusLabel;

        setLayout(new GridBagLayout());
        setBackground(Color.BLACK);
        setBorder(UiTheme.padding(24));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.BLACK);
        card.setBorder(UiTheme.amberBox(22));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(0, 0, 8, 0);
        card.add(new UiTheme.JPanelBar("SIGN", "SECURE SIGN-ON"), constraints);

        constraints.gridy++;
        card.add(UiTheme.text(
                "LOCAL ACCOUNT  ·  PASSWORD IS MASKED",
                true
        ), constraints);

        constraints.gridy++;
        constraints.insets = new Insets(18, 0, 4, 0);
        card.add(UiTheme.fieldLabel("USER"), constraints);

        constraints.gridy++;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 8, 0);
        card.add(usernameField, constraints);

        constraints.gridy++;
        constraints.fill = GridBagConstraints.NONE;
        card.add(UiTheme.fieldLabel("PASSWORD"), constraints);

        constraints.gridy++;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        card.add(passwordField, constraints);

        constraints.gridy++;
        constraints.fill = GridBagConstraints.NONE;
        card.add(UiTheme.fieldLabel("CONFIRM  ·  CREATE ONLY"), constraints);

        constraints.gridy++;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        card.add(confirmField, constraints);

        constraints.gridy++;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(16, 0, 0, 8);
        card.add(loginButton, constraints);

        constraints.gridx = 1;
        constraints.insets = new Insets(16, 0, 0, 0);
        card.add(registerButton, constraints);

        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(16, 0, 0, 0);
        card.add(UiTheme.text(
                "USER  3-30  LETTERS, DIGITS, . _ -     PASSWORD  8+ CHARACTERS",
                true
        ), constraints);

        add(card);

        loginButton.addActionListener(event -> submit(false));
        registerButton.addActionListener(event -> submit(true));
        passwordField.addActionListener(event -> submit(false));
        confirmField.addActionListener(event -> submit(true));
    }

    public void clear() {
        usernameField.setText("");
        passwordField.setText("");
        confirmField.setText("");
    }

    private void submit(boolean register) {
        String username = usernameField.getText();
        String password = readPassword(passwordField);
        String confirmation = readPassword(confirmField);

        if (register && !password.equals(confirmation)) {
            statusLabel.setText("Passwords do not match.");
            return;
        }

        List<JComponent> controls =
                List.of(loginButton, registerButton, usernameField, passwordField, confirmField);

        BackgroundWork.submit(
                statusLabel,
                controls,
                () -> setFormEnabled(true),
                register ? "Creating account..." : "Signing in...",
                () -> register
                        ? registerAccount(username, password)
                        : signIn(username, password),
                message -> {
                    if (message == null) {
                        clear();
                        onAuthenticated.run();
                    } else {
                        if (!register) {
                            passwordField.setText("");
                        }
                        statusLabel.setText(message);
                    }
                }
        );
    }

    private String signIn(String username, String password) {
        try {
            workspace.login(username, password);
            return null;
        } catch (SecurityException exception) {
            return "Invalid username or password.";
        } catch (IllegalArgumentException exception) {
            return safeValidationMessage(exception);
        } catch (RuntimeException exception) {
            return "Unable to sign in.";
        }
    }

    private String registerAccount(String username, String password) {
        try {
            User user = workspace.register(username, password);
            return "Account created for " + user.getUsername() + ". Log in to continue.";
        } catch (IllegalArgumentException exception) {
            return safeValidationMessage(exception);
        } catch (RuntimeException exception) {
            return "Unable to create the account.";
        }
    }

    private void setFormEnabled(boolean enabled) {
        usernameField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        confirmField.setEnabled(enabled);
        loginButton.setEnabled(enabled);
        registerButton.setEnabled(enabled);
    }

    private static String readPassword(JPasswordField field) {
        char[] characters = field.getPassword();
        String password = new String(characters);
        Arrays.fill(characters, '\0');
        return password;
    }

    private static String safeValidationMessage(IllegalArgumentException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "Check the username and password.";
        }
        return message;
    }
}
