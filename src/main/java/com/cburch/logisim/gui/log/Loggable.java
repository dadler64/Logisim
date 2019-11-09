/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.log;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Value;

public interface Loggable {

    Object[] getLogOptions(CircuitState state);

    String getLogName(Object option);

    Value getLogValue(CircuitState state, Object option);
}
