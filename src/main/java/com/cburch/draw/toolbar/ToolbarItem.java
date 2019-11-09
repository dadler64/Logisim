/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.toolbar;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

public interface ToolbarItem {

    boolean isSelectable();

    void paintIcon(Component destination, Graphics g);

    String getToolTip();

    Dimension getDimension(Object orientation);
}
