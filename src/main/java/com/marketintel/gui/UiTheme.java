package com.marketintel.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Insets;

/**
 * Amber-on-black desk styling. Colors are painted onto the components so
 * the window stays readable on the macOS system theme.
 */
public final class UiTheme {

    public static final Color BACKGROUND = Color.BLACK;
    public static final Color SIDEBAR = Color.BLACK;
    public static final Color CARD = new Color(0x100C04);
    public static final Color FIELD = Color.BLACK;
    public static final Color BORDER = new Color(0x6A4A00);
    public static final Color TEXT = new Color(0xF6E7C1);
    public static final Color MUTED = new Color(0xA8843A);
    public static final Color ACCENT = new Color(0xFFB000);
    public static final Color ACCENT_DARK = new Color(0xC48400);
    public static final Color BUTTON = new Color(0x1A1204);
    public static final Color DANGER = new Color(0xFF5A5A);
    public static final Color SUCCESS = new Color(0x3DDC6A);
    public static final Color SELECTED = new Color(0x3A2800);

    public static final Font UI_FONT = mono(Font.PLAIN, 13);
    public static final Font UI_FONT_BOLD = mono(Font.BOLD, 13);
    public static final Font TITLE_FONT = mono(Font.BOLD, 16);
    public static final Font MONO = UI_FONT;

    private UiTheme() {
    }

    public static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // Explicit colors still apply if the look and feel cannot change.
        }
        UIManager.put("Button.disabledText", MUTED);
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("ScrollBar.thumb", ACCENT_DARK);
        UIManager.put("ScrollBar.track", BACKGROUND);
        UIManager.put("ScrollBar.background", BACKGROUND);
    }

    public static JPanelBar titleBar(String code, String title) {
        return new JPanelBar(code, title);
    }

    public static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(TITLE_FONT);
        label.setForeground(ACCENT);
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

    public static JLabel fieldLabel(String value) {
        JLabel label = new JLabel(value);
        label.setFont(UI_FONT_BOLD);
        label.setForeground(ACCENT);
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

    public static void uppercase(JTextField field) {
        if (!(field.getDocument() instanceof AbstractDocument document)) {
            return;
        }
        document.setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(
                    FilterBypass bypass,
                    int offset,
                    String text,
                    AttributeSet attributes) throws BadLocationException {

                super.insertString(bypass, offset, upper(text), attributes);
            }

            @Override
            public void replace(
                    FilterBypass bypass,
                    int offset,
                    int length,
                    String text,
                    AttributeSet attributes) throws BadLocationException {

                super.replace(bypass, offset, length, upper(text), attributes);
            }
        });
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
        area.setMargin(new Insets(10, 12, 10, 12));
        area.setText("");
        return area;
    }

    public static JScrollPane scroll(JComponent view) {
        JScrollPane pane = new JScrollPane(view);
        pane.setBorder(BorderFactory.createLineBorder(ACCENT_DARK));
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
        button.setContentAreaFilled(true);
        button.setBackground(primary ? ACCENT : BACKGROUND);
        button.setForeground(primary ? Color.BLACK : ACCENT);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? ACCENT : ACCENT_DARK),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static void styleTable(JTable table) {
        table.setBackground(BACKGROUND);
        table.setForeground(TEXT);
        table.setGridColor(BORDER);
        table.setShowGrid(true);
        table.setSelectionBackground(SELECTED);
        table.setSelectionForeground(ACCENT);
        table.setFont(MONO);
        table.setRowHeight(22);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable source,
                    Object value,
                    boolean selected,
                    boolean focus,
                    int row,
                    int column) {

                Component rendered = super.getTableCellRendererComponent(
                        source, value, selected, focus, row, column
                );
                rendered.setBackground(new Color(0x1A1200));
                rendered.setForeground(ACCENT);
                rendered.setFont(UI_FONT_BOLD);
                if (rendered instanceof JComponent component) {
                    component.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, BORDER));
                }
                return rendered;
            }
        });
    }

    public static void styleList(JList<?> list) {
        list.setBackground(BACKGROUND);
        list.setForeground(ACCENT);
        list.setFont(MONO);
        list.setSelectionBackground(SELECTED);
        list.setSelectionForeground(TEXT);
        list.setFixedCellHeight(24);
    }

    public static void styleCombo(JComboBox<?> combo) {
        combo.setBackground(BACKGROUND);
        combo.setForeground(ACCENT);
        combo.setFont(MONO);
        combo.setBorder(BorderFactory.createLineBorder(ACCENT_DARK));
        combo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean selected,
                    boolean focus) {

                Component rendered = super.getListCellRendererComponent(
                        list, value, index, selected, focus
                );
                rendered.setBackground(selected ? SELECTED : BACKGROUND);
                rendered.setForeground(ACCENT);
                rendered.setFont(MONO);
                return rendered;
            }
        });
    }

    public static Border padding(int pixels) {
        return BorderFactory.createEmptyBorder(pixels, pixels, pixels, pixels);
    }

    public static Border amberBox(int padding) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT),
                padding(padding)
        );
    }

    private static void styleField(JTextField field) {
        field.setFont(MONO);
        field.setBackground(FIELD);
        field.setForeground(ACCENT);
        field.setCaretColor(ACCENT);
        field.setSelectionColor(SELECTED);
        field.setSelectedTextColor(TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_DARK),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
    }

    private static Font mono(int style, int size) {
        Font menlo = new Font("Menlo", style, size);
        if ("Menlo".equals(menlo.getFamily())) {
            return menlo;
        }
        return new Font(Font.MONOSPACED, style, size);
    }

    private static String upper(String text) {
        return text == null ? null : text.toUpperCase();
    }

    /**
     * Amber title strip used at the top of each desk screen.
     */
    public static final class JPanelBar extends javax.swing.JPanel {

        public JPanelBar(String code, String title) {
            super(new java.awt.BorderLayout());
            setBackground(ACCENT);
            setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            JLabel label = new JLabel(code + "   " + title);
            label.setFont(UI_FONT_BOLD);
            label.setForeground(Color.BLACK);
            add(label, java.awt.BorderLayout.WEST);
        }
    }
}
