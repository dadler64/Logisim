/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.model;

import com.cburch.logisim.data.Location;

public class Handle {

    private final CanvasObject object;
    private final int x;
    private final int y;

    public Handle(CanvasObject object, int x, int y) {
        this.object = object;
        this.x = x;
        this.y = y;
    }

    public Handle(CanvasObject object, Location location) {
        this(object, location.getX(), location.getY());
    }

    public CanvasObject getObject() {
        return object;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Location getLocation() {
        return Location.create(x, y);
    }

    public boolean isAt(Location location) {
        return x == location.getX() && y == location.getY();
    }

    public boolean isAt(int x, int y) {
        return this.x == x && this.y == y;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Handle) {
            Handle that = (Handle) other;
            return this.object.equals(that.object) && this.x == that.x && this.y == that.y;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (this.object.hashCode() * 31 + x) * 31 + y;
    }
}
