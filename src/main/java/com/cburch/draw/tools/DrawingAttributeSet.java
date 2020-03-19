/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.tools;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.util.EventSourceWeakSupport;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class DrawingAttributeSet implements AttributeSet, Cloneable {

    private static final List<Attribute<?>> ATTRIBUTES_ALL
            = UnmodifiableList.create(new Attribute<?>[]{
            DrawAttr.FONT, DrawAttr.ALIGNMENT,
            DrawAttr.PAINT_TYPE,
            DrawAttr.STROKE_WIDTH, DrawAttr.STROKE_COLOR,
            DrawAttr.FILL_COLOR, DrawAttr.TEXT_DEFAULT_FILL,
            DrawAttr.CORNER_RADIUS});
    private static final List<Object> DEFAULTS_ALL
            = Arrays.asList(DrawAttr.DEFAULT_FONT, DrawAttr.ALIGN_CENTER,
            DrawAttr.PAINT_STROKE,
            1, Color.BLACK,
            Color.WHITE, Color.BLACK, 10);
    private EventSourceWeakSupport<AttributeListener> listeners;
    private List<Attribute<?>> attributes;
    private List<Object> values;

    public DrawingAttributeSet() {
        listeners = new EventSourceWeakSupport<>();
        attributes = ATTRIBUTES_ALL;
        values = DEFAULTS_ALL;
    }

    public AttributeSet createSubset(AbstractTool tool) {
        return new Restriction(tool);
    }

    public void addAttributeListener(AttributeListener listener) {
        listeners.add(listener);
    }

    public void removeAttributeListener(AttributeListener listener) {
        listeners.remove(listener);
    }

    @Override
    public Object clone() {
        try {
            DrawingAttributeSet attributeSet = (DrawingAttributeSet) super.clone();
            attributeSet.listeners = new EventSourceWeakSupport<>();
            attributeSet.values = new ArrayList<>(this.values);
            return attributeSet;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public List<Attribute<?>> getAttributes() {
        return attributes;
    }

    public boolean containsAttribute(Attribute<?> attribute) {
        return attributes.contains(attribute);
    }

    public Attribute<?> getAttribute(String name) {
        for (Attribute<?> attribute : attributes) {
            if (attribute.getName().equals(name)) {
                return attribute;
            }
        }
        return null;
    }

    public boolean isReadOnly(Attribute<?> attribute) {
        return false;
    }

    public void setReadOnly(Attribute<?> attribute, boolean value) {
        throw new UnsupportedOperationException("setReadOnly");
    }

    public boolean isToSave(Attribute<?> attribute) {
        return true;
    }

    public <V> V getValue(Attribute<V> attribute) {
        Iterator<Attribute<?>> attributeIterator = attributes.iterator();
        Iterator<Object> objectIterator = values.iterator();
        while (attributeIterator.hasNext()) {
            Object a = attributeIterator.next();
            Object v = objectIterator.next();
            if (a.equals(attribute)) {
                @SuppressWarnings("unchecked")
                V returnObject = (V) v;
                return returnObject;
            }
        }
        return null;
    }

    public <V> void setValue(Attribute<V> attribute, V value) {
        Iterator<Attribute<?>> attributeIterator = attributes.iterator();
        ListIterator<Object> objectIterator = values.listIterator();
        while (attributeIterator.hasNext()) {
            Object a = attributeIterator.next();
            objectIterator.next();
            if (a.equals(attribute)) {
                objectIterator.set(value);
                AttributeEvent event = new AttributeEvent(this, attribute, value);
                for (AttributeListener listener : listeners) {
                    listener.attributeValueChanged(event);
                }
                if (attribute == DrawAttr.PAINT_TYPE) {
                    event = new AttributeEvent(this);
                    for (AttributeListener listener : listeners) {
                        listener.attributeListChanged(event);
                    }
                }
                return;
            }
        }
        throw new IllegalArgumentException(attribute.toString());
    }

    public <E extends CanvasObject> E applyTo(E drawable) {
        AbstractCanvasObject canvasObject = (AbstractCanvasObject) drawable;
        // use a for(i...) loop since the attribute list may change as we go on
        for (int i = 0; i < canvasObject.getAttributes().size(); i++) {
            Attribute<?> attribute = canvasObject.getAttributes().get(i);
            @SuppressWarnings("unchecked")
            Attribute<Object> objectAttribute = (Attribute<Object>) attribute;
            if (attribute == DrawAttr.FILL_COLOR && this.containsAttribute(DrawAttr.TEXT_DEFAULT_FILL)) {
                canvasObject.setValue(objectAttribute, this.getValue(DrawAttr.TEXT_DEFAULT_FILL));
            } else if (this.containsAttribute(objectAttribute)) {
                Object value = this.getValue(objectAttribute);
                canvasObject.setValue(objectAttribute, value);
            }
        }
        return drawable;
    }

    private class Restriction extends AbstractAttributeSet
            implements AttributeListener {

        private AbstractTool tool;
        private List<Attribute<?>> selectedAttributes;
        private List<Attribute<?>> selectedView;

        private Restriction(AbstractTool tool) {
            this.tool = tool;
            updateAttributes();
        }

        private void updateAttributes() {
            List<Attribute<?>> toolAttributes;
            if (tool == null) {
                toolAttributes = Collections.emptyList();
            } else {
                toolAttributes = tool.getAttributes();
            }
            if (!toolAttributes.equals(selectedAttributes)) {
                selectedAttributes = new ArrayList<>(toolAttributes);
                selectedView = Collections.unmodifiableList(selectedAttributes);
                DrawingAttributeSet.this.addAttributeListener(this);
                fireAttributeListChanged();
            }
        }

        @Override
        protected void copyInto(AbstractAttributeSet attributeSet) {
            DrawingAttributeSet.this.addAttributeListener(this);
        }

        @Override
        public List<Attribute<?>> getAttributes() {
            return selectedView;
        }

        @Override
        public <V> V getValue(Attribute<V> attribute) {
            return DrawingAttributeSet.this.getValue(attribute);
        }

        @Override
        public <V> void setValue(Attribute<V> attribute, V value) {
            DrawingAttributeSet.this.setValue(attribute, value);
            updateAttributes();
        }

        //
        // AttributeListener methods
        //
        public void attributeListChanged(AttributeEvent event) {
            fireAttributeListChanged();
        }

        public void attributeValueChanged(AttributeEvent event) {
            if (selectedAttributes.contains(event.getAttribute())) {
                @SuppressWarnings("unchecked")
                Attribute<Object> attribute = (Attribute<Object>) event.getAttribute();
                fireAttributeValueChanged(attribute, event.getValue());
            }
            updateAttributes();
        }
    }
}
