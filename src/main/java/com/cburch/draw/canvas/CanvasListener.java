/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.canvas;

import com.cburch.draw.model.CanvasModelEvent;
import com.cburch.draw.model.CanvasModelListener;
import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.data.Location;
import java.awt.Cursor;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;

class CanvasListener implements MouseListener, MouseMotionListener, KeyListener, CanvasModelListener {

    private Canvas canvas;
    private CanvasTool tool;

    public CanvasListener(Canvas canvas) {
        this.canvas = canvas;
        tool = null;
    }

    public CanvasTool getTool() {
        return tool;
    }

    public void setTool(CanvasTool tool) {
        CanvasTool oldTool = this.tool;
        if (tool != oldTool) {
            this.tool = tool;
            if (oldTool != null) {
                oldTool.toolDeselected(canvas);
            }
            if (tool != null) {
                tool.toolSelected(canvas);
                canvas.setCursor(tool.getCursor(canvas));
            } else {
                canvas.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }

    public void mouseMoved(MouseEvent event) {
        if (tool != null) {
            tool.mouseMoved(canvas, event);
        }
    }

    public void mousePressed(MouseEvent event) {
        canvas.requestFocus();
        if (event.isPopupTrigger()) {
            handlePopupTrigger(event);
        } else if (event.getButton() == 1) {
            if (tool != null) {
                tool.mousePressed(canvas, event);
            }
        }
    }

    public void mouseDragged(MouseEvent event) {
        if (isButton1(event)) {
            if (tool != null) {
                tool.mouseDragged(canvas, event);
            }
        } else {
            if (tool != null) {
                tool.mouseMoved(canvas, event);
            }
        }
    }

    public void mouseReleased(MouseEvent event) {
        if (event.isPopupTrigger()) {
            if (tool != null) {
                tool.cancelMousePress(canvas);
            }
            handlePopupTrigger(event);
        } else if (event.getButton() == 1) {
            if (tool != null) {
                tool.mouseReleased(canvas, event);
            }
        }
    }

    public void mouseClicked(MouseEvent event) {
    }

    public void mouseEntered(MouseEvent event) {
        if (tool != null) {
            tool.mouseEntered(canvas, event);
        }
    }

    public void mouseExited(MouseEvent event) {
        if (tool != null) {
            tool.mouseExited(canvas, event);
        }
    }

    public void keyPressed(KeyEvent event) {
        if (tool != null) {
            tool.keyPressed(canvas, event);
        }
    }

    public void keyReleased(KeyEvent event) {
        if (tool != null) {
            tool.keyReleased(canvas, event);
        }
    }

    public void keyTyped(KeyEvent event) {
        if (tool != null) {
            tool.keyTyped(canvas, event);
        }
    }

    public void modelChanged(CanvasModelEvent event) {
        canvas.getSelection().modelChanged(event);
        canvas.repaint();
    }

    private boolean isButton1(MouseEvent event) {
        return (event.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0;
    }

    private void handlePopupTrigger(MouseEvent event) {
        Location location = Location.create(event.getX(), event.getY());
        List<CanvasObject> objects = canvas.getModel().getObjectsFromTop();
        CanvasObject canvasObject = null;
        for (CanvasObject object : objects) {
            if (object.contains(location, false)) {
                canvasObject = object;
                break;
            }
        }
        if (canvasObject == null) {
            for (CanvasObject object : objects) {
                if (object.contains(location, true)) {
                    canvasObject = object;
                    break;
                }
            }
        }
        canvas.showPopupMenu(event, canvasObject);
    }
}
