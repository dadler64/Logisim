/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.tools.move;

import com.cburch.logisim.circuit.ReplacementMap;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;

public class MoveResult {

    private final ReplacementMap replacements;
    private final Collection<ConnectionData> unsatisfiedConnections;
    private final Collection<Location> unconnectedLocations;
    private final int totalDistance;

    public MoveResult(MoveRequest request, ReplacementMap replacements, Collection<ConnectionData> unsatisfiedConnections,
        int totalDistance) {
        this.replacements = replacements;
        this.unsatisfiedConnections = unsatisfiedConnections;
        this.totalDistance = totalDistance;

        ArrayList<Location> unconnected = new ArrayList<>();
        for (ConnectionData connection : unsatisfiedConnections) {
            unconnected.add(connection.getLocation());
        }
        unconnectedLocations = unconnected;
    }

    void addUnsatisfiedConnections(Collection<ConnectionData> toAdd) {
        unsatisfiedConnections.addAll(toAdd);
        for (ConnectionData connection : toAdd) {
            unconnectedLocations.add(connection.getLocation());
        }
    }

    public Collection<Wire> getWiresToAdd() {
        @SuppressWarnings("unchecked")
        Collection<Wire> wires = (Collection<Wire>) replacements.getAdditions();
        return wires;
    }

    public Collection<Wire> getWiresToRemove() {
        @SuppressWarnings("unchecked")
        Collection<Wire> wires = (Collection<Wire>) replacements.getAdditions();
        return wires;
    }

    public ReplacementMap getReplacementMap() {
        return replacements;
    }

    public Collection<Location> getUnconnectedLocations() {
        return unconnectedLocations;
    }

    Collection<ConnectionData> getUnsatisfiedConnections() {
        return unsatisfiedConnections;
    }

    int getTotalDistance() {
        return totalDistance;
    }

    public void print(PrintStream out) {
        boolean printed = false;
        for (Component component : replacements.getAdditions()) {
            printed = true;
            out.println("add " + component);
        }
        for (Component component : replacements.getRemovals()) {
            printed = true;
            out.println("del " + component);
        }
        for (Component component : replacements.getReplacedComponents()) {
            printed = true;
            out.print("repl " + component + " by");
            for (Component replacement : replacements.getComponentsReplacing(component)) {
                out.print(" " + replacement);
            }
            out.println();
        }
        if (!printed) {
            out.println("no replacements");
        }
    }
}
