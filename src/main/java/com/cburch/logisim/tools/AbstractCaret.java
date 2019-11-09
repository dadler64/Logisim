/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.tools;

import com.cburch.logisim.data.Bounds;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AbstractCaret implements Caret {

    private ArrayList<CaretListener> listeners = new ArrayList<CaretListener>();
    private List<CaretListener> listenersView;
    private Bounds bds = Bounds.EMPTY_BOUNDS;

    public AbstractCaret() {
        listenersView = Collections.unmodifiableList(listeners);
    }

    // listener methods
    public void addCaretListener(CaretListener e) {
        listeners.add(e);
    }

    public void removeCaretListener(CaretListener e) {
        listeners.remove(e);
    }

    protected List<CaretListener> getCaretListeners() {
        return listenersView;
    }

    // configuration methods
    public void setBounds(Bounds value) {
        bds = value;
    }

    // query/Graphics methods
    public String getText() {
        return "";
    }

    public Bounds getBounds(Graphics g) {
        return bds;
    }

    public void draw(Graphics g) {
    }

    // finishing
    public void commitText(String text) {
    }

    public void cancelEditing() {
    }

    public void stopEditing() {
    }

    // events to handle
    public void mousePressed(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }
}
