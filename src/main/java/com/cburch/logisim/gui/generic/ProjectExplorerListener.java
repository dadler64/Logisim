/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.generic;

import com.cburch.logisim.tools.AddTool;
import javax.swing.JPopupMenu;

public interface ProjectExplorerListener {

    void selectionChanged(ProjectExplorerEvent event);

    void doubleClicked(ProjectExplorerEvent event);

    void moveRequested(ProjectExplorerEvent event, AddTool dragged, AddTool target);

    void deleteRequested(ProjectExplorerEvent event);

    JPopupMenu menuRequested(ProjectExplorerEvent event);
}