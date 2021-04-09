/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.data;

import com.cburch.logisim.util.StringGetter;

public class AttributeOption implements AttributeOptionInterface {

    private final Object value;
    private final String name;
    private final StringGetter description;

    public AttributeOption(Object value, StringGetter description) {
        this.value = value;
        this.name = value.toString();
        this.description = description;
    }

    public AttributeOption(Object value, String name, StringGetter description) {
        this.value = value;
        this.name = name;
        this.description = description;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name;
    }

    public String toDisplayString() {
        return description.get();
    }
}
