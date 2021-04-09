/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.StdAttr;
import java.util.List;
import java.util.Objects;

class CounterAttributes extends AbstractAttributeSet {

    private AttributeSet base;

    public CounterAttributes() {
        base = AttributeSets.fixedSet(
            new Attribute<?>[]{
                StdAttr.WIDTH,
                Counter.ATTR_MAX,
                Counter.ATTR_ON_GOAL,
                StdAttr.EDGE_TRIGGER,
                StdAttr.LABEL,
                StdAttr.LABEL_FONT
            }, new Object[]{
                BitWidth.create(8),
                0xFF,
                Counter.ON_GOAL_WRAP,
                StdAttr.TRIG_RISING,
                "",
                StdAttr.DEFAULT_LABEL_FONT
            }
        );
    }

    @Override
    public void copyInto(AbstractAttributeSet dest) {
        ((CounterAttributes) dest).base = (AttributeSet) this.base.clone();
    }

    @Override
    public List<Attribute<?>> getAttributes() {
        return base.getAttributes();
    }

    @Override
    public <V> V getValue(Attribute<V> attr) {
        return base.getValue(attr);
    }

    @Override
    public <V> void setValue(Attribute<V> attr, V value) {
        Object baseValue = base.getValue(attr);
        if (Objects.equals(baseValue, value)) {
            return;
        }

        Integer newMax = null;
        if (attr == StdAttr.WIDTH) {
            BitWidth oldBitWidth = base.getValue(StdAttr.WIDTH);
            BitWidth newBitWidth = (BitWidth) value;
            int oldWidth = oldBitWidth.getWidth();
            int newWidth = newBitWidth.getWidth();
            int oldValue = base.getValue(Counter.ATTR_MAX);
            base.setValue(StdAttr.WIDTH, newBitWidth);
            if (newWidth > oldWidth) {
                newMax = newBitWidth.getMask();
            } else {
                int v = oldValue & newBitWidth.getMask();
                if (v != oldValue) {
                    Integer newValObj = v;
                    base.setValue(Counter.ATTR_MAX, newValObj);
                    fireAttributeValueChanged(Counter.ATTR_MAX, newValObj);
                }
            }
            fireAttributeValueChanged(StdAttr.WIDTH, newBitWidth);
        } else if (attr == Counter.ATTR_MAX) {
            int oldValue = base.getValue(Counter.ATTR_MAX);
            BitWidth bitWidth = base.getValue(StdAttr.WIDTH);
            int newValue = (Integer) value & bitWidth.getMask();
            if (newValue != oldValue) {
                @SuppressWarnings("unchecked")
                V val = (V) Integer.valueOf(newValue);
                value = val;
            }
        }
        base.setValue(attr, value);
        fireAttributeValueChanged(attr, value);
        if (newMax != null) {
            base.setValue(Counter.ATTR_MAX, newMax);
            fireAttributeValueChanged(Counter.ATTR_MAX, newMax);
        }
    }

    @Override
    public boolean containsAttribute(Attribute<?> attr) {
        return base.containsAttribute(attr);
    }

    @Override
    public Attribute<?> getAttribute(String name) {
        return base.getAttribute(name);
    }

    @Override
    public boolean isReadOnly(Attribute<?> attr) {
        return base.isReadOnly(attr);
    }

    @Override
    public void setReadOnly(Attribute<?> attr, boolean value) {
        base.setReadOnly(attr, value);
    }
}
