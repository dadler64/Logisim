/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.base;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.StdAttr;
import java.awt.Font;
import java.util.Arrays;
import java.util.List;

class TextAttributes extends AbstractAttributeSet {

    private static final List<Attribute<?>> ATTRIBUTES
        = Arrays.asList(Text.ATTR_TEXT, Text.ATTR_FONT, Text.ATTR_HALIGN, Text.ATTR_VALIGN);

    private String text;
    private Font font;
    private AttributeOption hAlign;
    private AttributeOption vAlign;
    private Bounds offsetBounds;

    public TextAttributes() {
        text = "";
        font = StdAttr.DEFAULT_LABEL_FONT;
        hAlign = Text.ATTR_HALIGN.parse("center");
        vAlign = Text.ATTR_VALIGN.parse("base");
        offsetBounds = null;
    }

    String getText() {
        return text;
    }

    Font getFont() {
        return font;
    }

    int getHorizontalAlign() {
        return (Integer) hAlign.getValue();
    }

    int getVerticalAlign() {
        return (Integer) vAlign.getValue();
    }

    Bounds getOffsetBounds() {
        return offsetBounds;
    }

    boolean setOffsetBounds(Bounds value) {
        Bounds old = offsetBounds;
        boolean same = old == null ? value == null : old.equals(value);
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
        if (attr == Text.ATTR_TEXT) {
            return (V) text;
        }
        if (attr == Text.ATTR_FONT) {
            return (V) font;
        }
        if (attr == Text.ATTR_HALIGN) {
            return (V) hAlign;
        }
        if (attr == Text.ATTR_VALIGN) {
            return (V) vAlign;
        }
        return null;
    }

    @Override
    public <V> void setValue(Attribute<V> attr, V value) {
        if (attr == Text.ATTR_TEXT) {
            text = (String) value;
        } else if (attr == Text.ATTR_FONT) {
            font = (Font) value;
        } else if (attr == Text.ATTR_HALIGN) {
            hAlign = (AttributeOption) value;
        } else if (attr == Text.ATTR_VALIGN) {
            vAlign = (AttributeOption) value;
        } else {
            throw new IllegalArgumentException("unknown attribute");
        }
        offsetBounds = null;
        fireAttributeValueChanged(attr, value);
    }

}
