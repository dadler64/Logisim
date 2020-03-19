/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.canvas;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public abstract class CanvasTool {

    public abstract Cursor getCursor(Canvas canvas);

    public void draw(Canvas canvas, Graphics graphics) {
    }

    public void toolSelected(Canvas canvas) {
    }

    public void toolDeselected(Canvas canvas) {
    }

    public void mouseMoved(Canvas canvas, MouseEvent event) {
    }

    public void mousePressed(Canvas canvas, MouseEvent event) {
    }

    public void mouseDragged(Canvas canvas, MouseEvent event) {
    }

    public void mouseReleased(Canvas canvas, MouseEvent event) {
    }

    public void mouseEntered(Canvas canvas, MouseEvent event) {
    }

    public void mouseExited(Canvas canvas, MouseEvent event) {
    }

    /**
     * This is because a popup menu may result from the subsequent mouse release
     */
    public void cancelMousePress(Canvas canvas) {
    }

    public void keyPressed(Canvas canvas, KeyEvent event) {
    }

    public void keyReleased(Canvas canvas, KeyEvent event) {
    }

    public void keyTyped(Canvas canvas, KeyEvent event) {
    }

    public void zoomFactorChanged(Canvas canvas) {
    }
}
