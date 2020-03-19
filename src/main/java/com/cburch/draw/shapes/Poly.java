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
import java.awt.geom.GeneralPath;
import java.util.List;
import java.util.Random;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Poly extends FillableCanvasObject {

    private boolean closed;
    // "handles" should be immutable - create a new array and change using
    // setHandles rather than changing contents
    private Handle[] handles;
    private GeneralPath path;
    private double[] lengths;
    private Bounds bounds;

    public Poly(boolean closed, List<Location> locations) {
        Handle[] handles = new Handle[locations.size()];
        int i = -1;
        for (Location location : locations) {
            i++;
            handles[i] = new Handle(this, location.getX(), location.getY());
        }

        this.closed = closed;
        this.handles = handles;
        recomputeBounds();
    }

    @Override
    public Poly clone() {
        Poly poly = (Poly) super.clone();
        Handle[] handles = this.handles.clone();
        for (int i = 0, n = handles.length; i < n; i++) {
            Handle oldHandle = handles[i];
            handles[i] = new Handle(poly, oldHandle.getX(), oldHandle.getY());
        }
        poly.handles = handles;
        return poly;
    }

    @Override
    public boolean matches(CanvasObject object) {
        if (object instanceof Poly) {
            Poly poly = (Poly) object;
            Handle[] handles = this.handles;
            Handle[] polyHandles = poly.handles;
            if (this.closed != poly.closed || handles.length != polyHandles.length) {
                return false;
            } else {
                for (int i = 0, n = handles.length; i < n; i++) {
                    if (!handles[i].equals(polyHandles[i])) {
                        return false;
                    }
                }
                return super.matches(poly);
            }
        } else {
            return false;
        }
    }

    @Override
    public int matchesHashCode() {
        int hashCode = super.matchesHashCode();
        hashCode = hashCode * 3 + (closed ? 1 : 0);
        Handle[] handles = this.handles;
        for (Handle handle : handles) {
            hashCode = hashCode * 31 + handle.hashCode();
        }
        return hashCode;
    }

    @Override
    public String getDisplayName() {
        if (closed) {
            return Strings.get("shapePolygon");
        } else {
            return Strings.get("shapePolyline");
        }
    }

    @Override
    public Element toSvgElement(Document document) {
        return SvgCreator.createPoly(document, this);
    }

    @Override
    public List<Attribute<?>> getAttributes() {
        return DrawAttr.getFillAttributes(getPaintType());
    }

    @Override
    public final boolean contains(Location location, boolean assumeFilled) {
        Object paintType = getPaintType();
        if (assumeFilled && paintType == DrawAttr.PAINT_STROKE) {
            paintType = DrawAttr.PAINT_STROKE_FILL;
        }
        if (paintType == DrawAttr.PAINT_STROKE) {
            int thresh = Math.max(Line.ON_LINE_THRESH, getStrokeWidth() / 2);
            PolyUtil.ClosestResult result = PolyUtil.getClosestPoint(location, closed, handles);
            assert result != null;
            return result.getDistanceSq() < thresh * thresh;
        } else if (paintType == DrawAttr.PAINT_FILL) {
            GeneralPath path = getPath();
            return path.contains(location.getX(), location.getY());
        } else { // fill and stroke
            GeneralPath path = getPath();
            if (path.contains(location.getX(), location.getY())) {
                return true;
            }
            int width = getStrokeWidth();
            PolyUtil.ClosestResult result = PolyUtil.getClosestPoint(location,
                    closed, handles);
            assert result != null;
            return result.getDistanceSq() < (width * width) / 4.0;
        }
    }

    @Override
    public final Location getRandomPoint(Bounds bounds, Random random) {
        if (getPaintType() == DrawAttr.PAINT_STROKE) {
            Location boundaryPoint = getRandomBoundaryPoint(bounds, random);
            int width = getStrokeWidth();
            if (width > 1) {
                int deltaX = random.nextInt(width) - width / 2;
                int deltaY = random.nextInt(width) - width / 2;
                boundaryPoint = boundaryPoint.translate(deltaX, deltaY);
            }
            return boundaryPoint;
        } else {
            return super.getRandomPoint(bounds, random);
        }
    }

    private Location getRandomBoundaryPoint(Bounds bounds, Random random) {
        Handle[] handles = this.handles;
        double[] lengths = this.lengths;
        if (lengths == null) {
            lengths = new double[handles.length + (closed ? 1 : 0)];
            double total = 0.0;
            for (int i = 0; i < lengths.length; i++) {
                int j = (i + 1) % handles.length;
                total += LineUtil.distance(handles[i].getX(), handles[i].getY(), handles[j].getX(), handles[j].getY());
                lengths[i] = total;
            }
            this.lengths = lengths;
        }
        double position = lengths[lengths.length - 1] * random.nextDouble();
        for (int i = 0; true; i++) {
            if (position < lengths[i]) {
                Handle p = handles[i];
                Handle q = handles[(i + 1) % handles.length];
                double u = Math.random();
                int x = (int) Math.round(p.getX() + u * (q.getX() - p.getX()));
                int y = (int) Math.round(p.getY() + u * (q.getY() - p.getY()));
                return Location.create(x, y);
            }
        }
    }

    @Override
    public Bounds getBounds() {
        return bounds;
    }

    @Override
    public void translate(int deltaX, int deltaY) {
        Handle[] handles = this.handles;
        Handle[] newHandles = new Handle[handles.length];
        for (int i = 0; i < handles.length; i++) {
            newHandles[i] = new Handle(this, handles[i].getX() + deltaX, handles[i].getY() + deltaY);
        }
        setHandles(newHandles);
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public List<Handle> getHandles(HandleGesture gesture) {
        Handle[] handles = this.handles;
        if (gesture == null) {
            return UnmodifiableList.create(handles);
        } else {
            Handle gestureHandle = gesture.getHandle();
            Handle[] retHandles = new Handle[handles.length];
            for (int i = 0, n = handles.length; i < n; i++) {
                Handle handle = handles[i];
                if (handle.equals(gestureHandle)) {
                    int x = handle.getX() + gesture.getDeltaX();
                    int y = handle.getY() + gesture.getDeltaY();
                    Location r;
                    if (gesture.isShiftDown()) {
                        Location previous = handles[(i + n - 1) % n].getLocation();
                        Location next = handles[(i + 1) % n].getLocation();
                        if (!closed) {
                            if (i == 0) {
                                previous = null;
                            }
                            if (i == n - 1) {
                                next = null;
                            }
                        }
                        if (previous == null) {
                            assert next != null;
                            r = LineUtil.snapTo8Cardinals(next, x, y);
                        } else if (next == null) {
                            r = LineUtil.snapTo8Cardinals(previous, x, y);
                        } else {
                            Location to = Location.create(x, y);
                            Location a = LineUtil.snapTo8Cardinals(previous, x, y);
                            Location b = LineUtil.snapTo8Cardinals(next, x, y);
                            int ad = a.manhattanDistanceTo(to);
                            int bd = b.manhattanDistanceTo(to);
                            r = ad < bd ? a : b;
                        }
                    } else {
                        r = Location.create(x, y);
                    }
                    retHandles[i] = new Handle(this, r);
                } else {
                    retHandles[i] = handle;
                }
            }
            return UnmodifiableList.create(retHandles);
        }
    }

    @Override
    public boolean canMoveHandle(Handle handle) {
        return true;
    }

    @Override
    public Handle moveHandle(HandleGesture gesture) {
        List<Handle> handles = getHandles(gesture);
        Handle[] newHandles = new Handle[handles.size()];
        Handle retHandle = null;
        int i = -1;
        for (Handle handle : handles) {
            i++;
            newHandles[i] = handle;
        }
        setHandles(newHandles);
        return retHandle;
    }

    @Override
    public Handle canInsertHandle(Location location) {
        PolyUtil.ClosestResult result = PolyUtil.getClosestPoint(location, closed, handles);
        int thresh = Math.max(Line.ON_LINE_THRESH, getStrokeWidth() / 2);
        assert result != null;
        if (result.getDistanceSq() < thresh * thresh) {
            Location resultLocation = result.getLocation();
            if (result.getPreviousHandle().isAt(resultLocation) || result.getNextHandle().isAt(resultLocation)) {
                return null;
            } else {
                return new Handle(this, result.getLocation());
            }
        } else {
            return null;
        }
    }

    @Override
    public Handle canDeleteHandle(Location location) {
        int minHandles = closed ? 3 : 2;
        Handle[] handles = this.handles;
        if (handles.length <= minHandles) {
            return null;
        } else {
            int qx = location.getX();
            int qy = location.getY();
            int width = Math.max(Line.ON_LINE_THRESH, getStrokeWidth() / 2);
            for (Handle handle : handles) {
                int hx = handle.getX();
                int hy = handle.getY();
                if (LineUtil.distance(qx, qy, hx, hy) < width * width) {
                    return handle;
                }
            }
            return null;
        }
    }

    @Override
    public void insertHandle(Handle desired, Handle previous) {
        Location location = desired.getLocation();
        Handle[] handles = this.handles;
        if (previous == null) {
            PolyUtil.ClosestResult result = PolyUtil.getClosestPoint(location,
                    closed, handles);
            previous = result != null ? result.getPreviousHandle() : null;
        }
        Handle[] is = new Handle[handles.length + 1];
        boolean inserted = false;
        for (int i = 0; i < handles.length; i++) {
            if (inserted) {
                is[i + 1] = handles[i];
            } else if (handles[i].equals(previous)) {
                inserted = true;
                is[i] = handles[i];
                is[i + 1] = desired;
            } else {
                is[i] = handles[i];
            }
        }
        if (!inserted) {
            throw new IllegalArgumentException("no such handle");
        }
        setHandles(is);
    }

    @Override
    public Handle deleteHandle(Handle handle) {
        Handle[] handles = this.handles;
        int n = handles.length;
        Handle[] is = new Handle[n - 1];
        Handle previous = null;
        boolean deleted = false;
        for (int i = 0; i < n; i++) {
            if (deleted) {
                is[i - 1] = handles[i];
            } else if (handles[i].equals(handle)) {
                if (previous == null) {
                    previous = handles[n - 1];
                }
                deleted = true;
            } else {
                previous = handles[i];
                is[i] = handles[i];
            }
        }
        setHandles(is);
        return previous;
    }

    @Override
    public void paint(Graphics graphics, HandleGesture gesture) {
        List<Handle> handles = getHandles(gesture);
        int[] xs = new int[handles.size()];
        int[] ys = new int[handles.size()];
        int i = -1;
        for (Handle handle : handles) {
            i++;
            xs[i] = handle.getX();
            ys[i] = handle.getY();
        }

        if (setForFill(graphics)) {
            graphics.fillPolygon(xs, ys, xs.length);
        }
        if (setForStroke(graphics)) {
            if (closed) {
                graphics.drawPolygon(xs, ys, xs.length);
            } else {
                graphics.drawPolyline(xs, ys, xs.length);
            }
        }
    }

    private void setHandles(Handle[] handles) {
        this.handles = handles;
        lengths = null;
        path = null;
        recomputeBounds();
    }

    private void recomputeBounds() {
        Handle[] handles = this.handles;
        int x0 = handles[0].getX();
        int y0 = handles[0].getY();
        int x1 = x0;
        int y1 = y0;
        for (int i = 1; i < handles.length; i++) {
            int x = handles[i].getX();
            int y = handles[i].getY();
            if (x < x0) {
                x0 = x;
            }
            if (x > x1) {
                x1 = x;
            }
            if (y < y0) {
                y0 = y;
            }
            if (y > y1) {
                y1 = y;
            }
        }
        Bounds bounds = Bounds.create(x0, y0, x1 - x0 + 1, y1 - y0 + 1);
        int stroke = getStrokeWidth();
        this.bounds = stroke < 2 ? bounds : bounds.expand(stroke / 2);
    }

    private GeneralPath getPath() {
        GeneralPath path = this.path;
        if (path == null) {
            path = new GeneralPath();
            Handle[] handles = this.handles;
            if (handles.length > 0) {
                boolean first = true;
                for (Handle handle : handles) {
                    if (first) {
                        path.moveTo(handle.getX(), handle.getY());
                        first = false;
                    } else {
                        path.lineTo(handle.getX(), handle.getY());
                    }
                }
            }
            this.path = path;
        }
        return path;
    }
}
