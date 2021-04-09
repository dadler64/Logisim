/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.memory;

import com.adlerd.logger.Logger;
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

public class ShiftRegisterPoker extends InstancePoker {

    private int location;

    @Override
    public boolean init(InstanceState state, MouseEvent e) {
        location = computeStage(state, e);
        return location >= 0;
    }

    private int computeStage(InstanceState state, MouseEvent e) {
        Integer lengthObject = state.getAttributeValue(ShiftRegister.ATTR_LENGTH);
        BitWidth widthObject = state.getAttributeValue(StdAttr.WIDTH);
        Boolean loadObject = state.getAttributeValue(ShiftRegister.ATTR_LOAD);
        Bounds bounds = state.getInstance().getBounds();

        int y = bounds.getY();
        String label = state.getAttributeValue(StdAttr.LABEL);
        if (label == null || label.equals("")) {
            y += bounds.getHeight() / 2;
        } else {
            y += 3 * bounds.getHeight() / 4;
        }
        y = e.getY() - y;
        if (y <= -6 || y >= 8) {
            return -1;
        }

        int x = e.getX() - (bounds.getX() + 15);
        if (!loadObject || widthObject.getWidth() > 4) {
            return -1;
        }
        if (x < 0 || x >= lengthObject * 10) {
            return -1;
        }
        return x / 10;
    }

    @Override
    public void paint(InstancePainter painter) {
        int location = this.location;
        if (location < 0) {
            return;
        }
        Bounds bounds = painter.getInstance().getBounds();
        int x = bounds.getX() + 15 + location * 10;
        int y = bounds.getY();
        String label = painter.getAttributeValue(StdAttr.LABEL);
        if (label == null || label.equals("")) {
            y += bounds.getHeight() / 2;
        } else {
            y += 3 * bounds.getHeight() / 4;
        }
        Graphics g = painter.getGraphics();
        g.setColor(Color.RED);
        g.drawRect(x, y - 6, 10, 13);
    }

    @Override
    public void mousePressed(InstanceState state, MouseEvent e) {
        location = computeStage(state, e);
    }

    @Override
    public void mouseReleased(InstanceState state, MouseEvent e) {
        int oldLocation = location;
        if (oldLocation < 0) {
            return;
        }
        BitWidth widthObject = state.getAttributeValue(StdAttr.WIDTH);
        if (widthObject.equals(BitWidth.ONE)) {
            int newLocation = computeStage(state, e);
            if (oldLocation == newLocation) {
                ShiftRegisterData data = (ShiftRegisterData) state.getData();
                int i = data.getLength() - 1 - location;
                Value value = data.get(i);
                if (value == Value.FALSE) {
                    value = Value.TRUE;
                } else {
                    value = Value.FALSE;
                }
                data.set(i, value);
                state.fireInvalidated();
            }
        }
    }

    @Override
    public void keyTyped(InstanceState state, KeyEvent e) {
        int location = this.location;
        if (location < 0) {
            return;
        }
        char c = e.getKeyChar();
        if (c == ' ') {
            Integer lengthObject = state.getAttributeValue(ShiftRegister.ATTR_LENGTH);
            if (location < lengthObject - 1) {
                this.location = location + 1;
                state.fireInvalidated();
            }
        } else if (c == '\u0008') {
            if (location > 0) {
                this.location = location - 1;
                state.fireInvalidated();
            }
        } else {
            try {
                int value = Integer.parseInt("" + e.getKeyChar(), 16);
                BitWidth widthObject = state.getAttributeValue(StdAttr.WIDTH);
                if ((value & ~widthObject.getMask()) != 0) {
                    return;
                }
                Value valueObject = Value.createKnown(widthObject, value);
                ShiftRegisterData data = (ShiftRegisterData) state.getData();
                int index = data.getLength() - 1 - location;
                if (!data.get(index).equals(valueObject)) {
                    data.set(index, valueObject);
                    state.fireInvalidated();
                }
            } catch (NumberFormatException ex) {
                Logger.debugln(ex.getMessage());
            }
        }
    }
}
