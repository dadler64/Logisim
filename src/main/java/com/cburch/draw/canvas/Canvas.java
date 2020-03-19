/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.canvas;

import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.undo.Action;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;


public class Canvas extends JComponent {

    public static final String TOOL_PROPERTY = "tool";
    public static final String MODEL_PROPERTY = "model";

    private CanvasModel model;
    private ActionDispatcher dispatcher;
    private CanvasListener listener;
    private Selection selection;

    public Canvas() {
        model = null;
        listener = new CanvasListener(this);
        selection = new Selection();

        addMouseListener(listener);
        addMouseMotionListener(listener);
        addKeyListener(listener);
        setPreferredSize(new Dimension(200, 200));
    }

    public CanvasModel getModel() {
        return model;
    }

    public CanvasTool getTool() {
        return listener.getTool();
    }

    public void setTool(CanvasTool tool) {
        CanvasTool oldTool = listener.getTool();
        if (tool != oldTool) {
            listener.setTool(tool);
            firePropertyChange(TOOL_PROPERTY, oldTool, tool);
        }
    }

    public void toolGestureComplete(CanvasTool tool, CanvasObject created) {
    } // nothing to do - subclass may override

    @SuppressWarnings("UnusedReturnValue")
    protected JPopupMenu showPopupMenu(MouseEvent event, CanvasObject clicked) {
        return null; // subclass will override if it supports popup menus
    }

    public Selection getSelection() {
        return selection;
    }

    protected void setSelection(Selection selection) {
        this.selection = selection;
        repaint();
    }

    public void doAction(Action action) {
        dispatcher.doAction(action);
    }

    public void setModel(CanvasModel model, ActionDispatcher dispatcher) {
        CanvasModel oldValue = this.model;
        if (oldValue != model) {
            if (oldValue != null) {
                oldValue.removeCanvasModelListener(listener);
            }
            this.model = model;
            this.dispatcher = dispatcher;
            if (model != null) {
                model.addCanvasModelListener(listener);
            }
            selection.clearSelected();
            repaint();
            firePropertyChange(MODEL_PROPERTY, oldValue, model);
        }
    }

    public void repaintCanvasCoords(int x, int y, int width, int height) {
        repaint(x, y, width, height);
    }

    public double getZoomFactor() {
        return 1.0; // subclass will have to override this
    }

    public int snapX(int x) {
        return x; // subclass will have to override this
    }

    public int snapY(int y) {
        return y; // subclass will have to override this
    }

    @Override
    public void paintComponent(Graphics graphics) {
        paintBackground(graphics);
        paintForeground(graphics);
    }

    protected void paintBackground(Graphics graphics) {
        graphics.clearRect(0, 0, getWidth(), getHeight());
    }

    protected void paintForeground(Graphics graphics) {
        CanvasModel model = this.model;
        CanvasTool tool = listener.getTool();
        if (model != null) {
            Graphics graphicsCopy = graphics.create();
            model.paint(graphics, selection);
            graphicsCopy.dispose();
        }
        if (tool != null) {
            Graphics graphicsCopy = graphics.create();
            tool.draw(this, graphicsCopy);
            graphicsCopy.dispose();
        }
    }
}
