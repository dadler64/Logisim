/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.shapes;

import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Graphics;
import java.util.List;

abstract class Rectangular extends FillableCanvasObject {

    private Bounds bounds; // excluding the stroke's width

    public Rectangular(int x, int y, int width, int height) {
        bounds = Bounds.create(x, y, width, height);
    }

    @Override
    public boolean matches(CanvasObject object) {
        if (object instanceof Rectangular) {
            Rectangular that = (Rectangular) object;
            return this.bounds.equals(that.bounds) && super.matches(that);
        } else {
            return false;
        }
    }

    @Override
    public int matchesHashCode() {
        return bounds.hashCode() * 31 + super.matchesHashCode();
    }

    public int getX() {
        return bounds.getX();
    }

    public int getY() {
        return bounds.getY();
    }

    public int getWidth() {
        return bounds.getWidth();
    }

    public int getHeight() {
        return bounds.getHeight();
    }

    @Override
    public Bounds getBounds() {
        int width = getStrokeWidth();
        Object paintType = getPaintType();
        if (width < 2 || paintType == DrawAttr.PAINT_FILL) {
            return bounds;
        } else {
            return bounds.expand(width / 2);
        }
    }

    @Override
    public void translate(int deltaX, int deltaY) {
        bounds = bounds.translate(deltaX, deltaY);
    }

    @Override
    public List<Handle> getHandles(HandleGesture gesture) {
        return UnmodifiableList.create(getHandleArray(gesture));
    }

    private Handle[] getHandleArray(HandleGesture gesture) {
        Bounds bounds = this.bounds;
        int x0 = bounds.getX();
        int y0 = bounds.getY();
        int x1 = x0 + bounds.getWidth();
        int y1 = y0 + bounds.getHeight();
        if (gesture == null) {
            return new Handle[]{new Handle(this, x0, y0),
                    new Handle(this, x1, y0), new Handle(this, x1, y1),
                    new Handle(this, x0, y1)};
        } else {
            int handleX = gesture.getHandle().getX();
            int heightY = gesture.getHandle().getY();
            int deltaX = gesture.getDeltaX();
            int deltaY = gesture.getDeltaY();
            int newX0 = x0 == handleX ? x0 + deltaX : x0;
            int newY0 = y0 == heightY ? y0 + deltaY : y0;
            int newX1 = x1 == handleX ? x1 + deltaX : x1;
            int newY1 = y1 == heightY ? y1 + deltaY : y1;
            if (gesture.isShiftDown()) {
                if (gesture.isAltDown()) {
                    if (x0 == handleX) {
                        newX1 -= deltaX;
                    }
                    if (x1 == handleX) {
                        newX0 -= deltaX;
                    }
                    if (y0 == heightY) {
                        newY1 -= deltaY;
                    }
                    if (y1 == heightY) {
                        newY0 -= deltaY;
                    }

                    int width = Math.abs(newX1 - newX0);
                    int height = Math.abs(newY1 - newY0);
                    if (width > height) { // reduce width to height
                        int dw = (width - height) / 2;
                        newX0 -= (newX0 > newX1 ? 1 : -1) * dw;
                        newX1 -= (newX1 > newX0 ? 1 : -1) * dw;
                    } else {
                        int dh = (height - width) / 2;
                        newY0 -= (newY0 > newY1 ? 1 : -1) * dh;
                        newY1 -= (newY1 > newY0 ? 1 : -1) * dh;
                    }
                } else {
                    int width = Math.abs(newX1 - newX0);
                    int height = Math.abs(newY1 - newY0);
                    if (width > height) { // reduce width to height
                        if (x0 == handleX) {
                            newX0 = newX1 + (newX0 > newX1 ? 1 : -1) * height;
                        }
                        if (x1 == handleX) {
                            newX1 = newX0 + (newX1 > newX0 ? 1 : -1) * height;
                        }
                    } else { // reduce height to w
                        if (y0 == heightY) {
                            newY0 = newY1 + (newY0 > newY1 ? 1 : -1) * width;
                        }
                        if (y1 == heightY) {
                            newY1 = newY0 + (newY1 > newY0 ? 1 : -1) * width;
                        }
                    }
                }
            } else {
                if (gesture.isAltDown()) {
                    if (x0 == handleX) {
                        newX1 -= deltaX;
                    }
                    if (x1 == handleX) {
                        newX0 -= deltaX;
                    }
                    if (y0 == heightY) {
                        newY1 -= deltaY;
                    }
                    if (y1 == heightY) {
                        newY0 -= deltaY;
                    }
                }  // else is already handled

            }
            return new Handle[]{new Handle(this, newX0, newY0),
                    new Handle(this, newX1, newY0), new Handle(this, newX1, newY1),
                    new Handle(this, newX0, newY1)};
        }
    }

    @Override
    public boolean canMoveHandle(Handle handle) {
        return true;
    }

    @Override
    public Handle moveHandle(HandleGesture gesture) {
        Handle[] oldHandles = getHandleArray(null);
        Handle[] newHandles = getHandleArray(gesture);
        Handle moved = gesture == null ? null : gesture.getHandle();
        Handle result = null;
        int x0 = Integer.MAX_VALUE;
        int x1 = Integer.MIN_VALUE;
        int y0 = Integer.MAX_VALUE;
        int y1 = Integer.MIN_VALUE;
        int index = -1;
        for (Handle handle : newHandles) {
            index++;
            if (oldHandles[index].equals(moved)) {
                result = handle;
            }
            int hx = handle.getX();
            int hy = handle.getY();
            if (hx < x0) {
                x0 = hx;
            }
            if (hx > x1) {
                x1 = hx;
            }
            if (hy < y0) {
                y0 = hy;
            }
            if (hy > y1) {
                y1 = hy;
            }
        }
        bounds = Bounds.create(x0, y0, x1 - x0, y1 - y0);
        return result;
    }

    @Override
    public void paint(Graphics graphics, HandleGesture gesture) {
        if (gesture == null) {
            Bounds bounds = this.bounds;
            draw(graphics, bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
        } else {
            Handle[] handles = getHandleArray(gesture);
            Handle p0 = handles[0];
            Handle p1 = handles[2];
            int x0 = p0.getX();
            int y0 = p0.getY();
            int x1 = p1.getX();
            int y1 = p1.getY();
            if (x1 < x0) {
                int t = x0;
                x0 = x1;
                x1 = t;
            }
            if (y1 < y0) {
                int t = y0;
                y0 = y1;
                y1 = t;
            }

            draw(graphics, x0, y0, x1 - x0, y1 - y0);
        }
    }

    @Override
    public boolean contains(Location location, boolean assumeFilled) {
        Object type = getPaintType();
        if (assumeFilled && type == DrawAttr.PAINT_STROKE) {
            type = DrawAttr.PAINT_STROKE_FILL;
        }
        Bounds bounds = this.bounds;
        int x = bounds.getX();
        int y = bounds.getY();
        int width = bounds.getWidth();
        int height = bounds.getHeight();
        int qx = location.getX();
        int qy = location.getY();
        if (type == DrawAttr.PAINT_FILL) {
            return isInRectangle(qx, qy, x, y, width, height) && contains(x, y, width, height, location);
        } else if (type == DrawAttr.PAINT_STROKE) {
            int stroke = getStrokeWidth();
            int tol2 = Math.max(2 * Line.ON_LINE_THRESH, stroke);
            int tol = tol2 / 2;
            return isInRectangle(qx, qy, x - tol, y - tol, width + tol2, height + tol2)
                    && contains(x - tol, y - tol, width + tol2, height + tol2, location)
                    && !contains(x + tol, y + tol, width - tol2, height - tol2, location);
        } else if (type == DrawAttr.PAINT_STROKE_FILL) {
            int stroke = getStrokeWidth();
            int tol = stroke / 2;
            return isInRectangle(qx, qy, x - tol, y - tol, width + stroke, height + stroke)
                    && contains(x - tol, y - tol, width + stroke, height + stroke, location);
        } else {
            return false;
        }
    }

    boolean isInRectangle(int qx, int qy, int x0, int y0, int width, int height) {
        return qx >= x0 && qx < x0 + width && qy >= y0 && qy < y0 + height;
    }

    protected abstract boolean contains(int x, int y, int width, int height, Location q);

    protected abstract void draw(Graphics graphics, int x, int y, int width, int height);
}
