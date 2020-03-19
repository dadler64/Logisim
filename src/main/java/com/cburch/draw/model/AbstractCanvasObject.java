/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.model;

import com.cburch.draw.shapes.DrawAttr;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.EventSourceWeakSupport;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.util.List;
import java.util.Random;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class AbstractCanvasObject
        implements AttributeSet, CanvasObject, Cloneable {

    private static final int OVERLAP_TRIES = 50;
    private static final int GENERATE_RANDOM_TRIES = 20;

    private EventSourceWeakSupport<AttributeListener> listeners;

    public AbstractCanvasObject() {
        listeners = new EventSourceWeakSupport<>();
    }

    public AttributeSet getAttributeSet() {
        return this;
    }

    public abstract String getDisplayName();

    public abstract Element toSvgElement(Document document);

    public abstract boolean matches(CanvasObject object);

    public abstract int matchesHashCode();

    public abstract Bounds getBounds();

    public abstract boolean contains(Location location, boolean assumeFilled);

    public abstract void translate(int deltaX, int deltaY);

    public abstract List<Handle> getHandles(HandleGesture gesture);

    protected abstract void updateValue(Attribute<?> attribute, Object value);

    public abstract void paint(Graphics graphics, HandleGesture gesture);

    public boolean canRemove() {
        return true;
    }

    public boolean canMoveHandle(Handle handle) {
        return false;
    }

    public Handle canInsertHandle(Location desired) {
        return null;
    }

    public Handle canDeleteHandle(Location location) {
        return null;
    }

    public Handle moveHandle(HandleGesture gesture) {
        throw new UnsupportedOperationException("moveHandle");
    }

    public void insertHandle(Handle desired, Handle previous) {
        throw new UnsupportedOperationException("insertHandle");
    }

    public Handle deleteHandle(Handle handle) {
        throw new UnsupportedOperationException("deleteHandle");
    }

    public boolean overlaps(CanvasObject object) {
        Bounds a = this.getBounds();
        Bounds b = object.getBounds();
        Bounds c = a.intersect(b);
        Random random = new Random();
        if (c.getWidth() == 0 || c.getHeight() == 0) {
            return false;
        } else if (object instanceof AbstractCanvasObject) {
            AbstractCanvasObject canvasObject = (AbstractCanvasObject) object;
            for (int i = 0; i < OVERLAP_TRIES; i++) {
                if (i % 2 == 0) {
                    Location location = this.getRandomPoint(c, random);
                    if (location != null && canvasObject.contains(location, false)) {
                        return true;
                    }
                } else {
                    Location location = canvasObject.getRandomPoint(c, random);
                    if (location != null && this.contains(location, false)) {
                        return true;
                    }
                }
            }
            return false;
        } else {
            for (int i = 0; i < OVERLAP_TRIES; i++) {
                Location location = this.getRandomPoint(c, random);
                if (location != null && object.contains(location, false)) {
                    return true;
                }
            }
            return false;
        }
    }

    protected Location getRandomPoint(Bounds bounds, Random random) {
        int x = bounds.getX();
        int y = bounds.getY();
        int width = bounds.getWidth();
        int height = bounds.getHeight();
        for (int i = 0; i < GENERATE_RANDOM_TRIES; i++) {
            Location location = Location.create(x + random.nextInt(width), y + random.nextInt(height));
            if (contains(location, false)) {
                return location;
            }
        }
        return null;
    }

    // methods required by AttributeSet interface
    public abstract List<Attribute<?>> getAttributes();

    public abstract <V> V getValue(Attribute<V> attribute);

    public void addAttributeListener(AttributeListener l) {
        listeners.add(l);
    }

    public void removeAttributeListener(AttributeListener l) {
        listeners.remove(l);
    }

    @Override
    public CanvasObject clone() {
        try {
            AbstractCanvasObject object = (AbstractCanvasObject) super.clone();
            object.listeners = new EventSourceWeakSupport<>();
            return object;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean containsAttribute(Attribute<?> attr) {
        return getAttributes().contains(attr);
    }

    public Attribute<?> getAttribute(String name) {
        for (Attribute<?> attr : getAttributes()) {
            if (attr.getName().equals(name)) {
                return attr;
            }
        }
        return null;
    }

    public boolean isReadOnly(Attribute<?> attr) {
        return false;
    }

    public void setReadOnly(Attribute<?> attr, boolean value) {
        throw new UnsupportedOperationException("setReadOnly");
    }

    public boolean isToSave(Attribute<?> attr) {
        return true;
    }

    public final <V> void setValue(Attribute<V> attribute, V value) {
        Object old = getValue(attribute);
        boolean same = old == null ? value == null : old.equals(value);
        if (!same) {
            updateValue(attribute, value);
            AttributeEvent e = new AttributeEvent(this, attribute, value);
            for (AttributeListener listener : listeners) {
                listener.attributeValueChanged(e);
            }
        }
    }

    protected void fireAttributeListChanged() {
        AttributeEvent e = new AttributeEvent(this);
        for (AttributeListener listener : listeners) {
            listener.attributeListChanged(e);
        }
    }

    protected boolean setForStroke(Graphics g) {
        List<Attribute<?>> attrs = getAttributes();
        if (attrs.contains(DrawAttr.PAINT_TYPE)) {
            Object value = getValue(DrawAttr.PAINT_TYPE);
            if (value == DrawAttr.PAINT_FILL) {
                return false;
            }
        }

        Integer width = getValue(DrawAttr.STROKE_WIDTH);
        if (width != null && width > 0) {
            Color color = getValue(DrawAttr.STROKE_COLOR);
            if (color != null && color.getAlpha() == 0) {
                return false;
            } else {
                GraphicsUtil.switchToWidth(g, width);
                if (color != null) {
                    g.setColor(color);
                }
                return true;
            }
        } else {
            return false;
        }
    }

    protected boolean setForFill(Graphics g) {
        List<Attribute<?>> attrs = getAttributes();
        if (attrs.contains(DrawAttr.PAINT_TYPE)) {
            Object value = getValue(DrawAttr.PAINT_TYPE);
            if (value == DrawAttr.PAINT_STROKE) {
                return false;
            }
        }

        Color color = getValue(DrawAttr.FILL_COLOR);
        if (color != null && color.getAlpha() == 0) {
            return false;
        } else {
            if (color != null) {
                g.setColor(color);
            }
            return true;
        }
    }

}
