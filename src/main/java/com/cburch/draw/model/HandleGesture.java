/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.model;

import java.awt.event.InputEvent;

public class HandleGesture {

    private final Handle handle;
    private final int deltaX;
    private final int deltaY;
    private final int modifiersEx;
    private Handle resultingHandle;

    public HandleGesture(Handle handle, int deltaX, int deltaY, int modifiersEx) {
        this.handle = handle;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
        this.modifiersEx = modifiersEx;
    }

    @Override
    public String toString() {
        return "HandleGesture[" + deltaX + "," + deltaY + ":" + handle.getObject() + "/" + handle.getX() + "," + handle.getY()
            + "]";
    }

    public Handle getHandle() {
        return handle;
    }

    public int getDeltaX() {
        return deltaX;
    }

    public int getDeltaY() {
        return deltaY;
    }

    public int getModifiersEx() {
        return modifiersEx;
    }

    public boolean isShiftDown() {
        return (modifiersEx & InputEvent.SHIFT_DOWN_MASK) != 0;
    }

    public boolean isControlDown() {
        return (modifiersEx & InputEvent.CTRL_DOWN_MASK) != 0;
    }

    public boolean isAltDown() {
        return (modifiersEx & InputEvent.ALT_DOWN_MASK) != 0;
    }

    public Handle getResultingHandle() {
        return resultingHandle;
    }

    public void setResultingHandle(Handle value) {
        resultingHandle = value;
    }
}
