/* Copyright (c) 2011, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.generic;

import java.awt.Component;
import java.awt.Window;

public interface AttrTableModelRow {

    String getLabel();

    String getValue();

    void setValue(Object value) throws AttrTableSetException;

    boolean isValueEditable();

    Component getEditor(Window parent);
}
