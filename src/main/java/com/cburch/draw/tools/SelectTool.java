/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.tools;

import com.cburch.draw.actions.ModelMoveHandleAction;
import com.cburch.draw.actions.ModelRemoveAction;
import com.cburch.draw.actions.ModelTranslateAction;
import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.canvas.Selection;
import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.swing.Icon;

public class SelectTool extends AbstractTool {

    private static final int IDLE = 0;
    private static final int MOVE_ALL = 1;
    private static final int RECTANGLE_SELECT = 2;
    private static final int RECTANGLE_TOGGLE = 3;
    private static final int MOVE_HANDLE = 4;

    private static final int DRAG_TOLERANCE = 2;
    private static final int HANDLE_SIZE = 8;

    private static final Color RECTANGLE_SELECT_BACKGROUND = new Color(0, 0, 0, 32);

    private int currentAction;
    private List<CanvasObject> beforePressSelection;
    private Handle beforePressHandle;
    private Location dragStart;
    private Location dragEnd;
    private boolean isDragEffective;
    private int lastMouseX;
    private int lastMouseY;
    private HandleGesture currentGesture;

    public SelectTool() {
        currentAction = IDLE;
        dragStart = Location.create(0, 0);
        dragEnd = dragStart;
        isDragEffective = false;
    }

    private static CanvasObject getObjectAt(CanvasModel model, int x, int y, boolean assumeFilled) {
        Location location = Location.create(x, y);
        for (CanvasObject object : model.getObjectsFromTop()) {
            if (object.contains(location, assumeFilled)) {
                return object;
            }
        }
        return null;
    }

    @Override
    public Icon getIcon() {
        return Icons.getIcon("select.gif");
    }

    @Override
    public Cursor getCursor(Canvas canvas) {
        return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    }

    @Override
    public List<Attribute<?>> getAttributes() {
        return Collections.emptyList();
    }

    @Override
    public void toolSelected(Canvas canvas) {
        currentAction = IDLE;
        canvas.getSelection().clearSelected();
        repaintArea(canvas);
    }

    @Override
    public void toolDeselected(Canvas canvas) {
        currentAction = IDLE;
        canvas.getSelection().clearSelected();
        repaintArea(canvas);
    }

    private int getHandleSize(Canvas canvas) {
        double zoom = canvas.getZoomFactor();
        return (int) Math.ceil(HANDLE_SIZE / Math.sqrt(zoom));
    }

    @Override
    public void mousePressed(Canvas canvas, MouseEvent event) {
        beforePressSelection = new ArrayList<>(canvas.getSelection().getSelected());
        beforePressHandle = canvas.getSelection().getSelectedHandle();
        int mouseX = event.getX();
        int mouseY = event.getY();
        boolean shift = (event.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;
        dragStart = Location.create(mouseX, mouseY);
        isDragEffective = false;
        dragEnd = dragStart;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        Selection selection = canvas.getSelection();
        selection.setHandleSelected(null);

        // see whether user is pressing within an existing handle
        int halfSize = getHandleSize(canvas) / 2;
        CanvasObject clicked = null;
        for (CanvasObject shape : selection.getSelected()) {
            List<Handle> handles = shape.getHandles(null);
            for (Handle han : handles) {
                int dx = han.getX() - mouseX;
                int dy = han.getY() - mouseY;
                if (dx >= -halfSize && dx <= halfSize
                    && dy >= -halfSize && dy <= halfSize) {
                    if (shape.canMoveHandle(han)) {
                        currentAction = MOVE_HANDLE;
                        currentGesture = new HandleGesture(han, 0, 0,
                            event.getModifiersEx());
                        repaintArea(canvas);
                        return;
                    } else if (clicked == null) {
                        clicked = shape;
                    }
                }
            }
        }

        // see whether the user is clicking within a shape
        if (clicked == null) {
            clicked = getObjectAt(canvas.getModel(), event.getX(), event.getY(), false);
        }
        if (clicked != null) {
            if (shift && selection.isSelected(clicked)) {
                selection.setSelected(clicked, false);
                currentAction = IDLE;
            } else {
                if (!shift && !selection.isSelected(clicked)) {
                    selection.clearSelected();
                }
                selection.setSelected(clicked, true);
                selection.setMovingShapes(selection.getSelected(), 0, 0);
                currentAction = MOVE_ALL;
            }
            repaintArea(canvas);
            return;
        }

        clicked = getObjectAt(canvas.getModel(), event.getX(), event.getY(), true);
        if (clicked != null && selection.isSelected(clicked)) {
            if (shift) {
                selection.setSelected(clicked, false);
                currentAction = IDLE;
            } else {
                selection.setMovingShapes(selection.getSelected(), 0, 0);
                currentAction = MOVE_ALL;
            }
            repaintArea(canvas);
            return;
        }

        if (shift) {
            currentAction = RECTANGLE_TOGGLE;
        } else {
            selection.clearSelected();
            currentAction = RECTANGLE_SELECT;
        }
        repaintArea(canvas);
    }

    @Override
    public void cancelMousePress(Canvas canvas) {
        List<CanvasObject> before = beforePressSelection;
        Handle handle = beforePressHandle;
        beforePressSelection = null;
        beforePressHandle = null;
        if (before != null) {
            currentAction = IDLE;
            Selection sel = canvas.getSelection();
            sel.clearDrawsSuppressed();
            sel.setMovingShapes(Collections.emptySet(), 0, 0);
            sel.clearSelected();
            sel.setSelected(before, true);
            sel.setHandleSelected(handle);
            repaintArea(canvas);
        }
    }

    @Override
    public void mouseDragged(Canvas canvas, MouseEvent event) {
        setMouse(canvas, event.getX(), event.getY(), event.getModifiersEx());
    }

    @Override
    public void mouseReleased(Canvas canvas, MouseEvent event) {
        beforePressSelection = null;
        beforePressHandle = null;
        setMouse(canvas, event.getX(), event.getY(), event.getModifiersEx());

        CanvasModel model = canvas.getModel();
        Selection selection = canvas.getSelection();
        Set<CanvasObject> selected = selection.getSelected();
        int action = currentAction;
        currentAction = IDLE;

        if (!isDragEffective) {
            Location location = dragEnd;
            CanvasObject object = getObjectAt(model, location.getX(), location.getY(), false);
            if (object != null) {
                Handle handle = object.canDeleteHandle(location);
                if (handle != null) {
                    selection.setHandleSelected(handle);
                } else {
                    handle = object.canInsertHandle(location);
                    if (handle != null) {
                        selection.setHandleSelected(handle);
                    }
                }
            }
        }

        Location start = dragStart;
        int x1 = event.getX();
        int y1 = event.getY();
        switch (action) {
            case MOVE_ALL:
                Location moveDelta = selection.getMovingDelta();
                if (isDragEffective && !moveDelta.equals(Location.create(0, 0))) {
                    canvas.doAction(new ModelTranslateAction(model, selected, moveDelta.getX(), moveDelta.getY()));
                }
                break;
            case MOVE_HANDLE:
                HandleGesture gesture = currentGesture;
                currentGesture = null;
                if (isDragEffective && gesture != null) {
                    ModelMoveHandleAction handleAction;
                    handleAction = new ModelMoveHandleAction(model, gesture);
                    canvas.doAction(handleAction);
                    Handle result = handleAction.getNewHandle();
                    if (result != null) {
                        Handle handle = result.getObject().canDeleteHandle(result.getLocation());
                        selection.setHandleSelected(handle);
                    }
                }
                break;
            case RECTANGLE_SELECT:
                if (isDragEffective) {
                    Bounds bounds = Bounds.create(start).add(x1, y1);
                    selection.setSelected(canvas.getModel().getObjectsIn(bounds), true);
                } else {
                    CanvasObject clicked;
                    clicked = getObjectAt(model, start.getX(), start.getY(), true);
                    if (clicked != null) {
                        selection.clearSelected();
                        selection.setSelected(clicked, true);
                    }
                }
                break;
            case RECTANGLE_TOGGLE:
                if (isDragEffective) {
                    Bounds bounds = Bounds.create(start).add(x1, y1);
                    selection.toggleSelected(canvas.getModel().getObjectsIn(bounds));
                } else {
                    CanvasObject clickedObject;
                    clickedObject = getObjectAt(model, start.getX(), start.getY(), true);
                    selection.setSelected(clickedObject, !selected.contains(clickedObject));
                }
                break;
        }
        selection.clearDrawsSuppressed();
        repaintArea(canvas);
    }

    @Override
    public void keyPressed(Canvas canvas, KeyEvent event) {
        int code = event.getKeyCode();
        if ((code == KeyEvent.VK_SHIFT || code == KeyEvent.VK_CONTROL || code == KeyEvent.VK_ALT) && currentAction != IDLE) {
            setMouse(canvas, lastMouseX, lastMouseY, event.getModifiersEx());
        }
    }

    @Override
    public void keyReleased(Canvas canvas, KeyEvent event) {
        keyPressed(canvas, event);
    }

    @Override
    public void keyTyped(Canvas canvas, KeyEvent event) {
        char ch = event.getKeyChar();
        Selection selected = canvas.getSelection();
        if ((ch == '\u0008' || ch == '\u007F') && !selected.isEmpty()) {
            ArrayList<CanvasObject> toRemove = new ArrayList<>();
            for (CanvasObject shape : selected.getSelected()) {
                if (shape.canRemove()) {
                    toRemove.add(shape);
                }
            }
            if (!toRemove.isEmpty()) {
                event.consume();
                CanvasModel model = canvas.getModel();
                canvas.doAction(new ModelRemoveAction(model, toRemove));
                selected.clearSelected();
                repaintArea(canvas);
            }
        } else if (ch == '\u001b' && !selected.isEmpty()) {
            selected.clearSelected();
            repaintArea(canvas);
        }
    }

    private void setMouse(Canvas canvas, int mouseX, int mouseY, int modifiers) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        boolean isShift = (modifiers & MouseEvent.SHIFT_DOWN_MASK) != 0;
        boolean isCtrl = (modifiers & InputEvent.CTRL_DOWN_MASK) != 0;
        Location newEnd = Location.create(mouseX, mouseY);
        dragEnd = newEnd;

        Location start = dragStart;
        int deltaX = newEnd.getX() - start.getX();
        int deltaY = newEnd.getY() - start.getY();
        if (!isDragEffective) {
            if (Math.abs(deltaX) + Math.abs(deltaY) > DRAG_TOLERANCE) {
                isDragEffective = true;
            } else {
                return;
            }
        }

        switch (currentAction) {
            case MOVE_HANDLE:
                HandleGesture gesture = currentGesture;
                if (isCtrl) {
                    Handle handle = gesture.getHandle();
                    deltaX = canvas.snapX(handle.getX() + deltaX) - handle.getX();
                    deltaY = canvas.snapY(handle.getY() + deltaY) - handle.getY();
                }
                currentGesture = new HandleGesture(gesture.getHandle(), deltaX, deltaY, modifiers);
                canvas.getSelection().setHandleGesture(currentGesture);
                break;
            case MOVE_ALL:
                if (isCtrl) {
                    int minX = Integer.MAX_VALUE;
                    int minY = Integer.MAX_VALUE;
                    for (CanvasObject object : canvas.getSelection().getSelected()) {
                        for (Handle handle : object.getHandles(null)) {
                            int x = handle.getX();
                            int y = handle.getY();
                            if (x < minX) {
                                minX = x;
                            }
                            if (y < minY) {
                                minY = y;
                            }
                        }
                    }
                    deltaX = canvas.snapX(minX + deltaX) - minX;
                    deltaY = canvas.snapY(minY + deltaY) - minY;
                }
                if (isShift) {
                    if (Math.abs(deltaX) > Math.abs(deltaY)) {
                        deltaY = 0;
                    } else {
                        deltaX = 0;
                    }
                }
                canvas.getSelection().setMovingDelta(deltaX, deltaY);
                break;
        }
        repaintArea(canvas);
    }

    private void repaintArea(Canvas canvas) {
        canvas.repaint();
    }

    @Override
    public void draw(Canvas canvas, Graphics graphics) {
        Selection selection = canvas.getSelection();
        int action = currentAction;

        Location start = dragStart;
        Location end = dragEnd;
        HandleGesture gesture = null;
        boolean drawHandles;
        switch (action) {
            case MOVE_ALL:
                drawHandles = !isDragEffective;
                break;
            case MOVE_HANDLE:
                drawHandles = !isDragEffective;
                if (isDragEffective) {
                    gesture = currentGesture;
                }
                break;
            default:
                drawHandles = true;
        }

        CanvasObject moveHandleObject = null;
        if (gesture != null) {
            moveHandleObject = gesture.getHandle().getObject();
        }
        if (drawHandles) {
            // unscale the coordinate system so that the stroke width isn't scaled
            double zoom = 1.0;
            Graphics graphicsClone = graphics.create();
            if (graphicsClone instanceof Graphics2D) {
                zoom = canvas.getZoomFactor();
                if (zoom != 1.0) {
                    ((Graphics2D) graphicsClone).scale(1.0 / zoom, 1.0 / zoom);
                }
            }
            GraphicsUtil.switchToWidth(graphicsClone, 1);

            int size = (int) Math.ceil(HANDLE_SIZE * Math.sqrt(zoom));
            int offset = size / 2;
            for (CanvasObject object : selection.getSelected()) {
                List<Handle> handles;
                if (action == MOVE_HANDLE && object == moveHandleObject) {
                    handles = object.getHandles(gesture);
                } else {
                    handles = object.getHandles(null);
                }
                for (Handle handle : handles) {
                    int x = handle.getX();
                    int y = handle.getY();
                    if (action == MOVE_ALL && isDragEffective) {
                        Location delta = selection.getMovingDelta();
                        x += delta.getX();
                        y += delta.getY();
                    }
                    x = (int) Math.round(zoom * x);
                    y = (int) Math.round(zoom * y);
                    graphicsClone.clearRect(x - offset, y - offset, size, size);
                    graphicsClone.drawRect(x - offset, y - offset, size, size);
                }
            }
            Handle selectedHandle = selection.getSelectedHandle();
            if (selectedHandle != null) {
                int x = selectedHandle.getX();
                int y = selectedHandle.getY();
                if (action == MOVE_ALL && isDragEffective) {
                    Location delta = selection.getMovingDelta();
                    x += delta.getX();
                    y += delta.getY();
                }
                x = (int) Math.round(zoom * x);
                y = (int) Math.round(zoom * y);
                int[] xs = {x - offset, x, x + offset, x};
                int[] ys = {y, y - offset, y, y + offset};
                graphicsClone.setColor(Color.WHITE);
                graphicsClone.fillPolygon(xs, ys, 4);
                graphicsClone.setColor(Color.BLACK);
                graphicsClone.drawPolygon(xs, ys, 4);
            }
        }

        switch (action) {
            case RECTANGLE_SELECT:
            case RECTANGLE_TOGGLE:
                if (isDragEffective) {
                    // find rectangle currently to show
                    int x0 = start.getX();
                    int y0 = start.getY();
                    int x1 = end.getX();
                    int y1 = end.getY();
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

                    // make the region that's not being selected darker
                    int width = canvas.getWidth();
                    int height = canvas.getHeight();
                    graphics.setColor(RECTANGLE_SELECT_BACKGROUND);
                    graphics.fillRect(0, 0, width, y0);
                    graphics.fillRect(0, y0, x0, y1 - y0);
                    graphics.fillRect(x1, y0, width - x1, y1 - y0);
                    graphics.fillRect(0, y1, width, height - y1);

                    // now draw the rectangle
                    graphics.setColor(Color.GRAY);
                    graphics.drawRect(x0, y0, x1 - x0, y1 - y0);
                }
                break;
        }
    }
}
