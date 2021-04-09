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
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.Icon;

public class LineTool extends AbstractTool {

    private final DrawingAttributeSet attributeSet;
    private boolean isActive;
    private Location mouseStart;
    private Location mouseEnd;
    private int lastMouseX;
    private int lastMouseY;

    public LineTool(DrawingAttributeSet attributeSet) {
        this.attributeSet = attributeSet;
        isActive = false;
    }

    static Location snapTo4Cardinals(Location from, int mouseX, int mouseY) {
        int px = from.getX();
        int py = from.getY();
        if (mouseX != px && mouseY != py) {
            if (Math.abs(mouseY - py) < Math.abs(mouseX - px)) {
                return Location.create(mouseX, py);
            } else {
                return Location.create(px, mouseY);
            }
        }
        return Location.create(mouseX, mouseY); // should never happen
    }

    @Override
    public Icon getIcon() {
        return Icons.getIcon("drawline.gif");
    }

    @Override
    public Cursor getCursor(Canvas canvas) {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    @Override
    public List<Attribute<?>> getAttributes() {
        return DrawAttr.ATTRIBUTES_STROKE;
    }

    @Override
    public void toolDeselected(Canvas canvas) {
        isActive = false;
        repaintArea(canvas);
    }

    @Override
    public void mousePressed(Canvas canvas, MouseEvent event) {
        int x = event.getX();
        int y = event.getY();
        int modifiers = event.getModifiersEx();
        if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0) {
            x = canvas.snapX(x);
            y = canvas.snapY(y);
        }
        Location location = Location.create(x, y);
        mouseStart = location;
        mouseEnd = location;
        lastMouseX = location.getX();
        lastMouseY = location.getY();
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
            Location mouseStart = this.mouseStart;
            Location mouseEnd = this.mouseEnd;
            CanvasObject add = null;
            if (!mouseStart.equals(mouseEnd)) {
                isActive = false;
                CanvasModel model = canvas.getModel();
                Location[] ends = {mouseStart, mouseEnd};
                List<Location> locations = UnmodifiableList.create(ends);
                add = attributeSet.applyTo(new Poly(false, locations));
                add.setValue(DrawAttr.PAINT_TYPE, DrawAttr.PAINT_STROKE);
                canvas.doAction(new ModelAddAction(model, add));
                repaintArea(canvas);
            }
            canvas.toolGestureComplete(this, add);
        }
    }

    @Override
    public void keyPressed(Canvas canvas, KeyEvent event) {
        int code = event.getKeyCode();
        if (isActive && (code == KeyEvent.VK_SHIFT || code == KeyEvent.VK_CONTROL)) {
            updateMouse(canvas, lastMouseX, lastMouseY, event.getModifiersEx());
        }
    }

    @Override
    public void keyReleased(Canvas canvas, KeyEvent event) {
        keyPressed(canvas, event);
    }

    private void updateMouse(Canvas canvas, int mouseX, int mouseY, int modifiers) {
        if (isActive) {
            boolean isShiftDown = (modifiers & MouseEvent.SHIFT_DOWN_MASK) != 0;
            Location newEnd;
            if (isShiftDown) {
                newEnd = LineUtil.snapTo8Cardinals(mouseStart, mouseX, mouseY);
            } else {
                newEnd = Location.create(mouseX, mouseY);
            }

            if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0) {
                int x = newEnd.getX();
                int y = newEnd.getY();
                x = canvas.snapX(x);
                y = canvas.snapY(y);
                newEnd = Location.create(x, y);
            }

            if (!newEnd.equals(mouseEnd)) {
                mouseEnd = newEnd;
                repaintArea(canvas);
            }
        }
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    private void repaintArea(Canvas canvas) {
        canvas.repaint();
    }

    @Override
    public void draw(Canvas canvas, Graphics graphics) {
        if (isActive) {
            Location mouseStart = this.mouseStart;
            Location mouseEnd = this.mouseEnd;
            graphics.setColor(Color.GRAY);
            graphics.drawLine(mouseStart.getX(), mouseStart.getY(), mouseEnd.getX(), mouseEnd.getY());
        }
    }
}
