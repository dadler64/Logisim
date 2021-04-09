/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.model;

import com.cburch.draw.canvas.Selection;
import com.cburch.draw.shapes.Text;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.util.EventSourceWeakSupport;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Drawing implements CanvasModel {

    private final EventSourceWeakSupport<CanvasModelListener> listeners;
    private final ArrayList<CanvasObject> canvasObjects;
    private final DrawingOverlaps overlaps;

    public Drawing() {
        listeners = new EventSourceWeakSupport<>();
        canvasObjects = new ArrayList<>();
        overlaps = new DrawingOverlaps();
    }

    public void addCanvasModelListener(CanvasModelListener listener) {
        listeners.add(listener);
    }

    public void removeCanvasModelListener(CanvasModelListener listener) {
        listeners.remove(listener);
    }

    private boolean isChangeAllowed(CanvasModelEvent event) {
        return true;
    }

    private void fireChanged(CanvasModelEvent event) {
        for (CanvasModelListener listener : listeners) {
            listener.modelChanged(event);
        }
    }

    public void paint(Graphics graphics, Selection selection) {
        Set<CanvasObject> suppressed = selection.getDrawsSuppressed();
        for (CanvasObject shape : getObjectsFromBottom()) {
            Graphics graphicsClone = graphics.create();
            if (suppressed.contains(shape)) {
                selection.drawSuppressed(graphicsClone, shape);
            } else {
                shape.paint(graphicsClone, null);
            }
            graphicsClone.dispose();
        }
    }

    public List<CanvasObject> getObjectsFromTop() {
        ArrayList<CanvasObject> objects = new ArrayList<>(getObjectsFromBottom());
        Collections.reverse(objects);
        return objects;
    }

    public List<CanvasObject> getObjectsFromBottom() {
        return Collections.unmodifiableList(canvasObjects);
    }

    public Collection<CanvasObject> getObjectsIn(Bounds bounds) {
        ArrayList<CanvasObject> shapes = null;
        for (CanvasObject shape : getObjectsFromBottom()) {
            if (bounds.contains(shape.getBounds())) {
                if (shapes == null) {
                    shapes = new ArrayList<>();
                }
                shapes.add(shape);
            }
        }
        if (shapes == null) {
            return Collections.emptyList();
        } else {
            return shapes;
        }
    }

    public Collection<CanvasObject> getObjectsOverlapping(CanvasObject shape) {
        return overlaps.getObjectsOverlapping(shape);
    }

    public void addObjects(int index, Collection<? extends CanvasObject> shapes) {
        LinkedHashMap<CanvasObject, Integer> indexes;
        indexes = new LinkedHashMap<>();
        int i = index;
        for (CanvasObject shape : shapes) {
            indexes.put(shape, i);
            i++;
        }
        addObjectsHelp(indexes);
    }

    public void addObjects(Map<? extends CanvasObject, Integer> shapes) {
        addObjectsHelp(shapes);
    }

    private void addObjectsHelp(Map<? extends CanvasObject, Integer> shapes) {
        // this is separate method so that subclass can call super.add to either
        // of the add methods, and it won't get redirected into the subclass
        // in calling the other add method
        CanvasModelEvent event = CanvasModelEvent.forAdd(this, shapes.keySet());
        if (!shapes.isEmpty() && isChangeAllowed(event)) {
            for (Map.Entry<? extends CanvasObject, Integer> entry : shapes.entrySet()) {
                CanvasObject shape = entry.getKey();
                int index = entry.getValue();
                canvasObjects.add(index, shape);
                overlaps.addShape(shape);
            }
            fireChanged(event);
        }
    }

    public void removeObjects(Collection<? extends CanvasObject> shapes) {
        List<CanvasObject> found = restrict(shapes);
        CanvasModelEvent event = CanvasModelEvent.forRemove(this, found);
        if (!found.isEmpty() && isChangeAllowed(event)) {
            for (CanvasObject shape : found) {
                canvasObjects.remove(shape);
                overlaps.removeShape(shape);
            }
            fireChanged(event);
        }
    }

    public void translateObjects(Collection<? extends CanvasObject> shapes, int deltaX, int deltaY) {
        List<CanvasObject> found = restrict(shapes);
        CanvasModelEvent event = CanvasModelEvent.forTranslate(this, found, deltaX, deltaY);
        if (!found.isEmpty() && (deltaX != 0 || deltaY != 0) && isChangeAllowed(event)) {
            for (CanvasObject shape : shapes) {
                shape.translate(deltaX, deltaY);
                overlaps.invalidateShape(shape);
            }
            fireChanged(event);
        }
    }

    public void reorderObjects(List<ReorderRequest> requests) {
        boolean hasEffect = false;
        for (ReorderRequest request : requests) {
            if (request.getFromIndex() != request.getToIndex()) {
                hasEffect = true;
                break;
            }
        }
        CanvasModelEvent event = CanvasModelEvent.forReorder(this, requests);
        if (hasEffect && isChangeAllowed(event)) {
            for (ReorderRequest request : requests) {
                if (canvasObjects.get(request.getFromIndex()) != request.getObject()) {
                    throw new IllegalArgumentException("object not present at indicated index: " + request.getFromIndex());
                }
                canvasObjects.remove(request.getFromIndex());
                canvasObjects.add(request.getToIndex(), request.getObject());
            }
            fireChanged(event);
        }
    }

    public Handle moveHandle(HandleGesture gesture) {
        CanvasModelEvent event = CanvasModelEvent.forMoveHandle(this, gesture);
        CanvasObject object = gesture.getHandle().getObject();
        if (canvasObjects.contains(object) && (gesture.getDeltaX() != 0 || gesture.getDeltaY() != 0) && isChangeAllowed(
            event)) {
            Handle moveHandle = object.moveHandle(gesture);
            gesture.setResultingHandle(moveHandle);
            overlaps.invalidateShape(object);
            fireChanged(event);
            return moveHandle;
        } else {
            return null;
        }
    }

    public void insertHandle(Handle desired, Handle previous) {
        CanvasObject object = desired.getObject();
        CanvasModelEvent event = CanvasModelEvent.forInsertHandle(this, desired);
        if (isChangeAllowed(event)) {
            object.insertHandle(desired, previous);
            overlaps.invalidateShape(object);
            fireChanged(event);
        }
    }

    public Handle deleteHandle(Handle handle) {
        CanvasModelEvent event = CanvasModelEvent.forDeleteHandle(this, handle);
        if (isChangeAllowed(event)) {
            CanvasObject object = handle.getObject();
            Handle deleteHandle = object.deleteHandle(handle);
            overlaps.invalidateShape(object);
            fireChanged(event);
            return deleteHandle;
        } else {
            return null;
        }
    }

    public void setAttributeValues(Map<AttributeMapKey, Object> values) {
        HashMap<AttributeMapKey, Object> oldValues = new HashMap<>();
        for (AttributeMapKey key : values.keySet()) {
            @SuppressWarnings("unchecked")
            Attribute<Object> attribute = (Attribute<Object>) key.getAttribute();
            Object oldValue = key.getObject().getValue(attribute);
            oldValues.put(key, oldValue);
        }
        CanvasModelEvent event = CanvasModelEvent.forChangeAttributes(this, oldValues, values);
        if (isChangeAllowed(event)) {
            for (Map.Entry<AttributeMapKey, Object> entry : values.entrySet()) {
                AttributeMapKey key = entry.getKey();
                CanvasObject shape = key.getObject();
                @SuppressWarnings("unchecked")
                Attribute<Object> attr = (Attribute<Object>) key.getAttribute();
                shape.setValue(attr, entry.getValue());
                overlaps.invalidateShape(shape);
            }
            fireChanged(event);
        }
    }

    public void setText(Text text, String value) {
        String oldValue = text.getText();
        CanvasModelEvent event = CanvasModelEvent.forChangeText(this, text, oldValue, value);
        if (canvasObjects.contains(text) && !oldValue.equals(value) && isChangeAllowed(event)) {
            text.setText(value);
            overlaps.invalidateShape(text);
            fireChanged(event);
        }
    }

    private ArrayList<CanvasObject> restrict(Collection<? extends CanvasObject> shapes) {
        ArrayList<CanvasObject> restrictList = new ArrayList<>(shapes.size());
        for (CanvasObject shape : shapes) {
            if (canvasObjects.contains(shape)) {
                restrictList.add(shape);
            }
        }
        return restrictList;
    }
}
