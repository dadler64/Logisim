/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.comp;

import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.StringUtil;
import java.awt.Color;
import java.awt.Graphics;
import javax.swing.Icon;

public abstract class AbstractComponentFactory implements ComponentFactory {

    private static final Icon toolIcon = Icons.getIcon("subcirc.gif");

    private AttributeSet defaultSet;

    protected AbstractComponentFactory() {
        defaultSet = null;
    }

    @Override
    public String toString() {
        return getName();
    }

    public abstract String getName();

    public String getDisplayName() {
        return getDisplayGetter().get();
    }

    public StringGetter getDisplayGetter() {
        return StringUtil.constantGetter(getName());
    }

    public abstract Component createComponent(Location location, AttributeSet attributes);

    public abstract Bounds getOffsetBounds(AttributeSet attributes);

    public AttributeSet createAttributeSet() {
        return AttributeSets.EMPTY;
    }

    public boolean isAllDefaultValues(AttributeSet attributeSet, LogisimVersion version) {
        return false;
    }

    public Object getDefaultAttributeValue(Attribute<?> attribute, LogisimVersion version) {
        AttributeSet defaultSet = this.defaultSet;
        if (defaultSet == null) {
            defaultSet = (AttributeSet) createAttributeSet().clone();
            this.defaultSet = defaultSet;
        }
        return defaultSet.getValue(attribute);
    }

    //
    // user interface methods
    //
    public void drawGhost(ComponentDrawContext context, Color color, int x, int y, AttributeSet attributes) {
        Graphics graphics = context.getGraphics();
        Bounds bounds = getOffsetBounds(attributes);
        graphics.setColor(color);
        GraphicsUtil.switchToWidth(graphics, 2);
        graphics.drawRect(x + bounds.getX(), y + bounds.getY(), bounds.getWidth(), bounds.getHeight());
    }

    public void paintIcon(ComponentDrawContext context, int x, int y, AttributeSet attributes) {
        Graphics graphics = context.getGraphics();
        if (toolIcon != null) {
            toolIcon.paintIcon(context.getDestination(), graphics, x + 2, y + 2);
        } else {
            graphics.setColor(Color.black);
            graphics.drawRect(x + 5, y + 2, 11, 17);
            Value[] values = {Value.TRUE, Value.FALSE};
            for (int i = 0; i < 3; i++) {
                graphics.setColor(values[i % 2].getColor());
                graphics.fillOval(x + 5 - 1, y + 5 + 5 * i - 1, 3, 3);
                graphics.setColor(values[(i + 1) % 2].getColor());
                graphics.fillOval(x + 16 - 1, y + 5 + 5 * i - 1, 3, 3);
            }
        }
    }

    public Object getFeature(Object key, AttributeSet attributeSet) {
        return null;
    }

}
