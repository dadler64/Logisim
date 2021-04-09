/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.canvas;

import com.cburch.draw.model.CanvasModelEvent;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.logisim.data.Location;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Selection {

    private static final String MOVING_HANDLE = "movingHandle";
    private static final String TRANSLATING = "translating";
    private static final String HIDDEN = "hidden";

    private final ArrayList<SelectionListener> listeners;
    private final HashSet<CanvasObject> selected;
    private final Set<CanvasObject> selectedView;
    private final HashMap<CanvasObject, String> suppressed;
    private final Set<CanvasObject> suppressedView;
    private Handle selectedHandle;
    private HandleGesture currentHandleGesture;
    private int moveDx;
    private int moveDy;

    protected Selection() {
        listeners = new ArrayList<>();
        selected = new HashSet<>();
        suppressed = new HashMap<>();
        selectedView = Collections.unmodifiableSet(selected);
        suppressedView = Collections.unmodifiableSet(suppressed.keySet());
    }

    public void addSelectionListener(SelectionListener listener) {
        listeners.add(listener);
    }

    public void removeSelectionListener(SelectionListener listener) {
        listeners.remove(listener);
    }

    private void fireChanged(int action, Collection<CanvasObject> affected) {
        SelectionEvent event = null;
        for (SelectionListener listener : listeners) {
            if (event == null) {
                event = new SelectionEvent(this, action, affected);
            }
            listener.selectionChanged(event);
        }
    }

    public boolean isEmpty() {
        return selected.isEmpty();
    }

    public boolean isSelected(CanvasObject shape) {
        return selected.contains(shape);
    }

    public Set<CanvasObject> getSelected() {
        return selectedView;
    }

    public void clearSelected() {
        if (!selected.isEmpty()) {
            ArrayList<CanvasObject> oldSelected = new ArrayList<>(selected);
            selected.clear();
            suppressed.clear();
            setHandleSelected(null);
            fireChanged(SelectionEvent.ACTION_REMOVED, oldSelected);
        }
    }

    public void setSelected(CanvasObject shape, boolean value) {
        setSelected(Collections.singleton(shape), value);
    }

    public void setSelected(Collection<CanvasObject> shapes, boolean value) {
        if (value) {
            ArrayList<CanvasObject> added = new ArrayList<>(shapes.size());
            for (CanvasObject shape : shapes) {
                if (selected.add(shape)) {
                    added.add(shape);
                }
            }
            if (!added.isEmpty()) {
                fireChanged(SelectionEvent.ACTION_ADDED, added);
            }
        } else {
            ArrayList<CanvasObject> removed = new ArrayList<>(shapes.size());
            for (CanvasObject shape : shapes) {
                if (selected.remove(shape)) {
                    suppressed.remove(shape);
                    Handle handle = selectedHandle;
                    if (handle != null && handle.getObject() == shape) {
                        setHandleSelected(null);
                    }
                    removed.add(shape);
                }
            }
            if (!removed.isEmpty()) {
                fireChanged(SelectionEvent.ACTION_REMOVED, removed);
            }
        }
    }

    public void toggleSelected(Collection<CanvasObject> shapes) {
        ArrayList<CanvasObject> added = new ArrayList<>(shapes.size());
        ArrayList<CanvasObject> removed = new ArrayList<>(shapes.size());
        for (CanvasObject shape : shapes) {
            if (selected.contains(shape)) {
                selected.remove(shape);
                suppressed.remove(shape);
                Handle handle = selectedHandle;
                if (handle != null && handle.getObject() == shape) {
                    setHandleSelected(null);
                }
                removed.add(shape);
            } else {
                selected.add(shape);
                added.add(shape);
            }
        }
        if (!removed.isEmpty()) {
            fireChanged(SelectionEvent.ACTION_REMOVED, removed);
        }
        if (!added.isEmpty()) {
            fireChanged(SelectionEvent.ACTION_ADDED, added);
        }
    }

    public Set<CanvasObject> getDrawsSuppressed() {
        return suppressedView;
    }

    public void clearDrawsSuppressed() {
        suppressed.clear();
        currentHandleGesture = null;
    }

    public Handle getSelectedHandle() {
        return selectedHandle;
    }

    public void setHandleSelected(Handle handle) {
        Handle selectedHandle = this.selectedHandle;
        boolean same = Objects.equals(selectedHandle, handle);
        if (!same) {
            this.selectedHandle = handle;
            currentHandleGesture = null;
            Collection<CanvasObject> objects;
            if (handle == null) {
                objects = Collections.emptySet();
            } else {
                objects = Collections.singleton(handle.getObject());
            }
            fireChanged(SelectionEvent.ACTION_HANDLE, objects);
        }
    }

    public void setHandleGesture(HandleGesture gesture) {
        HandleGesture currentHandleGesture = this.currentHandleGesture;
        if (currentHandleGesture != null) {
            suppressed.remove(currentHandleGesture.getHandle().getObject());
        }

        Handle handle = gesture.getHandle();
        suppressed.put(handle.getObject(), MOVING_HANDLE);
        this.currentHandleGesture = gesture;
    }

    public void setMovingShapes(Collection<? extends CanvasObject> shapes, int dx, int dy) {
        for (CanvasObject shape : shapes) {
            suppressed.put(shape, TRANSLATING);
        }
        moveDx = dx;
        moveDy = dy;
    }

    public void setHidden(Collection<? extends CanvasObject> shapes, boolean value) {
        if (value) {
            for (CanvasObject shape : shapes) {
                suppressed.put(shape, HIDDEN);
            }
        } else {
            suppressed.keySet().removeAll(shapes);
        }
    }

    public Location getMovingDelta() {
        return Location.create(moveDx, moveDy);
    }

    public void setMovingDelta(int dx, int dy) {
        moveDx = dx;
        moveDy = dy;
    }

    public void drawSuppressed(Graphics graphics, CanvasObject shape) {
        String state = suppressed.get(shape);
        if (state.equals(MOVING_HANDLE)) {
            shape.paint(graphics, currentHandleGesture);
        } else if (state.equals(TRANSLATING)) {
            graphics.translate(moveDx, moveDy);
            shape.paint(graphics, null);
        }
    }

    void modelChanged(CanvasModelEvent event) {
        int action = event.getAction();
        switch (action) {
            case CanvasModelEvent.ACTION_REMOVED:
                Collection<? extends CanvasObject> affected = event.getAffected();
                if (affected != null) {
                    selected.removeAll(affected);
                    suppressed.keySet().removeAll(affected);
                    Handle handle = selectedHandle;
                    if (handle != null && affected.contains(handle.getObject())) {
                        setHandleSelected(null);
                    }
                }
                break;
            case CanvasModelEvent.ACTION_HANDLE_DELETED:
                if (event.getHandle().equals(selectedHandle)) {
                    setHandleSelected(null);
                }
                break;
            case CanvasModelEvent.ACTION_HANDLE_MOVED:
                HandleGesture gesture = event.getHandleGesture();
                if (gesture.getHandle().equals(selectedHandle)) {
                    setHandleSelected(gesture.getResultingHandle());
                }
        }
    }
}
