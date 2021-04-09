/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class DrawingOverlaps {

    private final Map<CanvasObject, List<CanvasObject>> map;
    private final Set<CanvasObject> untested;

    public DrawingOverlaps() {
        map = new HashMap<>();
        untested = new HashSet<>();
    }

    public Collection<CanvasObject> getObjectsOverlapping(CanvasObject o) {
        ensureUpdated();

        List<CanvasObject> objects = map.get(o);
        if (objects == null || objects.isEmpty()) {
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(objects);
        }
    }

    private void ensureUpdated() {
        for (CanvasObject object : untested) {
            ArrayList<CanvasObject> over = new ArrayList<>();
            for (CanvasObject otherObject : map.keySet()) {
                if (object != otherObject && object.overlaps(otherObject)) {
                    over.add(otherObject);
                    addOverlap(otherObject, object);
                }
            }
            map.put(object, over);
        }
        untested.clear();
    }

    private void addOverlap(CanvasObject a, CanvasObject b) {
        List<CanvasObject> aList = map.get(a);
        if (aList == null) {
            aList = new ArrayList<>();
            map.put(a, aList);
        }
        if (!aList.contains(b)) {
            aList.add(b);
        }
    }

    public void addShape(CanvasObject shape) {
        untested.add(shape);
    }

    public void removeShape(CanvasObject shape) {
        untested.remove(shape);
        List<CanvasObject> mapped = map.remove(shape);
        if (mapped != null) {
            for (CanvasObject object : mapped) {
                List<CanvasObject> reverse = map.get(object);
                if (reverse != null) {
                    reverse.remove(shape);
                }
            }
        }
    }

    public void invalidateShape(CanvasObject shape) {
        removeShape(shape);
        untested.add(shape);
    }

    public void invalidateShapes(Collection<? extends CanvasObject> shapes) {
        for (CanvasObject shape : shapes) {
            invalidateShape(shape);
        }
    }
}
