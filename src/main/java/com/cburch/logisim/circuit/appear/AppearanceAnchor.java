/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.circuit.appear;

import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Graphics;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AppearanceAnchor extends AppearanceElement {

    public static final Attribute<Direction> FACING
        = Attributes.forDirection("facing", Strings.getter("appearanceFacingAttr"));
    static final List<Attribute<?>> ATTRIBUTES = UnmodifiableList.create(new Attribute<?>[]{FACING});

    private static final int RADIUS = 3;
    private static final int INDICATOR_LENGTH = 8;
    private static final Color SYMBOL_COLOR = new Color(0, 128, 0);

    private Direction facing;

    public AppearanceAnchor(Location location) {
        super(location);
        facing = Direction.EAST;
    }

    @Override
    public boolean matches(CanvasObject object) {
        if (object instanceof AppearanceAnchor) {
            AppearanceAnchor that = (AppearanceAnchor) object;
            return super.matches(that) && this.facing.equals(that.facing);
        } else {
            return false;
        }
    }

    @Override
    public int matchesHashCode() {
        return super.matchesHashCode() * 31 + facing.hashCode();
    }

    @Override
    public String getDisplayName() {
        return Strings.get("circuitAnchor");
    }

    @Override
    public Element toSvgElement(Document document) {
        Location loc = getLocation();
        Element ret = document.createElement("circ-anchor");
        ret.setAttribute("x", "" + (loc.getX() - RADIUS));
        ret.setAttribute("y", "" + (loc.getY() - RADIUS));
        ret.setAttribute("width", "" + 2 * RADIUS);
        ret.setAttribute("height", "" + 2 * RADIUS);
        ret.setAttribute("facing", facing.toString());
        return ret;
    }

    public Direction getFacing() {
        return facing;
    }

    @Override
    public List<Attribute<?>> getAttributes() {
        return ATTRIBUTES;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V getValue(Attribute<V> attribute) {
        if (attribute == FACING) {
            return (V) facing;
        } else {
            return super.getValue(attribute);
        }
    }

    @Override
    protected void updateValue(Attribute<?> attribute, Object value) {
        if (attribute == FACING) {
            facing = (Direction) value;
        } else {
            super.updateValue(attribute, value);
        }
    }

    @Override
    public void paint(Graphics graphics, HandleGesture gesture) {
        Location location = getLocation();
        int x = location.getX();
        int y = location.getY();
        graphics.setColor(SYMBOL_COLOR);
        graphics.drawOval(x - RADIUS, y - RADIUS, 2 * RADIUS, 2 * RADIUS);
        Location e0 = location.translate(facing, RADIUS);
        Location e1 = location.translate(facing, RADIUS + INDICATOR_LENGTH);
        graphics.drawLine(e0.getX(), e0.getY(), e1.getX(), e1.getY());
    }

    @Override
    public Bounds getBounds() {
        Bounds bds = super.getBounds(RADIUS);
        Location center = getLocation();
        Location end = center.translate(facing, RADIUS + INDICATOR_LENGTH);
        return bds.add(end);
    }

    @Override
    public boolean contains(Location location, boolean assumeFilled) {
        if (super.isInCircle(location, RADIUS)) {
            return true;
        } else {
            Location center = getLocation();
            Location end = center.translate(facing, RADIUS + INDICATOR_LENGTH);
            if (facing == Direction.EAST || facing == Direction.WEST) {
                return Math.abs(location.getY() - center.getY()) < 2
                    && (location.getX() < center.getX()) != (location.getX() < end.getX());
            } else {
                return Math.abs(location.getX() - center.getX()) < 2
                    && (location.getY() < center.getY()) != (location.getY() < end.getY());
            }
        }
    }

    @Override
    public List<Handle> getHandles(HandleGesture gesture) {
        Location c = getLocation();
        Location end = c.translate(facing, RADIUS + INDICATOR_LENGTH);
        return UnmodifiableList.create(new Handle[]{new Handle(this, c),
            new Handle(this, end)});
    }
}
