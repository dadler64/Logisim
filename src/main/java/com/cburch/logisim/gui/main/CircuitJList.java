/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Project;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import javax.swing.JList;

class CircuitJList extends JList<Circuit> {

    public CircuitJList(Project project, boolean includeEmpty) {
        LogisimFile file = project.getLogisimFile();
        Circuit current = project.getCurrentCircuit();
        Vector<Circuit> options = new Vector<>();
        boolean currentFound = false;
        for (Circuit circuit : file.getCircuits()) {
            if (!includeEmpty || circuit.getBounds() != Bounds.EMPTY_BOUNDS) {
                if (circuit == current) {
                    currentFound = true;
                }
                options.add(circuit);
            }
        }

        setListData(options);
        if (currentFound) {
            setSelectedValue(current, true);
        }
        setVisibleRowCount(Math.min(6, options.size()));
    }

    public List<Circuit> getSelectedCircuits() {
        List<Circuit> selected = getSelectedValuesList();
        if (selected != null && selected.size() > 0) {
            ArrayList<Circuit> circuits = new ArrayList<>(selected.size());
            for (Circuit circuit : selected) {
                if (circuit != null) {
                    circuits.add(circuit);
                }
            }
            return circuits;
        } else {
            return Collections.emptyList();
        }
    }

}
