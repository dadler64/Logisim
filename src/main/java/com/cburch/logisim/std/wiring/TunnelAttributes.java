/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.wiring;

import com.cburch.logisim.comp.TextField;
import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.instance.StdAttr;
import java.awt.Font;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

class TunnelAttributes extends AbstractAttributeSet {

    private static final List<Attribute<?>> ATTRIBUTES
        = Arrays.asList(StdAttr.FACING, StdAttr.WIDTH, StdAttr.LABEL, StdAttr.LABEL_FONT);

    private Direction facing;
    private BitWidth width;
    private String label;
    private Font labelFont;
    private Bounds offsetBounds;
    private int labelX;
    private int labelY;
    private int labelHAlign;
    private int labelVAlign;

    public TunnelAttributes() {
        facing = Direction.WEST;
        width = BitWidth.ONE;
        label = "";
        labelFont = StdAttr.DEFAULT_LABEL_FONT;
        offsetBounds = null;
        configureLabel();
    }

    Direction getFacing() {
        return facing;
    }

    String getLabel() {
        return label;
    }

    Font getFont() {
        return labelFont;
    }

    Bounds getOffsetBounds() {
        return offsetBounds;
    }

    int getLabelX() {
        return labelX;
    }

    int getLabelY() {
        return labelY;
    }

    int getLabelHAlign() {
        return labelHAlign;
    }

    int getLabelVAlign() {
        return labelVAlign;
    }

    boolean setOffsetBounds(Bounds value) {
        Bounds oldBounds = offsetBounds;
        boolean same = Objects.equals(oldBounds, value);
        if (!same) {
            offsetBounds = value;
        }
        return !same;
    }

    @Override
    protected void copyInto(AbstractAttributeSet destObj) {
        // nothing to do
    }

    @Override
    public List<Attribute<?>> getAttributes() {
        return ATTRIBUTES;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V getValue(Attribute<V> attr) {
        if (attr == StdAttr.FACING) {
            return (V) facing;
        }
        if (attr == StdAttr.WIDTH) {
            return (V) width;
        }
        if (attr == StdAttr.LABEL) {
            return (V) label;
        }
        if (attr == StdAttr.LABEL_FONT) {
            return (V) labelFont;
        }
        return null;
    }

    @Override
    public <V> void setValue(Attribute<V> attr, V value) {
        if (attr == StdAttr.FACING) {
            facing = (Direction) value;
            configureLabel();
        } else if (attr == StdAttr.WIDTH) {
            width = (BitWidth) value;
        } else if (attr == StdAttr.LABEL) {
            label = (String) value;
        } else if (attr == StdAttr.LABEL_FONT) {
            labelFont = (Font) value;
        } else {
            throw new IllegalArgumentException("unknown attribute");
        }
        offsetBounds = null;
        fireAttributeValueChanged(attr, value);
    }

    private void configureLabel() {
        Direction facing = this.facing;
        int x;
        int y;
        int hAlign;
        int vAlign;
        int margin = Tunnel.ARROW_MARGIN;
        if (facing == Direction.NORTH) {
            x = 0;
            y = margin;
            hAlign = TextField.H_CENTER;
            vAlign = TextField.V_TOP;
        } else if (facing == Direction.SOUTH) {
            x = 0;
            y = -margin;
            hAlign = TextField.H_CENTER;
            vAlign = TextField.V_BOTTOM;
        } else if (facing == Direction.EAST) {
            x = -margin;
            y = 0;
            hAlign = TextField.H_RIGHT;
            vAlign = TextField.V_CENTER_OVERALL;
        } else {
            x = margin;
            y = 0;
            hAlign = TextField.H_LEFT;
            vAlign = TextField.V_CENTER_OVERALL;
        }
        labelX = x;
        labelY = y;
        labelHAlign = hAlign;
        labelVAlign = vAlign;
    }
}
