/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.util;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class GraphicsUtil {

    public static final int H_LEFT = -1;
    public static final int H_CENTER = 0;
    public static final int H_RIGHT = 1;
    public static final int V_TOP = -1;
    public static final int V_CENTER = 0;
    public static final int V_BASELINE = 1;
    public static final int V_BOTTOM = 2;
    public static final int V_CENTER_OVERALL = 3;

    static public void switchToWidth(Graphics g, int width) {
        if (g instanceof Graphics2D) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke(new BasicStroke((float) width));
        }
    }

    static public void drawCenteredArc(Graphics g, int x, int y, int r, int start, int dist) {
        g.drawArc(x - r, y - r, 2 * r, 2 * r, start, dist);
    }

    static public Rectangle getTextBounds(Graphics g, Font font, String text, int x, int y, int hAlign, int vAlign) {
        if (g == null) {
            return new Rectangle(x, y, 0, 0);
        }
        Font oldFont = g.getFont();
        if (font != null) {
            g.setFont(font);
        }
        Rectangle textBounds = getTextBounds(g, text, x, y, hAlign, vAlign);
        if (font != null) {
            g.setFont(oldFont);
        }
        return textBounds;
    }

    static public Rectangle getTextBounds(Graphics g, String text, int x, int y, int hAlign, int vAlign) {
        if (g == null) {
            return new Rectangle(x, y, 0, 0);
        }
        FontMetrics fm = g.getFontMetrics();
        int width = fm.stringWidth(text);
        int ascent = fm.getAscent();
        int descent = fm.getDescent();
        int height = ascent + descent;

        Rectangle rectangle = new Rectangle(x, y, width, height);
        switch (hAlign) {
            case H_CENTER:
                rectangle.translate(-(width / 2), 0);
                break;
            case H_RIGHT:
                rectangle.translate(-width, 0);
                break;
            default:
        }
        switch (vAlign) {
            case V_TOP:
                break;
            case V_CENTER:
                rectangle.translate(0, -(ascent / 2));
                break;
            case V_CENTER_OVERALL:
                rectangle.translate(0, -(height / 2));
                break;
            case V_BASELINE:
                rectangle.translate(0, -ascent);
                break;
            case V_BOTTOM:
                rectangle.translate(0, -height);
                break;
            default:
        }
        return rectangle;
    }

    static public void drawText(Graphics g, Font font, String text, int x, int y, int hAlign, int vAlign) {
        Font oldFont = g.getFont();
        if (font != null) {
            g.setFont(font);
        }
        drawText(g, text, x, y, hAlign, vAlign);
        if (font != null) {
            g.setFont(oldFont);
        }
    }

    static public void drawText(Graphics g, String text, int x, int y, int hAlign, int vAlign) {
        if (text.length() == 0) {
            return;
        }
        Rectangle textBounds = getTextBounds(g, text, x, y, hAlign, vAlign);
        g.drawString(text, textBounds.x, textBounds.y + g.getFontMetrics().getAscent());
    }

    static public void drawCenteredText(Graphics g, String text, int x, int y) {
        drawText(g, text, x, y, H_CENTER, V_CENTER);
    }

    static public void drawArrow(Graphics g, int x0, int y0, int x1, int y1, int headLength, int headAngle) {
        double offset = headAngle * Math.PI / 180.0;
        double angle = Math.atan2(y0 - y1, x0 - x1);
        int[] xs = {x1 + (int) (headLength * Math.cos(angle + offset)), x1, x1 + (int) (headLength * Math.cos(angle - offset))};
        int[] ys = {y1 + (int) (headLength * Math.sin(angle + offset)), y1, y1 + (int) (headLength * Math.sin(angle - offset))};
        g.drawLine(x0, y0, x1, y1);
        g.drawPolyline(xs, ys, 3);
    }
}
