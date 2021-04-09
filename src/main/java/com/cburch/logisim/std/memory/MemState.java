/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.memory;

import com.adlerd.logger.Logger;
import com.cburch.hex.HexModel;
import com.cburch.hex.HexModelListener;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;
import java.awt.Color;
import java.awt.Graphics;

class MemState implements InstanceData, Cloneable, HexModelListener {

    private static final int ROWS = 4; // rows in memory display

    private static final int TABLE_WIDTH_12 = 80; // width of table for addr bits <= 12
    private static final int TABLE_WIDTH_32 = 65; // width of table for addr bits > 12

    private static final int ENTRY_HEIGHT = 15; // pixels high per entry

    private static final int ENTRY_X_OFFSET_12 = 40; // x offset for entries for addr bits <= 12
    private static final int ENTRY_X_OFFSET_32 = 60; // x offset for entries for addr bits > 12

    private static final int ENTRY_Y_OFFSET = 5; // y offset for entries

    private static final int ADDR_WIDTH_PER_CHAR = 10; // pixels wide per address character

    private MemContents contents;
    private int columns;
    private long currentScroll = 0;
    private long cursorLocation = -1;
    private long currentAddress = -1;

    MemState(MemContents contents) {
        this.contents = contents;
        setBits(contents.getLogLength(), contents.getWidth());
        contents.addHexModelListener(this);
    }

    @Override
    public MemState clone() {
        try {
            MemState state = (MemState) super.clone();
            state.contents = contents.clone();
            state.contents.addHexModelListener(state);
            return state;
        } catch (CloneNotSupportedException e) {
            Logger.debugln(e.getMessage());
            return null;
        }
    }

    //
    // methods for accessing the address bits
    //
    private void setBits(int addressBits, int dataBits) {
        if (contents == null) {
            contents = MemContents.create(addressBits, dataBits);
        } else {
            contents.setDimensions(addressBits, dataBits);
        }
        if (addressBits <= 12) {
            if (dataBits <= 8) {
                columns = dataBits <= 4 ? 8 : 4;
            } else {
                columns = dataBits <= 16 ? 2 : 1;
            }
        } else {
            columns = dataBits <= 8 ? 2 : 1;
        }
        long newLast = contents.getLastOffset();
        // I do subtraction in the next two conditions to account for possibility of overflow
        if (cursorLocation > newLast) {
            cursorLocation = newLast;
        }
        if (currentAddress - newLast > 0) {
            currentAddress = -1;
        }
        long maxScroll = Math.max(0, newLast + 1 - (long) (ROWS - 1) * columns);
        if (currentScroll > maxScroll) {
            currentScroll = maxScroll;
        }
    }

    public MemContents getContents() {
        return contents;
    }

    //
    // methods for accessing data within memory
    //
    int getAddressBits() {
        return contents.getLogLength();
    }

    int getDataBits() {
        return contents.getWidth();
    }

    long getLastAddress() {
        return (1L << contents.getLogLength()) - 1;
    }

    boolean isValidAddress(long address) {
        int addrBits = contents.getLogLength();
        return address >>> addrBits == 0;
    }

    int getRows() {
        return ROWS;
    }

    int getColumns() {
        return columns;
    }

    //
    // methods for manipulating cursor and scroll location
    //
    long getCursor() {
        return cursorLocation;
    }

    void setCursor(long value) {
        cursorLocation = isValidAddress(value) ? value : -1L;
    }

    long getCurrent() {
        return currentAddress;
    }

    void setCurrent(long value) {
        currentAddress = isValidAddress(value) ? value : -1L;
    }

    long getScroll() {
        return currentScroll;
    }

    void setScroll(long address) {
        long maxAddress = getLastAddress() - (long) ROWS * columns;
        if (address > maxAddress) {
            address = maxAddress; // note: maxAddress could be negative
        }
        if (address < 0) {
            address = 0;
        }
        currentScroll = address;
    }

    void scrollToShow(long address) {
        if (isValidAddress(address)) {
            address = address / columns * columns;
            long currentTop = currentScroll / columns * columns;
            if (address < currentTop) {
                currentScroll = address;
            } else if (address >= currentTop + (long) ROWS * columns) {
                currentScroll = address - (long) (ROWS - 1) * columns;
                if (currentScroll < 0) {
                    currentScroll = 0;
                }
            }
        }
    }

    //
    // graphical methods
    //
    public long getAddressAt(int x, int y) {
        int addressBits = getAddressBits();
        int boxX = addressBits <= 12 ? ENTRY_X_OFFSET_12 : ENTRY_X_OFFSET_32;
        int boxW = addressBits <= 12 ? TABLE_WIDTH_12 : TABLE_WIDTH_32;

        // See if outside box
        if (x < boxX || x >= boxX + boxW || y <= ENTRY_Y_OFFSET || y >= ENTRY_Y_OFFSET + ROWS * ENTRY_HEIGHT) {
            return -1;
        }

        int column = (x - boxX) / (boxW / columns);
        int row = (y - ENTRY_Y_OFFSET) / ENTRY_HEIGHT;
        long address = (currentScroll / columns * columns) + (long) columns * row + column;
        return isValidAddress(address) ? address : getLastAddress();
    }

    public Bounds getBounds(long address, Bounds bounds) {
        int addressBits = getAddressBits();
        int boxX = bounds.getX() + (addressBits <= 12 ? ENTRY_X_OFFSET_12 : ENTRY_X_OFFSET_32);
        int boxW = addressBits <= 12 ? TABLE_WIDTH_12 : TABLE_WIDTH_32;
        if (address < 0) {
            int addressLength = (contents.getWidth() + 3) / 4;
            int width = ADDR_WIDTH_PER_CHAR * addressLength;
            return Bounds.create(boxX - width, bounds.getY() + ENTRY_Y_OFFSET, width, ENTRY_HEIGHT);
        } else {
            int boundsX = addressToX(bounds, address);
            int boundsY = addressToY(bounds, address);
            return Bounds.create(boundsX, boundsY, boxW / columns, ENTRY_HEIGHT);
        }
    }

    public void paint(Graphics g, int leftX, int topY) {
        int addressBits = getAddressBits();
        int dataBits = contents.getWidth();
        int boxX = leftX + (addressBits <= 12 ? ENTRY_X_OFFSET_12 : ENTRY_X_OFFSET_32);
        int boxY = topY + ENTRY_Y_OFFSET;
        int boxW = addressBits <= 12 ? TABLE_WIDTH_12 : TABLE_WIDTH_32;
        int boxH = ROWS * ENTRY_HEIGHT;

        GraphicsUtil.switchToWidth(g, 1);
        g.drawRect(boxX, boxY, boxW, boxH);
        int entryWidth = boxW / columns;
        for (int row = 0; row < ROWS; row++) {
            long address = (currentScroll / columns * columns) + (long) columns * row;
            int x = boxX;
            int y = boxY + ENTRY_HEIGHT * row;
            int yOffset = ENTRY_HEIGHT - 3;
            if (isValidAddress(address)) {
                g.setColor(Color.GRAY);
                GraphicsUtil.drawText(g, StringUtil.toHexString(getAddressBits(), (int) address), x - 2, y + yOffset,
                    GraphicsUtil.H_RIGHT, GraphicsUtil.V_BASELINE);
            }
            g.setColor(Color.BLACK);
            for (int column = 0; column < columns && isValidAddress(address); column++) {
                int value = contents.get(address);
                if (address == currentAddress) {
                    g.fillRect(x, y, entryWidth, ENTRY_HEIGHT);
                    g.setColor(Color.WHITE);
                    GraphicsUtil.drawText(g, StringUtil.toHexString(dataBits, value), x + entryWidth / 2, y + yOffset,
                        GraphicsUtil.H_CENTER, GraphicsUtil.V_BASELINE);
                    g.setColor(Color.BLACK);
                } else {
                    GraphicsUtil.drawText(g, StringUtil.toHexString(dataBits, value), x + entryWidth / 2, y + yOffset,
                        GraphicsUtil.H_CENTER, GraphicsUtil.V_BASELINE);
                }
                address++;
                x += entryWidth;
            }
        }
    }

    private int addressToX(Bounds bounds, long address) {
        int addressBits = getAddressBits();
        int boxX = bounds.getX() + (addressBits <= 12 ? ENTRY_X_OFFSET_12 : ENTRY_X_OFFSET_32);
        int boxW = addressBits <= 12 ? TABLE_WIDTH_12 : TABLE_WIDTH_32;

        long topRow = currentScroll / columns;
        long row = address / columns;
        if (row < topRow || row >= topRow + ROWS) {
            return -1;
        }
        int column = (int) (address - row * columns);
        if (column < 0 || column >= columns) {
            return -1;
        }
        return boxX + boxW * column / columns;
    }

    private int addressToY(Bounds bounds, long address) {
        long topRow = currentScroll / columns;
        long row = address / columns;
        if (row < topRow || row >= topRow + ROWS) {
            return -1;
        }
        return (int) (bounds.getY() + ENTRY_Y_OFFSET + ENTRY_HEIGHT * (row - topRow));
    }

    public void metaInfoChanged(HexModel source) {
        setBits(contents.getLogLength(), contents.getWidth());
    }

    public void bytesChanged(HexModel source, long start, long numBytes, int[] oldValues) {
    }
}
