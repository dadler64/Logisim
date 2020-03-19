/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.util;

import com.cburch.logisim.data.Bounds;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import javax.swing.JTextField;

public class EditableLabel implements Cloneable {

    public static final int LEFT = JTextField.LEFT;
    public static final int RIGHT = JTextField.RIGHT;
    public static final int CENTER = JTextField.CENTER;

    public static final int TOP = 8;
    public static final int MIDDLE = 9;
    public static final int BASELINE = 10;
    public static final int BOTTOM = 11;

    private int x;
    private int y;
    private String text;
    private Font font;
    public Color color;
    private int horizontalAlignment;
    private int verticalAlignment;
    private boolean areDimensionsKnown;
    private int width;
    private int ascent;
    private int descent;
    private int[] charX;
    private int[] charY;

    public EditableLabel(int x, int y, String text, Font font) {
        this.x = x;
        this.y = y;
        this.text = text;
        this.font = font;
        this.color = Color.BLACK;
        this.horizontalAlignment = LEFT;
        this.verticalAlignment = BASELINE;
        this.areDimensionsKnown = false;
    }

    @Override
    public EditableLabel clone() {
        try {
            return (EditableLabel) super.clone();
        } catch (CloneNotSupportedException e) {
            return new EditableLabel(x, y, text, font);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof EditableLabel) {
            EditableLabel that = (EditableLabel) other;
            return this.x == that.x && this.y == that.y
                    && this.text.equals(that.text) && this.font.equals(that.font)
                    && this.color.equals(that.color)
                    && this.horizontalAlignment == that.horizontalAlignment
                    && this.verticalAlignment == that.verticalAlignment;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hashCode = x * 31 + y;
        hashCode = hashCode * 31 + text.hashCode();
        hashCode = hashCode * 31 + font.hashCode();
        hashCode = hashCode * 31 + color.hashCode();
        hashCode = hashCode * 31 + horizontalAlignment;
        hashCode = hashCode * 31 + verticalAlignment;
        return hashCode;
    }

    //
    // accessor methods
    //
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        areDimensionsKnown = false;
        this.text = text;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
        areDimensionsKnown = false;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public int getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public void setHorizontalAlignment(int horizontalAlignment) {
        if (horizontalAlignment != LEFT && horizontalAlignment != CENTER && horizontalAlignment != RIGHT) {
            throw new IllegalArgumentException("argument must be LEFT, CENTER, or RIGHT");
        }
        this.horizontalAlignment = horizontalAlignment;
        areDimensionsKnown = false;
    }

    public int getVerticalAlignment() {
        return verticalAlignment;
    }

    public void setVerticalAlignment(int verticalAlignment) {
        if (verticalAlignment != TOP && verticalAlignment != MIDDLE && verticalAlignment != BASELINE
                && verticalAlignment != BOTTOM) {
            throw new IllegalArgumentException("argument must be TOP, MIDDLE, BASELINE, or BOTTOM");
        }
        this.verticalAlignment = verticalAlignment;
        areDimensionsKnown = false;
    }

    //
    // more complex methods
    //
    public Bounds getBounds() {
        int x0 = getLeftX();
        int y0 = getBaseY() - ascent;
        int width = this.width;
        int height = ascent + descent;
        return Bounds.create(x0, y0, width, height);
    }

    public boolean contains(int qx, int qy) {
        int x0 = getLeftX();
        int y0 = getBaseY();
        if (qx >= x0 && qx < x0 + width
                && qy >= y0 - ascent && qy < y0 + descent) {
            int[] xs = charX;
            int[] ys = charY;
            if (xs == null || ys == null) {
                return true;
            } else {
                int i = Arrays.binarySearch(xs, qx - x0);
                if (i < 0) {
                    i = -(i + 1);
                }
                if (i >= xs.length) {
                    return false;
                } else {
                    int asc = (ys[i] >> 16) & 0xFFFF;
                    int desc = ys[i] & 0xFFFF;
                    int dy = y0 - qy;
                    return dy >= -desc && dy <= asc;
                }
            }
        } else {
            return false;
        }
    }

    private int getLeftX() {
        switch (horizontalAlignment) {
            case LEFT:
                return x;
            case CENTER:
                return x - width / 2;
            case RIGHT:
                return x - width;
            default:
                return x;
        }
    }

    private int getBaseY() {
        switch (verticalAlignment) {
            case TOP:
                return y + ascent;
            case MIDDLE:
                return y + (ascent - descent) / 2;
            case BASELINE:
                return y;
            case BOTTOM:
                return y - descent;
            default:
                return y;
        }
    }

    public void configureTextField(EditableLabelField field) {
        configureTextField(field, 1.0);
    }

    public void configureTextField(EditableLabelField field, double zoom) {
        Font font = this.font;
        if (zoom != 1.0) {
            font = font.deriveFont(AffineTransform.getScaleInstance(zoom, zoom));
        }
        field.setFont(font);

        Dimension dimension = field.getPreferredSize();
        int width;
        int border = EditableLabelField.FIELD_BORDER;
        if (areDimensionsKnown) {
            width = this.width + 1 + 2 * border;
        } else {
            FontMetrics fontMetrics = field.getFontMetrics(this.font);
            ascent = fontMetrics.getAscent();
            descent = fontMetrics.getDescent();
            width = 0;
        }

        int x0 = x;
        int y0 = getBaseY() - ascent;
        if (zoom != 1.0) {
            x0 = (int) Math.round(x0 * zoom);
            y0 = (int) Math.round(y0 * zoom);
            width = (int) Math.round(width * zoom);
        }

        width = Math.max(width, dimension.width);
        int height = dimension.height;
        switch (horizontalAlignment) {
            case LEFT:
                x0 = x0 - border;
                break;
            case CENTER:
                x0 = x0 - (width / 2) + 1;
                break;
            case RIGHT:
                x0 = x0 - width + border + 1;
                break;
            default:
                x0 = x0 - border;
        }
        y0 = y0 - border;

        field.setHorizontalAlignment(horizontalAlignment);
        field.setForeground(color);
        field.setBounds(x0, y0, width, height);
    }

    public void paint(Graphics graphics) {
        graphics.setFont(font);
        if (!areDimensionsKnown) {
            computeDimensions(graphics, font, graphics.getFontMetrics());
        }
        int x0 = getLeftX();
        int y0 = getBaseY();
        graphics.setColor(color);
        graphics.drawString(text, x0, y0);
    }

    private void computeDimensions(Graphics graphics, Font font, FontMetrics fontMetrics) {
        String text = this.text;
        FontRenderContext context = ((Graphics2D) graphics).getFontRenderContext();
        width = fontMetrics.stringWidth(text);
        ascent = fontMetrics.getAscent();
        descent = fontMetrics.getDescent();
        int[] xs = new int[text.length()];
        int[] ys = new int[text.length()];
        for (int i = 0; i < xs.length; i++) {
            xs[i] = fontMetrics.stringWidth(text.substring(0, i + 1));
            TextLayout layout = new TextLayout(text.substring(i, i + 1), font, context);
            Rectangle2D rectangle = layout.getBounds();
            int asc = (int) Math.ceil(-rectangle.getMinY());
            int desc = (int) Math.ceil(rectangle.getMaxY());
            if (asc < 0) {
                asc = 0;
            }
            if (asc > 0xFFFF) {
                asc = 0xFFFF;
            }
            if (desc < 0) {
                desc = 0;
            }
            if (desc > 0xFFFF) {
                desc = 0xFFFF;
            }
            ys[i] = (asc << 16) | desc;
        }
        charX = xs;
        charY = ys;
        areDimensionsKnown = true;
    }
}
