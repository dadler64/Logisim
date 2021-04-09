/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.comp;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;

public class EndData {

    public static final int INPUT_ONLY = 1;
    public static final int OUTPUT_ONLY = 2;
    public static final int INPUT_OUTPUT = 3;

    private final Location loc;
    private final BitWidth width;
    private final int io;
    private final boolean exclusive;

    public EndData(Location loc, BitWidth width, int type, boolean exclusive) {
        this.loc = loc;
        this.width = width;
        this.io = type;
        this.exclusive = exclusive;
    }

    public EndData(Location loc, BitWidth width, int type) {
        this(loc, width, type, type == OUTPUT_ONLY);
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public boolean isInput() {
        return (io & INPUT_ONLY) != 0;
    }

    public boolean isOutput() {
        return (io & OUTPUT_ONLY) != 0;
    }

    public Location getLocation() {
        return loc;
    }

    public BitWidth getWidth() {
        return width;
    }

    public int getType() {
        return io;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof EndData)) {
            return false;
        }
        if (other == this) {
            return true;
        }
        EndData o = (EndData) other;
        return o.loc.equals(this.loc) && o.width.equals(this.width)
            && o.io == this.io && o.exclusive == this.exclusive;
    }
}
