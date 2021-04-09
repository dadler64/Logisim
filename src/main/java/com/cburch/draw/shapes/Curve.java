/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.shapes;

import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.QuadCurve2D;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Curve extends FillableCanvasObject {

    private Location p0;
    private Location p1;
    private Location p2;
    private Bounds bounds;

    public Curve(Location end0, Location end1, Location control) {
        this.p0 = end0;
        this.p1 = control;
        this.p2 = end1;
        bounds = CurveUtil.getBounds(toArray(p0), toArray(p1), toArray(p2));
    }

    private static double[] toArray(Location location) {
        return new double[]{location.getX(), location.getY()};
    }

    @Override
    public boolean matches(CanvasObject object) {
        if (object instanceof Curve) {
            Curve curve = (Curve) object;
            return this.p0.equals(curve.p0) && this.p1.equals(curve.p1) && this.p2.equals(curve.p2) && super.matches(curve);
        } else {
            return false;
        }
    }

    @Override
    public int matchesHashCode() {
        int ret = p0.hashCode();
        ret = ret * 31 * 31 + p1.hashCode();
        ret = ret * 31 * 31 + p2.hashCode();
        ret = ret * 31 + super.matchesHashCode();
        return ret;
    }

    @Override
    public Element toSvgElement(Document document) {
        return SvgCreator.createCurve(document, this);
    }

    public Location getEnd0() {
        return p0;
    }

    public Location getEnd1() {
        return p2;
    }

    public Location getControl() {
        return p1;
    }

    public QuadCurve2D getCurve2D() {
        return new QuadCurve2D.Double(p0.getX(), p0.getY(), p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    @Override
    public String getDisplayName() {
        return Strings.get("shapeCurve");
    }

    @Override
    public List<Attribute<?>> getAttributes() {
        return DrawAttr.getFillAttributes(getPaintType());
    }

    @Override
    public Bounds getBounds() {
        return bounds;
    }

    @Override
    public boolean contains(Location location, boolean assumeFilled) {
        Object paintType = getPaintType();
        if (assumeFilled && paintType == DrawAttr.PAINT_STROKE) {
            paintType = DrawAttr.PAINT_STROKE_FILL;
        }
        if (paintType != DrawAttr.PAINT_FILL) {
            int stroke = getStrokeWidth();
            double[] locations = toArray(location);
            double[] p0 = toArray(this.p0);
            double[] p1 = toArray(this.p1);
            double[] p2 = toArray(this.p2);
            double[] nearestPoint = CurveUtil.findNearestPoint(locations, p0, p1, p2);
            if (nearestPoint == null) {
                return false;
            }

            int threshold;
            if (paintType == DrawAttr.PAINT_STROKE) {
                threshold = Math.max(Line.ON_LINE_THRESH, stroke / 2);
            } else {
                threshold = stroke / 2;
            }
            if (LineUtil.distanceSquared(nearestPoint[0], nearestPoint[1], locations[0], locations[1])
                < threshold * threshold) {
                return true;
            }
        }
        if (paintType != DrawAttr.PAINT_STROKE) {
            QuadCurve2D curve = getCurve(null);
            return curve.contains(location.getX(), location.getY());
        }
        return false;
    }

    @Override
    public void translate(int deltaX, int deltaY) {
        p0 = p0.translate(deltaX, deltaY);
        p1 = p1.translate(deltaX, deltaY);
        p2 = p2.translate(deltaX, deltaY);
        bounds = bounds.translate(deltaX, deltaY);
    }

    public List<Handle> getHandles() {
        return UnmodifiableList.create(getHandleArray(null));
    }

    @Override
    public List<Handle> getHandles(HandleGesture gesture) {
        return UnmodifiableList.create(getHandleArray(gesture));
    }

    private Handle[] getHandleArray(HandleGesture gesture) {
        if (gesture == null) {
            return new Handle[]{new Handle(this, p0), new Handle(this, p1),
                new Handle(this, p2)};
        } else {
            Handle gestureHandle = gesture.getHandle();
            int gx = gestureHandle.getX() + gesture.getDeltaX();
            int gy = gestureHandle.getY() + gesture.getDeltaY();
            Handle[] handles = {new Handle(this, p0), new Handle(this, p1),
                new Handle(this, p2)};
            if (gestureHandle.isAt(p0)) {
                if (gesture.isShiftDown()) {
                    Location point = LineUtil.snapTo8Cardinals(p2, gx, gy);
                    handles[0] = new Handle(this, point);
                } else {
                    handles[0] = new Handle(this, gx, gy);
                }
            } else if (gestureHandle.isAt(p2)) {
                if (gesture.isShiftDown()) {
                    Location point = LineUtil.snapTo8Cardinals(p0, gx, gy);
                    handles[2] = new Handle(this, point);
                } else {
                    handles[2] = new Handle(this, gx, gy);
                }
            } else if (gestureHandle.isAt(p1)) {
                if (gesture.isShiftDown()) {
                    double x0 = p0.getX();
                    double y0 = p0.getY();
                    double x1 = p2.getX();
                    double y1 = p2.getY();
                    double midPointX = (x0 + x1) / 2;
                    double midPointY = (y0 + y1) / 2;
                    double deltaX = x1 - x0;
                    double deltaY = y1 - y0;
                    double[] p = LineUtil.nearestPointInfinite(gx, gy,
                        midPointX, midPointY, midPointX - deltaY, midPointY + deltaX);
                    gx = (int) Math.round(p[0]);
                    gy = (int) Math.round(p[1]);
                }
                if (gesture.isAltDown()) {
                    double[] e0 = {p0.getX(), p0.getY()};
                    double[] e1 = {p2.getX(), p2.getY()};
                    double[] midPoint = {gx, gy};
                    double[] ct = CurveUtil.interpolate(e0, e1, midPoint);
                    gx = (int) Math.round(ct[0]);
                    gy = (int) Math.round(ct[1]);
                }
                handles[1] = new Handle(this, gx, gy);
            }
            return handles;
        }
    }

    @Override
    public boolean canMoveHandle(Handle handle) {
        return true;
    }

    @Override
    public Handle moveHandle(HandleGesture gesture) {
        Handle[] handles = getHandleArray(gesture);
        Handle handle = null;
        if (!handles[0].getLocation().equals(p0)) {
            p0 = handles[0].getLocation();
            handle = handles[0];
        }
        if (!handles[1].getLocation().equals(p1)) {
            p1 = handles[1].getLocation();
            handle = handles[1];
        }
        if (!handles[2].getLocation().equals(p2)) {
            p2 = handles[2].getLocation();
            handle = handles[2];
        }
        bounds = CurveUtil.getBounds(toArray(p0), toArray(p1), toArray(p2));
        return handle;
    }

    @Override
    public void paint(Graphics graphics, HandleGesture gesture) {
        QuadCurve2D curve = getCurve(gesture);
        if (setForFill(graphics)) {
            ((Graphics2D) graphics).fill(curve);
        }
        if (setForStroke(graphics)) {
            ((Graphics2D) graphics).draw(curve);
        }
    }

    private QuadCurve2D getCurve(HandleGesture gesture) {
        Handle[] handles = getHandleArray(gesture);
        return new QuadCurve2D.Double(handles[0].getX(), handles[0].getY(), handles[1].getX(), handles[1].getY(),
            handles[2].getX(), handles[2].getY());
    }
}
