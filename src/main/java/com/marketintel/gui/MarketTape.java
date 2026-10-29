package com.marketintel.gui;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Horizontal market strip. The text moves on the event thread.
 * Quote and news text is supplied from outside, after the network call.
 */
public final class MarketTape extends JPanel {

    private String line = "  MARKET  ·  SIGN ON TO LOAD THE TAPE";
    private int offset;
    private final Timer scroll;

    public MarketTape() {
        setBackground(Color.BLACK);
        setForeground(UiTheme.ACCENT);
        setPreferredSize(new Dimension(400, 28));
        setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.ACCENT));
        scroll = new Timer(30, event -> {
            offset += 1;
            repaint();
        });
    }

    public void setLine(String text) {
        String next = text == null || text.isBlank()
                ? "  MARKET  ·  NO TAPE"
                : "  " + text.trim() + "     ";
        line = next;
        offset = 0;
        repaint();
    }

    public void start() {
        if (!scroll.isRunning()) {
            scroll.start();
        }
    }

    public void stop() {
        scroll.stop();
        setLine("MARKET  ·  SIGN ON TO LOAD THE TAPE");
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D drawing = (Graphics2D) graphics.create();
        drawing.setFont(UiTheme.UI_FONT_BOLD);
        drawing.setColor(UiTheme.ACCENT);
        FontMetrics metrics = drawing.getFontMetrics();
        int width = Math.max(metrics.stringWidth(line), 1);
        int baseline = (getHeight() + metrics.getAscent() - metrics.getDescent()) / 2;
        int start = -(offset % width);
        drawing.drawString(line, start, baseline);
        drawing.drawString(line, start + width, baseline);
        drawing.dispose();
    }
}
