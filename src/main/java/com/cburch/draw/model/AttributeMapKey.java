/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.model;

import com.cburch.logisim.data.Attribute;
import java.util.Objects;

public class AttributeMapKey {

    private final Attribute<?> attribute;
    private final CanvasObject object;

    public AttributeMapKey(Attribute<?> attribute, CanvasObject object) {
        this.attribute = attribute;
        this.object = object;
    }

    public Attribute<?> getAttribute() {
        return attribute;
    }

    public CanvasObject getObject() {
        return object;
    }

    @Override
    public int hashCode() {
        int a = attribute == null ? 0 : attribute.hashCode();
        int b = object == null ? 0 : object.hashCode();
        return a ^ b;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AttributeMapKey)) {
            return false;
        }
        AttributeMapKey mapKey = (AttributeMapKey) other;
        return (Objects.equals(attribute, mapKey.attribute)) && (Objects.equals(object, mapKey.object));
    }
}
