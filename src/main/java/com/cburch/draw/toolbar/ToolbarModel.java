/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.toolbar;

import java.util.List;

public interface ToolbarModel {

    void addToolbarModelListener(ToolbarModelListener listener);

    void removeToolbarModelListener(ToolbarModelListener listener);

    List<ToolbarItem> getItems();

    boolean isSelected(ToolbarItem item);

    void itemSelected(ToolbarItem item);
}
