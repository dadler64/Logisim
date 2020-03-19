/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.model;

import com.cburch.draw.canvas.Selection;
import com.cburch.draw.shapes.Text;
import com.cburch.logisim.data.Bounds;
import java.awt.Graphics;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface CanvasModel {

    // listener methods
    void addCanvasModelListener(CanvasModelListener listener);

    void removeCanvasModelListener(CanvasModelListener listener);

    // methods that don't change any data in the model
    void paint(Graphics graphics, Selection selection);

    List<CanvasObject> getObjectsFromTop();

    List<CanvasObject> getObjectsFromBottom();

    Collection<CanvasObject> getObjectsIn(Bounds bounds);

    Collection<CanvasObject> getObjectsOverlapping(CanvasObject shape);

    // methods that alter the model
    void addObjects(int index, Collection<? extends CanvasObject> shapes);

    void addObjects(Map<? extends CanvasObject, Integer> shapes);

    void removeObjects(Collection<? extends CanvasObject> shapes);

    void translateObjects(Collection<? extends CanvasObject> shapes, int dx, int dy);

    void reorderObjects(List<ReorderRequest> requests);

    Handle moveHandle(HandleGesture gesture);

    void insertHandle(Handle desired, Handle previous);

    Handle deleteHandle(Handle handle);

    void setAttributeValues(Map<AttributeMapKey, Object> values);

    void setText(Text text, String value);
}
