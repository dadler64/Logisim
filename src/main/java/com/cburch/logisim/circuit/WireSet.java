/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.circuit;

import com.cburch.logisim.data.Location;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class WireSet {

    private static final Set<Wire> NULL_WIRES = Collections.emptySet();
    public static final WireSet EMPTY = new WireSet(NULL_WIRES);

    private Set<Wire> wires;
    private Set<Location> points;

    WireSet(Set<Wire> wires) {
        if (wires.isEmpty()) {
            this.wires = NULL_WIRES;
            points = Collections.emptySet();
        } else {
            this.wires = wires;
            points = new HashSet<>();
            for (Wire w : wires) {
                points.add(w.start);
                points.add(w.end);
            }
        }
    }

    public boolean containsWire(Wire w) {
        return wires.contains(w);
    }

    public boolean containsLocation(Location loc) {
        return points.contains(loc);
    }
}
