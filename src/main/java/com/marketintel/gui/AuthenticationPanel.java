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
 * Separate sign-on and account-creation cards. The confirmation field
 * exists only while an account is being created. Password text stays in
 * masked fields and is not copied into status messages.
 */
public final class AuthenticationPanel extends JPanel {

    private final SessionWorkspace workspace;
    private final Runnable onAuthenticated;
    private final JLabel statusLabel;

    private final JLabel title = new JLabel();
    private final JLabel hint = UiTheme.text("", true);
    private final JLabel confirmLabel = UiTheme.fieldLabel("CONFIRM");
    private final JTextField usernameField = UiTheme.textField(24);
    private final JPasswordField passwordField = UiTheme.passwordField(24);
    private final JPasswordField confirmField = UiTheme.passwordField(24);
    private final JButton primaryButton = UiTheme.button("LOGIN <GO>", true);
    private final JButton switchButton = UiTheme.button("CREATE ACCOUNT");
    private final JLabel rules = UiTheme.text("", true);
    private boolean creating;

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

        JPanel bar = new JPanel(new java.awt.BorderLayout());
        bar.setBackground(UiTheme.ACCENT);
        bar.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 10, 4, 10));
        title.setFont(UiTheme.UI_FONT_BOLD);
        title.setForeground(Color.BLACK);
        bar.add(title, java.awt.BorderLayout.WEST);

        GridBagConstraints constraints = constraints();
        card.add(bar, constraints);

        constraints.gridy++;
        card.add(hint, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(18, 0, 4, 0);
        card.add(UiTheme.fieldLabel("USER"), constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 8, 0);
        card.add(usernameField, constraints);

        constraints.gridy++;
        card.add(UiTheme.fieldLabel("PASSWORD"), constraints);

        constraints.gridy++;
        card.add(passwordField, constraints);

        constraints.gridy++;
        card.add(confirmLabel, constraints);

        constraints.gridy++;
        card.add(confirmField, constraints);

        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 12, 0));
        actions.setOpaque(false);
        actions.add(primaryButton);
        actions.add(switchButton);
        constraints.gridy++;
        constraints.insets = new Insets(16, 0, 0, 0);
        card.add(actions, constraints);

        constraints.gridy++;
        card.add(rules, constraints);

        add(card);

        primaryButton.addActionListener(event -> submit());
        switchButton.addActionListener(event -> showMode(!creating));
        passwordField.addActionListener(event -> {
            if (!creating) {
                submit();
            } else {
                confirmField.requestFocusInWindow();
            }
        });
        confirmField.addActionListener(event -> submit());
        showMode(false);
    }

    public void clear() {
        usernameField.setText("");
        passwordField.setText("");
        confirmField.setText("");
        showMode(false);
    }

    private void showMode(boolean createAccount) {
        creating = createAccount;
        title.setText(createAccount ? "OPEN   NEW ACCOUNT" : "SIGN   SECURE SIGN-ON");
        hint.setText(createAccount
                ? "NEW LOCAL ACCOUNT  ·  PASSWORD IS MASKED"
                : "LOCAL ACCOUNT  ·  PASSWORD IS MASKED");
        rules.setText(createAccount
                ? "USER  3-30  LETTERS, DIGITS, . _ -     PASSWORD  8+ CHARACTERS"
                : "ENTER THE ACCOUNT YOU ALREADY CREATED");
        primaryButton.setText(createAccount ? "CREATE <GO>" : "LOGIN <GO>");
        switchButton.setText(createAccount ? "BACK TO SIGN-ON" : "CREATE ACCOUNT");
        confirmLabel.setVisible(createAccount);
        confirmField.setVisible(createAccount);
        if (!createAccount) {
            confirmField.setText("");
        }
        revalidate();
        repaint();
    }

    private void submit() {
        String username = usernameField.getText();
        String password = readPassword(passwordField);
        String confirmation = readPassword(confirmField);

        if (creating && !password.equals(confirmation)) {
            statusLabel.setText("Passwords do not match.");
            return;
        }

        List<JComponent> controls = List.of(
                primaryButton,
                switchButton,
                usernameField,
                passwordField,
                confirmField
        );

        boolean register = creating;
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
                        passwordField.setText("");
                        confirmField.setText("");
                        if (register) {
                            showMode(false);
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
        confirmField.setEnabled(enabled && creating);
        primaryButton.setEnabled(enabled);
        switchButton.setEnabled(enabled);
    }

    private static GridBagConstraints constraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        constraints.insets = new Insets(0, 0, 8, 0);
        return constraints;
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
