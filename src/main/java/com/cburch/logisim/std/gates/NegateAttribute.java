/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.gates;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.util.StringUtil;

class NegateAttribute extends Attribute<Boolean> {

    private static final Attribute<Boolean> BOOLEAN_ATTR = Attributes.forBoolean("negateDummy");
    private final Direction side;
    int index;

    public NegateAttribute(int index, Direction side) {
        super("negate" + index, null);
        this.index = index;
        this.side = side;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof NegateAttribute) {
            NegateAttribute attribute = (NegateAttribute) other;
            return this.index == attribute.index && this.side == attribute.side;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return index * 31 + (side == null ? 0 : side.hashCode());
    }

    @Override
    public String getDisplayName() {
        String ret = StringUtil.format(Strings.get("gateNegateAttr"), "" + (index + 1));
        if (side != null) {
            ret += " (" + side.toVerticalDisplayString() + ")";
        }
        return ret;
    }

    @Override
    public String toDisplayString(Boolean value) {
        return BOOLEAN_ATTR.toDisplayString(value);
    }

    @Override
    public Boolean parse(String value) {
        return BOOLEAN_ATTR.parse(value);
    }

    @Override
    public java.awt.Component getCellEditor(Boolean value) {
        return BOOLEAN_ATTR.getCellEditor(null, value);
    }


}
