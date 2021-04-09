/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.tools;

import com.cburch.draw.actions.ModelAddAction;
import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.draw.shapes.LineUtil;
import com.cburch.draw.shapes.Poly;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.Icons;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;

public class PolyTool extends AbstractTool {

    // how close we need to be to the start point to count as "closing the loop"
    private static final int CLOSE_TOLERANCE = 2;

    private final boolean isClosed; // whether we are drawing polygons or polylines
    private final DrawingAttributeSet attributeSet;
    private final ArrayList<Location> locations;
    private boolean isActive;
    private boolean isMouseDown;
    private int lastMouseX;
    private int lastMouseY;

    public PolyTool(boolean isClosed, DrawingAttributeSet attributeSet) {
        this.isClosed = isClosed;
        this.attributeSet = attributeSet;
        isActive = false;
        locations = new ArrayList<>();
    }

    @Override
    public Icon getIcon() {
        if (isClosed) {
            return Icons.getIcon("drawpoly.gif");
        } else {
            return Icons.getIcon("drawplin.gif");
        }
    }

    @Override
    public List<Attribute<?>> getAttributes() {
        return DrawAttr.getFillAttributes(attributeSet.getValue(DrawAttr.PAINT_TYPE));
    }

    @Override
    public Cursor getCursor(Canvas canvas) {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    @Override
    public void toolDeselected(Canvas canvas) {
        CanvasObject add = commit(canvas);
        canvas.toolGestureComplete(this, add);
        repaintArea(canvas);
    }

    @Override
    public void mousePressed(Canvas canvas, MouseEvent event) {
        int mouseX = event.getX();
        int mouseY = event.getY();
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        int modifiers = event.getModifiersEx();
        if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0) {
            mouseX = canvas.snapX(mouseX);
            mouseY = canvas.snapY(mouseY);
        }

        if (isActive && event.getClickCount() > 1) {
            CanvasObject add = commit(canvas);
            canvas.toolGestureComplete(this, add);
            return;
        }

        Location location = Location.create(mouseX, mouseY);
        ArrayList<Location> locations = this.locations;
        if (!isActive) {
            locations.clear();
            locations.add(location);
        }
        locations.add(location);

        isMouseDown = true;
        isActive = canvas.getModel() != null;
        repaintArea(canvas);
    }

    @Override
    public void mouseDragged(Canvas canvas, MouseEvent event) {
        updateMouse(canvas, event.getX(), event.getY(), event.getModifiersEx());
    }

    @Override
    public void mouseReleased(Canvas canvas, MouseEvent event) {
        if (isActive) {
            updateMouse(canvas, event.getX(), event.getY(), event.getModifiersEx());
            isMouseDown = false;
            int size = locations.size();
            if (size >= 3) {
                Location first = locations.get(0);
                Location last = locations.get(size - 1);
                if (first.manhattanDistanceTo(last) <= CLOSE_TOLERANCE) {
                    locations.remove(size - 1);
                    CanvasObject add = commit(canvas);
                    canvas.toolGestureComplete(this, add);
                }
            }
        }
    }

    @Override
    public void keyPressed(Canvas canvas, KeyEvent event) {
        int code = event.getKeyCode();
        if (isActive && isMouseDown && (code == KeyEvent.VK_SHIFT || code == KeyEvent.VK_CONTROL)) {
            updateMouse(canvas, lastMouseX, lastMouseY, event.getModifiersEx());
        }
    }

    @Override
    public void keyReleased(Canvas canvas, KeyEvent event) {
        keyPressed(canvas, event);
    }

    @Override
    public void keyTyped(Canvas canvas, KeyEvent event) {
        if (isActive) {
            char ch = event.getKeyChar();
            if (ch == '\u001b') { // escape key
                isActive = false;
                locations.clear();
                repaintArea(canvas);
                canvas.toolGestureComplete(this, null);
            } else if (ch == '\n') { // enter key
                CanvasObject add = commit(canvas);
                canvas.toolGestureComplete(this, add);
            }
        }
    }

    private CanvasObject commit(Canvas canvas) {
        if (!isActive) {
            return null;
        }
        CanvasObject add = null;
        isActive = false;
        ArrayList<Location> locations = this.locations;
        for (int i = locations.size() - 2; i >= 0; i--) {
            if (locations.get(i).equals(locations.get(i + 1))) {
                locations.remove(i);
            }
        }
        if (locations.size() > 1) {
            CanvasModel model = canvas.getModel();
            add = new Poly(isClosed, locations);
            canvas.doAction(new ModelAddAction(model, add));
            repaintArea(canvas);
        }
        locations.clear();
        return add;
    }

    private void updateMouse(Canvas canvas, int mouseX, int mouseY, int modifiers) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (isActive) {
            int index = locations.size() - 1;
            Location last = locations.get(index);
            Location newLast;
            if ((modifiers & MouseEvent.SHIFT_DOWN_MASK) != 0 && index > 0) {
                Location nextLast = locations.get(index - 1);
                newLast = LineUtil.snapTo8Cardinals(nextLast, mouseX, mouseY);
            } else {
                newLast = Location.create(mouseX, mouseY);
            }
            if ((modifiers & MouseEvent.CTRL_DOWN_MASK) != 0) {
                int lastX = newLast.getX();
                int lastY = newLast.getY();
                lastX = canvas.snapX(lastX);
                lastY = canvas.snapY(lastY);
                newLast = Location.create(lastX, lastY);
            }

            if (!newLast.equals(last)) {
                locations.set(index, newLast);
                repaintArea(canvas);
            }
        }
    }

    private void repaintArea(Canvas canvas) {
        canvas.repaint();
    }

    @Override
    public void draw(Canvas canvas, Graphics graphics) {
        if (isActive) {
            graphics.setColor(Color.GRAY);
            int size = locations.size();
            int[] xs = new int[size];
            int[] ys = new int[size];
            for (int i = 0; i < size; i++) {
                Location location = locations.get(i);
                xs[i] = location.getX();
                ys[i] = location.getY();
            }
            graphics.drawPolyline(xs, ys, size);
            int lastX = xs[xs.length - 1];
            int lastY = ys[ys.length - 1];
            graphics.fillOval(lastX - 2, lastY - 2, 4, 4);
        }
    }
}
