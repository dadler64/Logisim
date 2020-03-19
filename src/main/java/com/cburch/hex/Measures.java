/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.hex;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

class Measures {

    private HexEditor hex;
    private int headerChars;
    private int cellChars;
    private int headerWidth;
    private int spacerWidth;
    private int cellWidth;
    private int cellHeight;
    private int columns;
    private int baseX;
    private boolean isGuessed;

    public Measures(HexEditor hex) {
        this.hex = hex;
        this.isGuessed = true;
        this.columns = 1;
        this.cellWidth = -1;
        this.cellHeight = -1;
        this.cellChars = 2;
        this.headerChars = 4;

        computeCellSize(null);
    }

    public int getColumnCount() {
        return columns;
    }

    public int getBaseX() {
        return baseX;
    }

    public int getCellHeight() {
        return cellHeight;
    }

    public int getCellWidth() {
        return cellWidth;
    }

    public int getLabelWidth() {
        return headerWidth;
    }

    public int getLabelChars() {
        return headerChars;
    }

    public int getCellChars() {
        return cellChars;
    }

    public int getValuesX() {
        return baseX + spacerWidth;
    }

    public int getValuesWidth() {
        return ((columns - 1) / 4) * spacerWidth + columns * cellWidth;
    }

    public long getBaseAddress(HexModel model) {
        if (model == null) {
            return 0;
        } else {
            long addr0 = model.getFirstOffset();
            return addr0 - addr0 % columns;
        }
    }

    public int toY(long address) {
        long row = (address - getBaseAddress(hex.getModel())) / columns;
        long ret = row * cellHeight;
        return ret < Integer.MAX_VALUE ? (int) ret : Integer.MAX_VALUE;
    }

    public int toX(long address) {
        int column = (int) (address % columns);
        return baseX + (1 + (column / 4)) * spacerWidth + column * cellWidth;
    }

    public long toAddress(int x, int y) {
        HexModel model = hex.getModel();
        if (model == null) {
            return Integer.MIN_VALUE;
        }
        long addr0 = model.getFirstOffset();
        long addr1 = model.getLastOffset();

        long base = getBaseAddress(model) + ((long) y / cellHeight) * columns;
        int offset = (x - baseX) / (cellWidth + (spacerWidth + 2) / 4);
        if (offset < 0) {
            offset = 0;
        }
        if (offset >= columns) {
            offset = columns - 1;
        }

        long ret = base + offset;
        if (ret > addr1) {
            ret = addr1;
        }
        if (ret < addr0) {
            ret = addr0;
        }
        return ret;
    }

    void ensureComputed(Graphics graphics) {
        if (isGuessed || cellWidth < 0) {
            computeCellSize(graphics);
        }
    }

    void recompute() {
        computeCellSize(hex.getGraphics());
    }

    void widthChanged() {
        int oldColumns = columns;
        int width;
        if (isGuessed || cellWidth < 0) {
            columns = 16;
            width = hex.getPreferredSize().width;
        } else {
            width = hex.getWidth();
            int ret = (width - headerWidth) / (cellWidth + (spacerWidth + 3) / 4);
            if (ret >= 16) {
                columns = 16;
            } else if (ret >= 8) {
                columns = 8;
            } else {
                columns = 4;
            }
        }
        int lineWidth = headerWidth + columns * cellWidth
                + ((columns / 4) - 1) * spacerWidth;
        int newBase = headerWidth + Math.max(0, (width - lineWidth) / 2);
        if (baseX != newBase) {
            baseX = newBase;
            hex.repaint();
        }
        if (columns != oldColumns) {
            recompute();
        }
    }

    private void computeCellSize(Graphics graphics) {
        HexModel model = hex.getModel();

        // compute number of characters in headers and cells
        if (model == null) {
            headerChars = 4;
            cellChars = 2;
        } else {
            int logSize = 0;
            long addressEnd = model.getLastOffset();
            while (addressEnd > (1L << logSize)) {
                logSize++;
            }
            headerChars = (logSize + 3) / 4;
            cellChars = (model.getValueWidth() + 3) / 4;
        }

        // compute character sizes
        FontMetrics fontMetrics = graphics == null ? null : graphics.getFontMetrics(hex.getFont());
        int charWidth;
        int spaceWidth;
        int lineHeight;
        if (fontMetrics == null) {
            charWidth = 8;
            spaceWidth = 6;
            Font font = hex.getFont();
            if (font == null) {
                lineHeight = 16;
            } else {
                lineHeight = font.getSize();
            }
        } else {
            isGuessed = false;
            charWidth = 0;
            for (int i = 0; i < 16; i++) {
                int width = fontMetrics.stringWidth(Integer.toHexString(i));
                if (width > charWidth) {
                    charWidth = width;
                }
            }
            spaceWidth = fontMetrics.stringWidth(" ");
            lineHeight = fontMetrics.getHeight();
        }

        // update header and cell dimensions
        headerWidth = headerChars * charWidth + spaceWidth;
        spacerWidth = spaceWidth;
        cellWidth = cellChars * charWidth + spaceWidth;
        cellHeight = lineHeight;

        // compute preferred size
        int width = headerWidth + columns * cellWidth + (columns / 4) * spacerWidth;
        long height;
        if (model == null) {
            height = 16 * cellHeight;
        } else {
            long addr0 = getBaseAddress(model);
            long addr1 = model.getLastOffset();
            long rows = (int) (((addr1 - addr0 + 1) + columns - 1) / columns);
            height = rows * cellHeight;
            if (height > Integer.MAX_VALUE) {
                height = Integer.MAX_VALUE;
            }
        }

        // update preferred size
        Dimension preferredSize = hex.getPreferredSize();
        if (preferredSize.width != width || preferredSize.height != height) {
            preferredSize.width = width;
            preferredSize.height = (int) height;
            hex.setPreferredSize(preferredSize);
            hex.revalidate();
        }

        widthChanged();
    }
}
