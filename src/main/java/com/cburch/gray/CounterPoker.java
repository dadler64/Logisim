/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.gray;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * When the user clicks a counter using the Poke Tool, a CounterPoker object
 * is created, and that object will handle all user events. Note that
 * CounterPoker is a class specific to GrayCounter, and that it must be a
 * subclass of InstancePoker in the com.cburch.logisim.instance package.
 */
public class CounterPoker extends InstancePoker {

    public CounterPoker() {
    }

    /**
     * Determines whether the location the mouse was pressed should result
     * in initiating a poke.
     */
    @Override
    public boolean init(InstanceState state, MouseEvent event) {
        return state.getInstance().getBounds().contains(event.getX(), event.getY());
        // Anywhere in the main rectangle initiates the poke. The user might
        // have clicked within a label, but that will be outside the bounds.
    }

    /**
     * Draws an indicator that the caret is being selected. Here, we'll draw
     * a red rectangle around the value.
     */
    @Override
    public void paint(InstancePainter painter) {
        Bounds bounds = painter.getBounds();
        BitWidth bitWidth = painter.getAttributeValue(StdAttr.WIDTH);
        int length = (bitWidth.getWidth() + 3) / 4;

        Graphics graphics = painter.getGraphics();
        graphics.setColor(Color.RED);
        int width = 7 * length + 2; // width of caret rectangle
        int height = 16; // height of caret rectangle
        graphics.drawRect(bounds.getX() + (bounds.getWidth() - width) / 2, bounds.getY() + (bounds.getHeight() - height) / 2,
            width, height);
        graphics.setColor(Color.BLACK);
    }

    /**
     * Processes a key by just adding it onto the end of the current value.
     */
    @Override
    public void keyTyped(InstanceState state, KeyEvent event) {
        // convert it to a hex digit; if it isn't a hex digit, abort.
        int value = Character.digit(event.getKeyChar(), 16);
        BitWidth bitWidth = state.getAttributeValue(StdAttr.WIDTH);
        if (value < 0 || (value & bitWidth.getMask()) != value) {
            return;
        }

        // compute the next value
        CounterData currentData = CounterData.get(state, bitWidth);
        int newVal = (currentData.getValue().toIntValue() * 16 + value) & bitWidth.getMask();
        Value newValue = Value.createKnown(bitWidth, newVal);
        currentData.setValue(newValue);
        state.fireInvalidated();

        // You might be tempted to propagate the value immediately here, using
        // state.setPort. However, the circuit may currently be propagating in
        // another thread, and invoking setPort directly could interfere with
        // that. Using fireInvalidated notifies the propagation thread to
        // invoke propagate on the counter at its next opportunity.
    }
}
