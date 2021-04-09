/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.tools;

import com.cburch.draw.actions.ModelAddAction;
import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.shapes.Curve;
import com.cburch.draw.shapes.CurveUtil;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.draw.shapes.LineUtil;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.Icons;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.Icon;

public class CurveTool extends AbstractTool {

    private static final int BEFORE_CREATION = 0;
    private static final int ENDPOINT_DRAG = 1;
    private static final int CONTROL_DRAG = 2;

    private final DrawingAttributeSet attributes;
    private int state;
    private Location end0;
    private Location end1;
    private Curve currentCurve;
    private boolean isMouseDown;
    private int lastMouseX;
    private int lastMouseY;

    public CurveTool(DrawingAttributeSet attributes) {
        this.attributes = attributes;
        state = BEFORE_CREATION;
        isMouseDown = false;
    }

    @Override
    public Icon getIcon() {
        return Icons.getIcon("drawcurv.gif");
    }

    @Override
    public Cursor getCursor(Canvas canvas) {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    @Override
    public void toolDeselected(Canvas canvas) {
        state = BEFORE_CREATION;
        repaintArea(canvas);
    }

    @Override
    public void mousePressed(Canvas canvas, MouseEvent event) {
        int mouseX = event.getX();
        int mouseY = event.getY();
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        isMouseDown = true;
        int mods = event.getModifiersEx();
        if ((mods & InputEvent.CTRL_DOWN_MASK) != 0) {
            mouseX = canvas.snapX(mouseX);
            mouseY = canvas.snapY(mouseY);
        }

        switch (state) {
            case BEFORE_CREATION:
            case CONTROL_DRAG:
                end0 = Location.create(mouseX, mouseY);
                end1 = end0;
                state = ENDPOINT_DRAG;
                break;
            case ENDPOINT_DRAG:
                currentCurve = new Curve(end0, end1, Location.create(mouseX, mouseY));
                state = CONTROL_DRAG;
                break;
        }
        repaintArea(canvas);
    }

    @Override
    public void mouseDragged(Canvas canvas, MouseEvent event) {
        updateMouse(canvas, event.getX(), event.getY(), event.getModifiersEx());
        repaintArea(canvas);
    }

    @Override
    public void mouseReleased(Canvas canvas, MouseEvent event) {
        Curve curve = updateMouse(canvas, event.getX(), event.getY(), event.getModifiersEx());
        isMouseDown = false;
        if (state == CONTROL_DRAG) {
            if (curve != null) {
                attributes.applyTo(curve);
                CanvasModel model = canvas.getModel();
                canvas.doAction(new ModelAddAction(model, curve));
                canvas.toolGestureComplete(this, curve);
            }
            state = BEFORE_CREATION;
        }
        repaintArea(canvas);
    }

    @Override
    public void keyPressed(Canvas canvas, KeyEvent event) {
        int code = event.getKeyCode();
        if (isMouseDown && (code == KeyEvent.VK_SHIFT || code == KeyEvent.VK_CONTROL || code == KeyEvent.VK_ALT)) {
            updateMouse(canvas, lastMouseX, lastMouseY, event.getModifiersEx());
            repaintArea(canvas);
        }
    }

    @Override
    public void keyReleased(Canvas canvas, KeyEvent event) {
        keyPressed(canvas, event);
    }

    @Override
    public void keyTyped(Canvas canvas, KeyEvent event) {
        char c = event.getKeyChar();
        if (c == '\u001b') { // escape key
            state = BEFORE_CREATION;
            repaintArea(canvas);
            canvas.toolGestureComplete(this, null);
        }
    }

    private Curve updateMouse(Canvas canvas, int mouseX, int mouseY, int mods) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        boolean isShiftDown = (mods & MouseEvent.SHIFT_DOWN_MASK) != 0;
        boolean isCtrlDown = (mods & MouseEvent.CTRL_DOWN_MASK) != 0;
        boolean isAltDown = (mods & MouseEvent.ALT_DOWN_MASK) != 0;
        Curve curve = null;
        switch (state) {
            case ENDPOINT_DRAG:
                if (isMouseDown) {
                    if (isShiftDown) {
                        Location point = LineUtil.snapTo8Cardinals(end0, mouseX, mouseY);
                        mouseX = point.getX();
                        mouseY = point.getY();
                    }
                    if (isCtrlDown) {
                        mouseX = canvas.snapX(mouseX);
                        mouseY = canvas.snapY(mouseY);
                    }
                    end1 = Location.create(mouseX, mouseY);
                }
                break;
            case CONTROL_DRAG:
                if (isMouseDown) {
                    int cx = mouseX;
                    int cy = mouseY;
                    if (isCtrlDown) {
                        cx = canvas.snapX(cx);
                        cy = canvas.snapY(cy);
                    }
                    if (isShiftDown) {
                        double x0 = end0.getX();
                        double y0 = end0.getY();
                        double x1 = end1.getX();
                        double y1 = end1.getY();
                        double midPointX = (x0 + x1) / 2;
                        double midPointY = (y0 + y1) / 2;
                        double deltaX = x1 - x0;
                        double deltaY = y1 - y0;
                        double[] p = LineUtil
                            .nearestPointInfinite(cx, cy, midPointX, midPointY, midPointX - deltaY, midPointY + deltaX);
                        cx = (int) Math.round(p[0]);
                        cy = (int) Math.round(p[1]);
                    }
                    if (isAltDown) {
                        double[] e0 = {end0.getX(), end0.getY()};
                        double[] e1 = {end1.getX(), end1.getY()};
                        double[] midPoints = {cx, cy};
                        double[] ct = CurveUtil.interpolate(e0, e1, midPoints);
                        cx = (int) Math.round(ct[0]);
                        cy = (int) Math.round(ct[1]);
                    }
                    curve = new Curve(end0, end1, Location.create(cx, cy));
                    currentCurve = curve;
                }
                break;
        }
        return curve;
    }

    private void repaintArea(Canvas canvas) {
        canvas.repaint();
    }

    @Override
    public List<Attribute<?>> getAttributes() {
        return DrawAttr.getFillAttributes(attributes.getValue(DrawAttr.PAINT_TYPE));
    }

    @Override
    public void draw(Canvas canvas, Graphics graphics) {
        graphics.setColor(Color.GRAY);
        switch (state) {
            case ENDPOINT_DRAG:
                graphics.drawLine(end0.getX(), end0.getY(), end1.getX(), end1.getY());
                break;
            case CONTROL_DRAG:
                ((Graphics2D) graphics).draw(currentCurve.getCurve2D());
                break;
        }
    }
}
