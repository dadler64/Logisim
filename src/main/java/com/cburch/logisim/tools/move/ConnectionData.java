/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.tools.move;

import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import java.util.List;

class ConnectionData {

    private final Location location;
    private final Direction direction;
    /**
     * The list of wires leading up to this point - we may well want to
     * truncate this path somewhat.
     */
    private final List<Wire> wirePath;
    private final Location wirePathStart;

    public ConnectionData(Location location, Direction direction, List<Wire> wirePath,
        Location wirePathStart) {
        this.location = location;
        this.direction = direction;
        this.wirePath = wirePath;
        this.wirePathStart = wirePathStart;
    }

    public Location getLocation() {
        return location;
    }

    public Direction getDirection() {
        return direction;
    }

    public List<Wire> getWirePath() {
        return wirePath;
    }

    public Location getWirePathStart() {
        return wirePathStart;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ConnectionData) {
            ConnectionData data = (ConnectionData) other;
            return this.location.equals(data.location) && this.direction.equals(data.direction);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return location.hashCode() * 31 + (direction == null ? 0 : direction.hashCode());
    }
}
