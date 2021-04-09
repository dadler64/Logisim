/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.proj;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.util.Dag;

public class Dependencies {

    private final MyListener myListener = new MyListener();
    private final Dag depends = new Dag();

    Dependencies(LogisimFile file) {
        addDependencies(file);
    }

    public boolean canRemove(Circuit circuit) {
        return !depends.hasPredecessors(circuit);
    }

    public boolean canAdd(Circuit circuit, Circuit subcircuit) {
        return depends.canFollow(subcircuit, circuit);
    }

    private void addDependencies(LogisimFile file) {
        file.addLibraryListener(myListener);
        for (Circuit circuit : file.getCircuits()) {
            processCircuit(circuit);
        }
    }

    private void processCircuit(Circuit circuit) {
        circuit.addCircuitListener(myListener);
        for (Component component : circuit.getNonWires()) {
            if (component.getFactory() instanceof SubcircuitFactory) {
                SubcircuitFactory factory = (SubcircuitFactory) component.getFactory();
                depends.addEdge(circuit, factory.getSubcircuit());
            }
        }
    }

    private class MyListener
        implements LibraryListener, CircuitListener {

        public void libraryChanged(LibraryEvent e) {
            switch (e.getAction()) {
                case LibraryEvent.ADD_TOOL: {
                    if (e.getData() instanceof AddTool) {
                        ComponentFactory factory = ((AddTool) e.getData()).getFactory();
                        if (factory instanceof SubcircuitFactory) {
                            SubcircuitFactory subcircuitFactory = (SubcircuitFactory) factory;
                            processCircuit(subcircuitFactory.getSubcircuit());
                        }
                    }
                    break;
                }
                case LibraryEvent.REMOVE_TOOL: {
                    if (e.getData() instanceof AddTool) {
                        ComponentFactory factory = ((AddTool) e.getData()).getFactory();
                        if (factory instanceof SubcircuitFactory) {
                            SubcircuitFactory subcircuitFactory = (SubcircuitFactory) factory;
                            Circuit circuit = subcircuitFactory.getSubcircuit();
                            depends.removeNode(circuit);
                            circuit.removeCircuitListener(this);
                        }
                    }
                    break;
                }
            }
        }

        public void circuitChanged(CircuitEvent e) {
            Component component;
            switch (e.getAction()) {
                case CircuitEvent.ACTION_ADD:
                    component = (Component) e.getData();
                    if (component.getFactory() instanceof SubcircuitFactory) {
                        SubcircuitFactory factory = (SubcircuitFactory) component.getFactory();
                        depends.addEdge(e.getCircuit(), factory.getSubcircuit());
                    }
                    break;
                case CircuitEvent.ACTION_REMOVE:
                    component = (Component) e.getData();
                    if (component.getFactory() instanceof SubcircuitFactory) {
                        SubcircuitFactory factory = (SubcircuitFactory) component.getFactory();
                        boolean found = false;
                        for (Component comp : e.getCircuit().getNonWires()) {
                            if (comp.getFactory() == factory) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            depends.removeEdge(e.getCircuit(), factory.getSubcircuit());
                        }
                    }
                    break;
                case CircuitEvent.ACTION_CLEAR:
                    depends.removeNode(e.getCircuit());
                    break;
            }
        }
    }

}
