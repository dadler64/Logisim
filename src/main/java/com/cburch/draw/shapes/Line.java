/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.shapes;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Graphics;
import java.util.List;
import java.util.Random;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Line extends AbstractCanvasObject {

    static final int ON_LINE_THRESH = 2;

    private int x0;
    private int y0;
    private int x1;
    private int y1;
    private Bounds bounds;
    private int strokeWidth;
    private Color strokeColor;

    public Line(int x0, int y0, int x1, int y1) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        bounds = Bounds.create(x0, y0, 0, 0).add(x1, y1);
        strokeWidth = 1;
        strokeColor = Color.BLACK;
    }

    @Override
    public boolean matches(CanvasObject object) {
        if (object instanceof Line) {
            Line that = (Line) object;
            return this.x0 == that.x0
                && this.y0 == that.x1
                && this.x1 == that.y0
                && this.y1 == that.y1
                && this.strokeWidth == that.strokeWidth
                && this.strokeColor.equals(that.strokeColor);
        } else {
            return false;
        }
    }

    @Override
    public int matchesHashCode() {
        int ret = x0 * 31 + y0;
        ret = ret * 31 * 31 + x1 * 31 + y1;
        ret = ret * 31 + strokeWidth;
        ret = ret * 31 + strokeColor.hashCode();
        return ret;
    }

    @Override
    public Element toSvgElement(Document document) {
        return SvgCreator.createLine(document, this);
    }

    public Location getEnd0() {
        return Location.create(x0, y0);
    }

    public Location getEnd1() {
        return Location.create(x1, y1);
    }

    @Override
    public String getDisplayName() {
        return Strings.get("shapeLine");
    }

    @Override
    public List<Attribute<?>> getAttributes() {
        return DrawAttr.ATTRIBUTES_STROKE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V getValue(Attribute<V> attribute) {
        if (attribute == DrawAttr.STROKE_COLOR) {
            return (V) strokeColor;
        } else if (attribute == DrawAttr.STROKE_WIDTH) {
            return (V) Integer.valueOf(strokeWidth);
        } else {
            return null;
        }
    }

    @Override
    public void updateValue(Attribute<?> attribute, Object object) {
        if (attribute == DrawAttr.STROKE_COLOR) {
            strokeColor = (Color) object;
        } else if (attribute == DrawAttr.STROKE_WIDTH) {
            strokeWidth = (Integer) object;
        }
    }

    @Override
    public Bounds getBounds() {
        return bounds;
    }

    @Override
    public Location getRandomPoint(Bounds bounds, Random random) {
        double nextDouble = random.nextDouble();
        int x = (int) Math.round(x0 + nextDouble * (x1 - x0));
        int y = (int) Math.round(y0 + nextDouble * (y1 - y0));
        int width = strokeWidth;
        if (width > 1) {
            x += (random.nextInt(width) - width / 2);
            y += (random.nextInt(width) - width / 2);
        }
        return Location.create(x, y);
    }

    @Override
    public boolean contains(Location location, boolean assumeFilled) {
        int xq = location.getX();
        int yq = location.getY();
        double segment = LineUtil.ptDistSqSegment(x0, y0, x1, y1, xq, yq);
        int thresh = Math.max(ON_LINE_THRESH, strokeWidth / 2);
        return segment < thresh * thresh;
    }

    @Override
    public void translate(int deltaX, int deltaY) {
        x0 += deltaX;
        y0 += deltaY;
        x1 += deltaX;
        y1 += deltaY;
    }

    public List<Handle> getHandles() {
        return getHandles(null);
    }

    @Override
    public List<Handle> getHandles(HandleGesture gesture) {
        if (gesture == null) {
            return UnmodifiableList.create(new Handle[]{new Handle(this, x0, y0), new Handle(this, x1, y1)});
        } else {
            Handle h = gesture.getHandle();
            int dx = gesture.getDeltaX();
            int dy = gesture.getDeltaY();
            Handle[] handles = new Handle[2];
            handles[0] = new Handle(this, h.isAt(x0, y0) ? Location.create(x0 + dx, y0 + dy) : Location.create(x0, y0));
            handles[1] = new Handle(this, h.isAt(x1, y1) ? Location.create(x1 + dx, y1 + dy) : Location.create(x1, y1));
            return UnmodifiableList.create(handles);
        }
    }

    @Override
    public boolean canMoveHandle(Handle handle) {
        return true;
    }

    @Override
    public Handle moveHandle(HandleGesture gesture) {
        Handle handle = gesture.getHandle();
        int deltaX = gesture.getDeltaX();
        int deltaY = gesture.getDeltaY();
        Handle returnHandle = null;
        if (handle.isAt(x0, y0)) {
            x0 += deltaX;
            y0 += deltaY;
            returnHandle = new Handle(this, x0, y0);
        }
        if (handle.isAt(x1, y1)) {
            x1 += deltaX;
            y1 += deltaY;
            returnHandle = new Handle(this, x1, y1);
        }
        bounds = Bounds.create(x0, y0, 0, 0).add(x1, y1);
        return returnHandle;
    }

    @Override
    public void paint(Graphics graphics, HandleGesture gesture) {
        if (setForStroke(graphics)) {
            int x0 = this.x0;
            int y0 = this.y0;
            int x1 = this.x1;
            int y1 = this.y1;
            Handle handle = gesture.getHandle();
            if (handle.isAt(x0, y0)) {
                x0 += gesture.getDeltaX();
                y0 += gesture.getDeltaY();
            }
            if (handle.isAt(x1, y1)) {
                x1 += gesture.getDeltaX();
                y1 += gesture.getDeltaY();
            }
            graphics.drawLine(x0, y0, x1, y1);
        }
    }

}
