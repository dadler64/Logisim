/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.tools;

import com.cburch.draw.actions.ModelAddAction;
import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

abstract class RectangularTool extends AbstractTool {

    private boolean isActive;
    private Location dragStart;
    private int lastMouseX;
    private int lastMouseY;
    private Bounds currentBounds;

    public RectangularTool() {
        isActive = false;
        currentBounds = Bounds.EMPTY_BOUNDS;
    }

    public abstract CanvasObject createShape(int x, int y, int width, int height);

    public abstract void drawShape(Graphics graphics, int x, int y, int width, int height);

    public abstract void fillShape(Graphics graphics, int x, int y, int width, int height);

    @Override
    public Cursor getCursor(Canvas canvas) {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    @Override
    public void toolDeselected(Canvas canvas) {
        Bounds bounds = currentBounds;
        isActive = false;
        repaintArea(canvas, bounds);
    }

    @Override
    public void mousePressed(Canvas canvas, MouseEvent event) {
        Location location = Location.create(event.getX(), event.getY());
        Bounds bounds = Bounds.create(location);
        dragStart = location;
        lastMouseX = location.getX();
        lastMouseY = location.getY();
        isActive = canvas.getModel() != null;
        repaintArea(canvas, bounds);
    }

    @Override
    public void mouseDragged(Canvas canvas, MouseEvent event) {
        updateMouse(canvas, event.getX(), event.getY(), event.getModifiersEx());
    }

    @Override
    public void mouseReleased(Canvas canvas, MouseEvent event) {
        if (isActive) {
            Bounds oldBounds = currentBounds;
            Bounds bounds = computeBounds(canvas, event.getX(), event.getY(), event.getModifiersEx());
            currentBounds = Bounds.EMPTY_BOUNDS;
            isActive = false;
            CanvasObject add = null;
            if (bounds.getWidth() != 0 && bounds.getHeight() != 0) {
                CanvasModel model = canvas.getModel();
                add = createShape(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
                canvas.doAction(new ModelAddAction(model, add));
                repaintArea(canvas, oldBounds.add(bounds));
            }
            canvas.toolGestureComplete(this, add);
        }
    }

    @Override
    public void keyPressed(Canvas canvas, KeyEvent event) {
        int code = event.getKeyCode();
        if (isActive && (code == KeyEvent.VK_SHIFT || code == KeyEvent.VK_ALT || code == KeyEvent.VK_CONTROL)) {
            updateMouse(canvas, lastMouseX, lastMouseY, event.getModifiersEx());
        }
    }

    @Override
    public void keyReleased(Canvas canvas, KeyEvent event) {
        keyPressed(canvas, event);
    }

    private void updateMouse(Canvas canvas, int mouseX, int mouseY, int mods) {
        Bounds oldBounds = currentBounds;
        Bounds bounds = computeBounds(canvas, mouseX, mouseY, mods);
        if (!bounds.equals(oldBounds)) {
            currentBounds = bounds;
            repaintArea(canvas, oldBounds.add(bounds));
        }
    }

    private Bounds computeBounds(Canvas canvas, int mouseX, int mouseY, int mods) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (!isActive) {
            return Bounds.EMPTY_BOUNDS;
        } else {
            Location start = dragStart;
            int x0 = start.getX();
            int y0 = start.getY();
            int x1 = mouseX;
            int y1 = mouseY;
            if (x0 == x1 && y0 == y1) {
                return Bounds.EMPTY_BOUNDS;
            }

            boolean isCtrlDown = (mods & MouseEvent.CTRL_DOWN_MASK) != 0;
            if (isCtrlDown) {
                x0 = canvas.snapX(x0);
                y0 = canvas.snapY(y0);
                x1 = canvas.snapX(x1);
                y1 = canvas.snapY(y1);
            }

            boolean isAltDown = (mods & MouseEvent.ALT_DOWN_MASK) != 0;
            boolean isShiftDown = (mods & MouseEvent.SHIFT_DOWN_MASK) != 0;
            if (isAltDown) {
                if (isShiftDown) {
                    int r = Math.min(Math.abs(x0 - x1), Math.abs(y0 - y1));
                    x1 = x0 + r;
                    y1 = y0 + r;
                    x0 -= r;
                    y0 -= r;
                } else {
                    x0 = x0 - (x1 - x0);
                    y0 = y0 - (y1 - y0);
                }
            } else {
                if (isShiftDown) {
                    int r = Math.min(Math.abs(x0 - x1), Math.abs(y0 - y1));
                    y1 = y1 < y0 ? y0 - r : y0 + r;
                    x1 = x1 < x0 ? x0 - r : x0 + r;
                }
            }

            int x = x0;
            int y = y0;
            int width = x1 - x0;
            int height = y1 - y0;
            if (width < 0) {
                x = x1;
                width = -width;
            }
            if (height < 0) {
                y = y1;
                height = -height;
            }
            return Bounds.create(x, y, width, height);
        }
    }

    private void repaintArea(Canvas canvas, Bounds bounds) {
        canvas.repaint();
		/* The below doesn't work because Java doesn't deal correctly with stroke
		 * widths that go outside the clip area
		canvas.repaintCanvasCoords(bounds.getX() - 10, bounds.getY() - 10,
				bounds.getWidth() + 20, bounds.getHeight() + 20);
		 */
    }

    @Override
    public void draw(Canvas canvas, Graphics graphics) {
        Bounds currentBounds = this.currentBounds;
        if (isActive && currentBounds != null && currentBounds != Bounds.EMPTY_BOUNDS) {
            graphics.setColor(Color.GRAY);
            drawShape(graphics, currentBounds.getX(), currentBounds.getY(), currentBounds.getWidth(),
                    currentBounds.getHeight());
        }
    }

}
