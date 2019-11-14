/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.circuit;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

public class WireUtil {

    private WireUtil() {
    }

    static CircuitPoints computeCircuitPoints(Collection<? extends Component> components) {
        CircuitPoints points = new CircuitPoints();
        for (Component comp : components) {
            points.add(comp);
        }
        return points;
    }

    // Merge all parallel endpoint-to-endpoint wires within the given set.
    public static Collection<? extends Component> mergeExclusive(Collection<? extends Component> toMerge) {
        if (toMerge.size() <= 1) {
            return toMerge;
        }

        HashSet<Component> ret = new HashSet<>(toMerge);
        CircuitPoints points = computeCircuitPoints(toMerge);

        HashSet<Wire> wires = new HashSet<>();
        for (Location loc : points.getSplitLocations()) {
            Collection<? extends Component> at = points.getComponents(loc);
            if (at.size() == 2) {
                Iterator<? extends Component> atIt = at.iterator();
                Component o0 = atIt.next();
                Component o1 = atIt.next();
                if (o0 instanceof Wire && o1 instanceof Wire) {
                    Wire w0 = (Wire) o0;
                    Wire w1 = (Wire) o1;
                    if (w0.isXEqual == w1.isXEqual) {
                        wires.add(w0);
                        wires.add(w1);
                    }
                }
            }
        }
        points = null;

        ret.removeAll(wires);
        while (!wires.isEmpty()) {
            Iterator<Wire> it = wires.iterator();
            Wire w = it.next();
            Location e0 = w.start;
            Location e1 = w.end;
            it.remove();
            boolean found;
            do {
                found = false;
                for (it = wires.iterator(); it.hasNext(); ) {
                    Wire cand = it.next();
                    if (cand.start.equals(e1)) {
                        e1 = cand.end;
                        found = true;
                        it.remove();
                    } else if (cand.end.equals(e0)) {
                        e0 = cand.start;
                        found = true;
                        it.remove();
                    }
                }
            } while (found);
            ret.add(Wire.create(e0, e1));
        }

        return ret;
    }
}
