/* Copyright (c) 2011, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.appear;

import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

class ClipboardContents {

    static final ClipboardContents EMPTY
            = new ClipboardContents(Collections.emptySet(), null, null);

    private Collection<CanvasObject> onClipboard;
    private Location anchorLocation;
    private Direction anchorFacing;

    public ClipboardContents(Collection<CanvasObject> onClipboard,
            Location anchorLocation, Direction anchorFacing) {
        this.onClipboard = Collections.unmodifiableList(new ArrayList<>(onClipboard));
        this.anchorLocation = anchorLocation;
        this.anchorFacing = anchorFacing;
    }

    public Collection<CanvasObject> getElements() {
        return onClipboard;
    }

    public Location getAnchorLocation() {
        return anchorLocation;
    }

    public Direction getAnchorFacing() {
        return anchorFacing;
    }
}
