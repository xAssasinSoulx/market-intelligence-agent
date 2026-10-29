package com.marketintel.gui;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

/**
 * Horizontal market strip. Each quote keeps its own color.
 * The text moves on the event thread after the network call has finished.
 */
public final class MarketTape extends JPanel {

    private static final String GAP = "     ·     ";

    private List<TapeLine.Piece> pieces = List.of(
            new TapeLine.Piece("MARKET  ·  SIGN ON TO LOAD THE TAPE", TapeLine.Tone.NEWS)
    );
    private int offset;
    private final Timer scroll;

    public MarketTape() {
        setBackground(UiTheme.BACKGROUND);
        setPreferredSize(new Dimension(400, 34));
        setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.ACCENT));
        scroll = new Timer(16, event -> {
            offset += 1;
            repaint();
        });
    }

    public void setLine(String text) {
        String shown = text == null || text.isBlank()
                ? "MARKET  ·  NO TAPE"
                : text.trim();
        pieces = List.of(new TapeLine.Piece(shown, TapeLine.Tone.NEWS));
        offset = 0;
        repaint();
    }

    public void setPieces(List<TapeLine.Piece> next) {
        pieces = next == null || next.isEmpty()
                ? List.of(new TapeLine.Piece(
                        "MARKET TAPE  ·  QUOTES AND NEWS ARE UNAVAILABLE",
                        TapeLine.Tone.NEWS
                ))
                : List.copyOf(next);
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
        FontMetrics metrics = drawing.getFontMetrics();
        int width = cycleWidth(metrics);
        int baseline = (getHeight() + metrics.getAscent() - metrics.getDescent()) / 2;
        int start = -(offset % width);
        paintCycle(drawing, metrics, start, baseline);
        paintCycle(drawing, metrics, start + width, baseline);
        drawing.dispose();
    }

    private int cycleWidth(FontMetrics metrics) {
        int width = 0;
        for (int index = 0; index < pieces.size(); index++) {
            if (index > 0) {
                width += metrics.stringWidth(GAP);
            }
            width += metrics.stringWidth(pieces.get(index).text());
        }
        return Math.max(width + metrics.stringWidth("   "), 1);
    }

    private void paintCycle(Graphics2D drawing, FontMetrics metrics, int x, int baseline) {
        int cursor = x;
        for (int index = 0; index < pieces.size(); index++) {
            if (index > 0) {
                drawing.setColor(UiTheme.MUTED);
                drawing.drawString(GAP, cursor, baseline);
                cursor += metrics.stringWidth(GAP);
            }
            TapeLine.Piece piece = pieces.get(index);
            drawing.setColor(TapeLine.color(piece.tone()));
            drawing.drawString(piece.text(), cursor, baseline);
            cursor += metrics.stringWidth(piece.text());
        }
    }
}
