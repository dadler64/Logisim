/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.undo;

import java.util.EventListener;

public interface UndoLogListener extends EventListener {

    void undoLogChanged(UndoLogEvent e);
}
