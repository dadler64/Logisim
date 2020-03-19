/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.circuit;

import com.cburch.logisim.comp.AbstractComponentFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;
import java.awt.Color;
import java.awt.Graphics;

class WireFactory extends AbstractComponentFactory {

    public static final WireFactory instance = new WireFactory();

    private WireFactory() {
    }

    @Override
    public String getName() {
        return "Wire";
    }

    @Override
    public StringGetter getDisplayGetter() {
        return Strings.getter("wireComponent");
    }

    @Override
    public AttributeSet createAttributeSet() {
        return Wire.create(Location.create(0, 0), Location.create(100, 0));
    }

    @Override
    public Component createComponent(Location location, AttributeSet attributes) {
        Object dir = attributes.getValue(Wire.DIRECTION_ATTRIBUTE);
        int len = attributes.getValue(Wire.LENGTH_ATTRIBUTE);

        if (dir == Wire.VALUE_HORIZONTAL) {
            return Wire.create(location, location.translate(len, 0));
        } else {
            return Wire.create(location, location.translate(0, len));
        }
    }

    @Override
    public Bounds getOffsetBounds(AttributeSet attributes) {
        Object dir = attributes.getValue(Wire.DIRECTION_ATTRIBUTE);
        int len = attributes.getValue(Wire.LENGTH_ATTRIBUTE);

        if (dir == Wire.VALUE_HORIZONTAL) {
            return Bounds.create(0, -2, len, 5);
        } else {
            return Bounds.create(-2, 0, 5, len);
        }
    }

    //
    // user interface methods
    //
    @Override
    public void drawGhost(ComponentDrawContext context,
            Color color, int x, int y, AttributeSet attributes) {
        Graphics g = context.getGraphics();
        Object dir = attributes.getValue(Wire.DIRECTION_ATTRIBUTE);
        int len = attributes.getValue(Wire.LENGTH_ATTRIBUTE);

        g.setColor(color);
        GraphicsUtil.switchToWidth(g, 3);
        if (dir == Wire.VALUE_HORIZONTAL) {
            g.drawLine(x, y, x + len, y);
        } else {
            g.drawLine(x, y, x, y + len);
        }
    }
}
