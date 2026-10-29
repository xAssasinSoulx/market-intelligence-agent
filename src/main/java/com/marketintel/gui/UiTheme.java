package com.marketintel.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Insets;

/**
 * Dark, readable Swing styling shared by every panel. Colors are applied
 * directly so the window does not depend on the macOS system theme.
 */
public final class UiTheme {

    public static final Color BACKGROUND = new Color(0x0E141B);
    public static final Color SIDEBAR = new Color(0x121A22);
    public static final Color CARD = new Color(0x18222C);
    public static final Color FIELD = new Color(0x0C1218);
    public static final Color BORDER = new Color(0x2C3C4A);
    public static final Color TEXT = new Color(0xE8EEF4);
    public static final Color MUTED = new Color(0x8EA0B0);
    public static final Color ACCENT = new Color(0xE8A317);
    public static final Color ACCENT_DARK = new Color(0x8A6110);
    public static final Color BUTTON = new Color(0x243140);
    public static final Color DANGER = new Color(0xFF6B6B);
    public static final Color SUCCESS = new Color(0x3DDB8C);

    public static final Font UI_FONT = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font UI_FONT_BOLD = new Font("SansSerif", Font.BOLD, 14);
    public static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 22);
    public static final Font MONO = monoFont();

    private UiTheme() {
    }

    public static void install() {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getCrossPlatformLookAndFeelClassName()
            );
        } catch (Exception ignored) {
            // Explicit component colors still apply if the look and feel fails.
        }
    }

    public static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(TITLE_FONT);
        label.setForeground(TEXT);
        return label;
    }

    public static JLabel text(String value) {
        return text(value, false);
    }

    public static JLabel text(String value, boolean muted) {
        JLabel label = new JLabel(value);
        label.setFont(UI_FONT);
        label.setForeground(muted ? MUTED : TEXT);
        return label;
    }

    public static JTextField textField(int columns) {
        JTextField field = new JTextField(columns);
        styleField(field);
        return field;
    }

    public static JPasswordField passwordField(int columns) {
        JPasswordField field = new JPasswordField(columns);
        styleField(field);
        return field;
    }

    public static JTextArea resultArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(MONO);
        area.setBackground(FIELD);
        area.setForeground(TEXT);
        area.setCaretColor(ACCENT);
        area.setMargin(new Insets(12, 12, 12, 12));
        area.setText("");
        return area;
    }

    public static JScrollPane scroll(JComponent view) {
        JScrollPane pane = new JScrollPane(view);
        pane.setBorder(BorderFactory.createLineBorder(BORDER));
        pane.getViewport().setBackground(FIELD);
        pane.setBackground(FIELD);
        return pane;
    }

    public static JButton button(String label) {
        return button(label, false);
    }

    public static JButton button(String label, boolean primary) {
        JButton button = new JButton(label);
        button.setFont(UI_FONT_BOLD);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBackground(primary ? ACCENT : BUTTON);
        button.setForeground(primary ? Color.BLACK : TEXT);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? ACCENT_DARK : BORDER),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static Border padding(int pixels) {
        return BorderFactory.createEmptyBorder(pixels, pixels, pixels, pixels);
    }

    private static void styleField(JTextField field) {
        field.setFont(UI_FONT);
        field.setBackground(FIELD);
        field.setForeground(TEXT);
        field.setCaretColor(ACCENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(7, 8, 7, 8)
        ));
    }

    private static Font monoFont() {
        Font menlo = new Font("Menlo", Font.PLAIN, 13);
        if ("Menlo".equals(menlo.getFamily())) {
            return menlo;
        }
        return new Font(Font.MONOSPACED, Font.PLAIN, 13);
    }
}
