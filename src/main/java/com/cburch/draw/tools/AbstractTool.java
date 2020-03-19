/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.tools;

import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.canvas.CanvasTool;
import com.cburch.logisim.data.Attribute;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.Icon;

public abstract class AbstractTool extends CanvasTool {

    public static AbstractTool[] getTools(DrawingAttributeSet attributes) {
        return new AbstractTool[]{
                new SelectTool(),
                new LineTool(attributes),
                new CurveTool(attributes),
                new PolyTool(false, attributes),
                new RectangleTool(attributes),
                new RoundRectangleTool(attributes),
                new OvalTool(attributes),
                new PolyTool(true, attributes),
        };
    }

    public abstract Icon getIcon();

    public abstract List<Attribute<?>> getAttributes();

    public String getDescription() {
        return null;
    }

    //
    // CanvasTool methods
    //
    @Override
    public abstract Cursor getCursor(Canvas canvas);

    @Override
    public void toolSelected(Canvas canvas) {
    }

    @Override
    public void toolDeselected(Canvas canvas) {
    }

    @Override
    public void mouseMoved(Canvas canvas, MouseEvent event) {
    }

    @Override
    public void mousePressed(Canvas canvas, MouseEvent event) {
    }

    @Override
    public void mouseDragged(Canvas canvas, MouseEvent event) {
    }

    @Override
    public void mouseReleased(Canvas canvas, MouseEvent event) {
    }

    @Override
    public void mouseEntered(Canvas canvas, MouseEvent event) {
    }

    @Override
    public void mouseExited(Canvas canvas, MouseEvent event) {
    }

    /**
     * This is because a popup menu may result from the subsequent mouse release
     */
    @Override
    public void cancelMousePress(Canvas canvas) {
    }

    @Override
    public void keyPressed(Canvas canvas, KeyEvent event) {
    }

    @Override
    public void keyReleased(Canvas canvas, KeyEvent event) {
    }

    @Override
    public void keyTyped(Canvas canvas, KeyEvent event) {
    }

    @Override
    public void draw(Canvas canvas, Graphics graphics) {
    }
}
