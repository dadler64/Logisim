/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.tools;

import com.cburch.logisim.proj.Project;
import javax.swing.JPopupMenu;

public interface MenuExtender {

    void configureMenu(JPopupMenu menu, Project proj);
}
