/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.generic;

import java.beans.PropertyChangeListener;

public interface ZoomModel {

    String ZOOM = "zoom";
    String SHOW_GRID = "grid";

    void addPropertyChangeListener(String prop, PropertyChangeListener l);

    void removePropertyChangeListener(String prop, PropertyChangeListener l);

    boolean getShowGrid();

    void setShowGrid(boolean value);

    double getZoomFactor();

    void setZoomFactor(double value);

    double[] getZoomOptions();
}
