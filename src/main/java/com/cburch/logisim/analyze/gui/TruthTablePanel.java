/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.analyze.gui;

import com.cburch.logisim.analyze.model.Entry;
import com.cburch.logisim.analyze.model.TruthTable;
import java.awt.Color;
import java.awt.event.MouseEvent;

interface TruthTablePanel {

    Color ERROR_COLOR = new Color(255, 128, 128);

    TruthTable getTruthTable();

    int getOutputColumn(MouseEvent event);

    int getRow(MouseEvent event);

    void setEntryProvisional(int row, int col, Entry value);
}
