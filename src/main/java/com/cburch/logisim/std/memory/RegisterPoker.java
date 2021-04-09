/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class RegisterPoker extends InstancePoker {

    private int initialValue;
    private int currentValue;

    @Override
    public boolean init(InstanceState state, MouseEvent e) {
        RegisterData data = (RegisterData) state.getData();
        if (data == null) {
            data = new RegisterData();
            state.setData(data);
        }
        initialValue = data.value;
        currentValue = initialValue;
        return true;
    }

    @Override
    public void paint(InstancePainter painter) {
        Bounds bounds = painter.getBounds();
        BitWidth dataWidth = painter.getAttributeValue(StdAttr.WIDTH);
        int width = dataWidth == null ? 8 : dataWidth.getWidth();
        int length = (width + 3) / 4;

        Graphics g = painter.getGraphics();
        g.setColor(Color.RED);
        if (length > 4) {
            g.drawRect(bounds.getX(), bounds.getY() + 3, bounds.getWidth(), 25);
        } else {
            int wid = 7 * length + 2;
            g.drawRect(bounds.getX() + (bounds.getWidth() - wid) / 2, bounds.getY() + 4, wid, 15);
        }
        g.setColor(Color.BLACK);
    }

    @Override
    public void keyTyped(InstanceState state, KeyEvent e) {
        int value = Character.digit(e.getKeyChar(), 16);
        if (value < 0) {
            return;
        }

        BitWidth dataWidth = state.getAttributeValue(StdAttr.WIDTH);
        if (dataWidth == null) {
            dataWidth = BitWidth.create(8);
        }
        currentValue = (currentValue * 16 + value) & dataWidth.getMask();
        RegisterData data = (RegisterData) state.getData();
        data.value = currentValue;

        state.fireInvalidated();
    }
}
