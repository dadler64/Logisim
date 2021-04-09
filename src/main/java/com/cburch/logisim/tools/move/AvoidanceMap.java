/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.tools.move;

import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

class AvoidanceMap {

    private final HashMap<Location, String> avoid;

    private AvoidanceMap(HashMap<Location, String> map) {
        avoid = map;
    }

    static AvoidanceMap create(Collection<Component> elements, int dx, int dy) {
        AvoidanceMap ret = new AvoidanceMap(new HashMap<>());
        ret.markAll(elements, dx, dy);
        return ret;
    }

    public AvoidanceMap cloneMap() {
        return new AvoidanceMap(new HashMap<>(avoid));
    }

    public Object get(Location loc) {
        return avoid.get(loc);
    }

    public void markAll(Collection<Component> elements, int dx, int dy) {
        // first we go through the components, saying that we should not
        // intersect with any point that lies within a component
        for (Component element : elements) {
            if (element instanceof Wire) {
                markWire((Wire) element, dx, dy);
            } else {
                markComponent(element, dx, dy);
            }
        }
    }

    public void markComponent(Component component, int dx, int dy) {
        HashMap<Location, String> avoid = this.avoid;
        boolean translated = dx != 0 || dy != 0;
        Bounds bounds = component.getBounds();
        int x0 = bounds.getX() + dx;
        int y0 = bounds.getY() + dy;
        int x1 = x0 + bounds.getWidth();
        int y1 = y0 + bounds.getHeight();
        x0 += 9 - (x0 + 9) % 10;
        y0 += 9 - (y0 + 9) % 10;
        for (int x = x0; x <= x1; x += 10) {
            for (int y = y0; y <= y1; y += 10) {
                Location location = Location.create(x, y);
                // location is most likely in the component, so go ahead and put it into the map as if it is - and in the
                // rare event that location isn't in the component, we can remove it.
                String previous = avoid.put(location, Connector.ALLOW_NEITHER);
                if (!previous.equals(Connector.ALLOW_NEITHER)) {
                    Location baseLoc = translated ? location.translate(-dx, -dy) : location;
                    if (!component.contains(baseLoc)) {
                        if (previous == null) {
                            avoid.remove(location);
                        } else {
                            avoid.put(location, previous);
                        }
                    }
                }
            }
        }
    }

    public void markWire(Wire wire, int dx, int dy) {
        HashMap<Location, String> avoid = this.avoid;
        boolean translated = dx != 0 || dy != 0;
        Location location0 = wire.getEnd0();
        Location location1 = wire.getEnd1();
        if (translated) {
            location0 = location0.translate(dx, dy);
            location1 = location1.translate(dx, dy);
        }
        avoid.put(location0, Connector.ALLOW_NEITHER);
        avoid.put(location1, Connector.ALLOW_NEITHER);
        int x0 = location0.getX();
        int y0 = location0.getY();
        int x1 = location1.getX();
        int y1 = location1.getY();
        if (x0 == x1) { // vertical wire
            for (Location location : Wire.create(location0, location1)) {
                Object previous = avoid.put(location, Connector.ALLOW_HORIZONTAL);
                if (previous == Connector.ALLOW_NEITHER || previous == Connector.ALLOW_VERTICAL) {
                    avoid.put(location, Connector.ALLOW_NEITHER);
                }
            }
        } else if (y0 == y1) { // horizontal wire
            for (Location location : Wire.create(location0, location1)) {
                Object prev = avoid.put(location, Connector.ALLOW_VERTICAL);
                if (prev == Connector.ALLOW_NEITHER || prev == Connector.ALLOW_HORIZONTAL) {
                    avoid.put(location, Connector.ALLOW_NEITHER);
                }
            }
        } else { // diagonal - shouldn't happen
            throw new RuntimeException("diagonal wires not supported");
        }
    }

    public void unmarkLocation(Location location) {
        avoid.remove(location);
    }

    public void unmarkWire(Wire wire, Location deletedEnd, Set<Location> unmarkable) {
        Location location0 = wire.getEnd0();
        Location location1 = wire.getEnd1();
        if (unmarkable == null || unmarkable.contains(deletedEnd)) {
            avoid.remove(deletedEnd);
        }
        int x0 = location0.getX();
        int y0 = location0.getY();
        int x1 = location1.getX();
        int y1 = location1.getY();
        if (x0 == x1) { // vertical wire
            for (Location location : wire) {
                if (unmarkable == null || unmarkable.contains(deletedEnd)) {
                    Object previous = avoid.remove(location);
                    if (previous != Connector.ALLOW_HORIZONTAL && previous != null) {
                        avoid.put(location, Connector.ALLOW_VERTICAL);
                    }
                }
            }
        } else if (y0 == y1) { // horizontal wire
            for (Location location : wire) {
                if (unmarkable == null || unmarkable.contains(deletedEnd)) {
                    Object previous = avoid.remove(location);
                    if (previous != Connector.ALLOW_VERTICAL && previous != null) {
                        avoid.put(location, Connector.ALLOW_HORIZONTAL);
                    }
                }
            }
        } else { // diagonal - shouldn't happen
            throw new RuntimeException("diagonal wires not supported");
        }
    }

    public void print(PrintStream stream) {
        ArrayList<Location> list = new ArrayList<>(avoid.keySet());
        Collections.sort(list);
        for (Location location : list) {
            stream.println(location + ": " + avoid.get(location));
        }
    }
}
