/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.data;

import java.util.List;

public interface AttributeSet {

    Object clone();

    void addAttributeListener(AttributeListener l);

    void removeAttributeListener(AttributeListener l);

    List<Attribute<?>> getAttributes();

    boolean containsAttribute(Attribute<?> attr);

    Attribute<?> getAttribute(String name);

    boolean isReadOnly(Attribute<?> attr);

    void setReadOnly(Attribute<?> attr, boolean value);  // optional

    boolean isToSave(Attribute<?> attr);

    <V> V getValue(Attribute<V> attr);

    <V> void setValue(Attribute<V> attr, V value);
}
