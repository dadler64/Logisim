/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.toolbar;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

public class ToolbarSeparator implements ToolbarItem {

    private final int size;

    public ToolbarSeparator(int size) {
        this.size = size;
    }

    public boolean isSelectable() {
        return false;
    }

    public void paintIcon(Component destination, Graphics graphics) {
        Dimension dimension = destination.getSize();
        graphics.setColor(Color.GRAY);
        int x;
        int y;
        int width = dimension.width;
        int height = dimension.height;
        if (height >= width) { // separator is a vertical line in horizontal toolbar
            height -= 8;
            y = 2;
            x = (width - 2) / 2;
            width = 2;
        } else { // separator is a horizontal line in vertical toolbar
            width -= 8;
            x = 2;
            y = (height - 2) / 2;
            height = 2;
        }
        graphics.fillRect(x, y, width, height);
    }

    public String getToolTip() {
        return null;
    }

    public Dimension getDimension(Object orientation) {
        return new Dimension(size, size);
    }
}
