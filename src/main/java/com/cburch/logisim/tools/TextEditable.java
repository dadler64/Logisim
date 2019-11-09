/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.tools;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.proj.Action;

public interface TextEditable {

    Caret getTextCaret(ComponentUserEvent event);

    Action getCommitAction(Circuit circuit, String oldText, String newText);
}
