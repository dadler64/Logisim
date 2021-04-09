/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.tools;

import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import java.util.ArrayList;
import java.util.List;

class FactoryAttributes implements AttributeSet, AttributeListener, Cloneable {

    private final Class<? extends Library> descriptionBase;
    private final FactoryDescription description;
    private final ArrayList<AttributeListener> listeners;
    private ComponentFactory factory;
    private AttributeSet baseAttrs;

    public FactoryAttributes(Class<? extends Library> descriptionBase, FactoryDescription description) {
        this.descriptionBase = descriptionBase;
        this.description = description;
        this.factory = null;
        this.baseAttrs = null;
        this.listeners = new ArrayList<>();
    }

    public FactoryAttributes(ComponentFactory factory) {
        this.descriptionBase = null;
        this.description = null;
        this.factory = factory;
        this.baseAttrs = null;
        this.listeners = new ArrayList<>();
    }

    boolean isFactoryInstantiated() {
        return baseAttrs != null;
    }

    AttributeSet getBase() {
        AttributeSet ret = baseAttrs;
        if (ret == null) {
            ComponentFactory factory = this.factory;
            if (factory == null) {
                factory = description.getFactory(descriptionBase);
                this.factory = factory;
            }
            if (factory == null) {
                ret = AttributeSets.EMPTY;
            } else {
                ret = factory.createAttributeSet();
                ret.addAttributeListener(this);
            }
            baseAttrs = ret;
        }
        return ret;
    }

    public void addAttributeListener(AttributeListener l) {
        listeners.add(l);
    }

    public void removeAttributeListener(AttributeListener l) {
        listeners.remove(l);
    }

    @Override
    public AttributeSet clone() {
        return (AttributeSet) getBase().clone();
    }

    public boolean containsAttribute(Attribute<?> attr) {
        return getBase().containsAttribute(attr);
    }

    public Attribute<?> getAttribute(String name) {
        return getBase().getAttribute(name);
    }

    public List<Attribute<?>> getAttributes() {
        return getBase().getAttributes();
    }

    public <V> V getValue(Attribute<V> attr) {
        return getBase().getValue(attr);
    }

    public boolean isReadOnly(Attribute<?> attr) {
        return getBase().isReadOnly(attr);
    }

    public boolean isToSave(Attribute<?> attr) {
        return getBase().isToSave(attr);
    }

    public void setReadOnly(Attribute<?> attr, boolean value) {
        getBase().setReadOnly(attr, value);
    }

    public <V> void setValue(Attribute<V> attr, V value) {
        getBase().setValue(attr, value);
    }

    public void attributeListChanged(AttributeEvent baseEvent) {
        AttributeEvent event = null;
        for (AttributeListener listener : listeners) {
            if (event == null) {
                event = new AttributeEvent(this, baseEvent.getAttribute(), baseEvent.getValue());
            }
            listener.attributeListChanged(event);
        }
    }

    public void attributeValueChanged(AttributeEvent baseEvent) {
        AttributeEvent event = null;
        for (AttributeListener listener : listeners) {
            if (event == null) {
                event = new AttributeEvent(this, baseEvent.getAttribute(), baseEvent.getValue());
            }
            listener.attributeValueChanged(event);
        }
    }
}
