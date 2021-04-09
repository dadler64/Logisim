/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.circuit;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentListener;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.tools.CustomHandles;
import com.cburch.logisim.util.Cache;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Graphics;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public final class Wire implements Component, AttributeSet, CustomHandles, Iterable<Location> {

    /**
     * Stroke width when drawing wires.
     */
    public static final int WIDTH = 3;

    public static final AttributeOption VALUE_HORIZONTAL
        = new AttributeOption("horz", Strings.getter("wireDirectionHorzOption"));
    public static final AttributeOption VALUE_VERTICAL
        = new AttributeOption("vert", Strings.getter("wireDirectionVertOption"));
    public static final Attribute<AttributeOption> DIRECTION_ATTRIBUTE
        = Attributes.forOption("direction", Strings.getter("wireDirectionAttr"),
        new AttributeOption[]{VALUE_HORIZONTAL, VALUE_VERTICAL});
    public static final Attribute<Integer> LENGTH_ATTRIBUTE
        = Attributes.forInteger("length", Strings.getter("wireLengthAttr"));

    private static final List<Attribute<?>> ATTRIBUTES = Arrays.asList(DIRECTION_ATTRIBUTE, LENGTH_ATTRIBUTE);
    private static final Cache cache = new Cache();
    final Location start; // e0
    final Location end; // e1
    final boolean isXEqual;

    private Wire(Location start, Location end) {
        this.isXEqual = start.getX() == end.getX();
        if (isXEqual) {
            if (start.getY() > end.getY()) {
                this.start = end;
                this.end = start;
            } else {
                this.start = start;
                this.end = end;
            }
        } else {
            if (start.getX() > end.getX()) {
                this.start = end;
                this.end = start;
            } else {
                this.start = start;
                this.end = end;
            }
        }
    }

    public static Wire create(Location start, Location end) {
        return (Wire) cache.get(new Wire(start, end));
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Wire)) {
            return false;
        }
        Wire wire = (Wire) other;
        return wire.start.equals(this.start) && wire.end.equals(this.end);
    }

    @Override
    public int hashCode() {
        return start.hashCode() * 31 + end.hashCode();
    }

    public int getLength() {
        return (end.getY() - start.getY()) + (end.getX() - start.getX());
    }

    @Override
    public String toString() {
        return "Wire[" + start + "-" + end + "]";
    }

    //
    // Component methods
    //
    // (Wire never issues ComponentEvents, so we don't need to track listeners)
    public void addComponentListener(ComponentListener e) {
    }

    public void removeComponentListener(ComponentListener e) {
    }

    public ComponentFactory getFactory() {
        return WireFactory.instance;
    }

    public AttributeSet getAttributeSet() {
        return this;
    }

    // location/extent methods
    public Location getLocation() {
        return start;
    }

    public Bounds getBounds() {
        int x0 = start.getX();
        int y0 = start.getY();
        return Bounds.create(x0 - 2, y0 - 2,
            end.getX() - x0 + 5, end.getY() - y0 + 5);
    }

    public Bounds getBounds(Graphics g) {
        return getBounds();
    }

    public boolean contains(Location q) {
        int qx = q.getX();
        int qy = q.getY();
        if (isXEqual) {
            int wx = start.getX();
            return qx >= wx - 2 && qx <= wx + 2
                && start.getY() <= qy && qy <= end.getY();
        } else {
            int wy = start.getY();
            return qy >= wy - 2 && qy <= wy + 2
                && start.getX() <= qx && qx <= end.getX();
        }
    }

    public boolean contains(Location pt, Graphics g) {
        return contains(pt);
    }

    //
    // propagation methods
    //
    public List<EndData> getEnds() {
        return new EndList();
    }

    public EndData getEnd(int index) {
        Location loc = getEndLocation(index);
        return new EndData(loc, BitWidth.UNKNOWN,
            EndData.INPUT_OUTPUT);
    }

    public boolean endsAt(Location pt) {
        return start.equals(pt) || end.equals(pt);
    }

    public void propagate(CircuitState state) {
        // Normally this is handled by CircuitWires, and so it won't get
        // called. The exception is when a wire is added or removed
        state.markPointAsDirty(start);
        state.markPointAsDirty(end);
    }

    //
    // user interface methods
    //
    public void expose(ComponentDrawContext context) {
        java.awt.Component dest = context.getDestination();
        int x0 = start.getX();
        int y0 = start.getY();
        dest.repaint(x0 - 5, y0 - 5,
            end.getX() - x0 + 10, end.getY() - y0 + 10);
    }

    public void draw(ComponentDrawContext context) {
        CircuitState state = context.getCircuitState();
        Graphics g = context.getGraphics();

        GraphicsUtil.switchToWidth(g, WIDTH);
        g.setColor(state.getValue(start).getColor());
        g.drawLine(start.getX(), start.getY(),
            end.getX(), end.getY());
    }

    public Object getFeature(Object key) {
        if (key == CustomHandles.class) {
            return this;
        }
        return null;
    }

    //
    // AttributeSet methods
    //
    // It makes some sense for a wire to be its own attribute, since
    // after all it is immutable.
    //
    @Override
    public Object clone() {
        return this;
    }

    public void addAttributeListener(AttributeListener l) {
    }

    public void removeAttributeListener(AttributeListener l) {
    }

    public List<Attribute<?>> getAttributes() {
        return ATTRIBUTES;
    }

    public boolean containsAttribute(Attribute<?> attr) {
        return ATTRIBUTES.contains(attr);
    }

    public Attribute<?> getAttribute(String name) {
        for (Attribute<?> attr : ATTRIBUTES) {
            if (name.equals(attr.getName())) {
                return attr;
            }
        }
        return null;
    }

    public boolean isReadOnly(Attribute<?> attr) {
        return true;
    }

    public void setReadOnly(Attribute<?> attr, boolean value) {
        throw new UnsupportedOperationException();
    }

    public boolean isToSave(Attribute<?> attr) {
        return false;
    }

    @SuppressWarnings("unchecked")
    public <V> V getValue(Attribute<V> attr) {
        if (attr == DIRECTION_ATTRIBUTE) {
            return (V) (isXEqual ? VALUE_VERTICAL : VALUE_HORIZONTAL);
        } else if (attr == LENGTH_ATTRIBUTE) {
            return (V) Integer.valueOf(getLength());
        } else {
            return null;
        }
    }

    public <V> void setValue(Attribute<V> attr, V value) {
        throw new IllegalArgumentException("read only attribute");
    }

    //
    // other methods
    //
    public boolean isVertical() {
        return isXEqual;
    }

    public Location getEndLocation(int index) {
        return index == 0 ? start : end;
    }

    public Location getEnd0() {
        return start;
    }

    public Location getEnd1() {
        return end;
    }

    public Location getOtherEnd(Location loc) {
        return (loc.equals(start) ? end : start);
    }

    public boolean sharesEnd(Wire other) {
        return this.start.equals(other.start) || this.end.equals(other.start)
            || this.start.equals(other.end) || this.end.equals(other.end);
    }

    public boolean overlaps(Wire other, boolean includeEnds) {
        return overlaps(other.start, other.end, includeEnds);
    }

    private boolean overlaps(Location q0, Location q1, boolean includeEnds) {
        if (isXEqual) {
            int x0 = q0.getX();
            if (x0 != q1.getX() || x0 != start.getX()) {
                return false;
            }
            if (includeEnds) {
                return end.getY() >= q0.getY() && start.getY() <= q1.getY();
            } else {
                return end.getY() > q0.getY() && start.getY() < q1.getY();
            }
        } else {
            int y0 = q0.getY();
            if (y0 != q1.getY() || y0 != start.getY()) {
                return false;
            }
            if (includeEnds) {
                return end.getX() >= q0.getX() && start.getX() <= q1.getX();
            } else {
                return end.getX() > q0.getX() && start.getX() < q1.getX();
            }
        }
    }

    public boolean isParallel(Wire other) {
        return this.isXEqual == other.isXEqual;
    }

    public Iterator<Location> iterator() {
        return new WireIterator(start, end);
    }

    public void drawHandles(ComponentDrawContext context) {
        context.drawHandle(start);
        context.drawHandle(end);
    }

    private class EndList extends AbstractList<EndData> {

        @Override
        public EndData get(int i) {
            return getEnd(i);
        }

        @Override
        public int size() {
            return 2;
        }
    }
}
