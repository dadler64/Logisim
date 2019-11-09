/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.circuit;

public interface SimulatorListener {

    void propagationCompleted(SimulatorEvent e);

    void tickCompleted(SimulatorEvent e);

    void simulatorStateChanged(SimulatorEvent e);
}
