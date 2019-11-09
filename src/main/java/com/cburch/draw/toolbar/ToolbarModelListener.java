/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.toolbar;

public interface ToolbarModelListener {

    void toolbarContentsChanged(ToolbarModelEvent event);

    void toolbarAppearanceChanged(ToolbarModelEvent event);
}
