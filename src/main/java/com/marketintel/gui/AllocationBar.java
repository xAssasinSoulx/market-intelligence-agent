package com.marketintel.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * One horizontal bar. Segment width is share of priced market value.
 * Up, down, and flat use the desk colors. An empty book draws no shares.
 */
public final class AllocationBar extends javax.swing.JPanel {

    private List<HomeDeskView.Segment> segments = List.of();

    public AllocationBar() {
        setBackground(Color.BLACK);
        setOpaque(true);
        setPreferredSize(new Dimension(100, 14));
        setMinimumSize(new Dimension(20, 14));
    }

    public void setSegments(List<HomeDeskView.Segment> segments) {
        this.segments = segments == null ? List.of() : List.copyOf(segments);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        if (segments.isEmpty()) {
            graphics.setColor(UiTheme.BORDER);
            graphics.fillRect(0, height / 2, width, 1);
            return;
        }
        int x = 0;
        for (int index = 0; index < segments.size(); index++) {
            int segmentWidth = index == segments.size() - 1
                    ? Math.max(0, width - x)
                    : segments.get(index).fraction()
                            .multiply(BigDecimal.valueOf(width))
                            .setScale(0, RoundingMode.DOWN)
                            .intValue();
            if (segmentWidth < 0) {
                segmentWidth = 0;
            }
            int gap = segmentWidth > 2 && index < segments.size() - 1 ? 1 : 0;
            graphics.setColor(color(segments.get(index).tone()));
            graphics.fillRect(x, 0, Math.max(0, segmentWidth - gap), height);
            x += segmentWidth;
        }
    }

    private static Color color(HomeDeskView.Tone tone) {
        return switch (tone) {
            case UP -> UiTheme.SUCCESS;
            case DOWN -> UiTheme.DANGER;
            case FLAT -> UiTheme.TEXT;
        };
    }
}
