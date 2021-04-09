/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.gui;

import com.cburch.draw.canvas.Selection;
import com.cburch.draw.canvas.SelectionEvent;
import com.cburch.draw.canvas.SelectionListener;
import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SelectionAttributes extends AbstractAttributeSet {

    private final Selection selection;
    private Listener listener;
    private Map<AttributeSet, CanvasObject> selected;
    private Attribute<?>[] selectedAttributes;
    private Object[] selectedValues;
    private List<Attribute<?>> attributesView;

    public SelectionAttributes(Selection selection) {
        this.selection = selection;
        this.listener = new Listener();
        this.selected = Collections.emptyMap();
        this.selectedAttributes = new Attribute<?>[0];
        this.selectedValues = new Object[0];
        this.attributesView = Collections.unmodifiableList(Arrays.asList(selectedAttributes));

        selection.addSelectionListener(listener);
        listener.selectionChanged(null);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private static Object getSelectionValue(Attribute<?> attribute, Set<AttributeSet> selectedAttributes) {
        Object object = null;
        for (AttributeSet attributeSet : selectedAttributes) {
            if (attributeSet.containsAttribute(attribute)) {
                Object value = attributeSet.getValue(attribute);
                if (object == null) {
                    object = value;
                } else if (value != null && value.equals(object)) { // TODO: Remove this
                    // keep on, making sure everything else matches
                } else {
                    return null;
                }
            }
        }
        return object;
    }

    public Iterable<Map.Entry<AttributeSet, CanvasObject>> entries() {
        Set<Map.Entry<AttributeSet, CanvasObject>> raw = selected.entrySet();
        return new ArrayList<>(raw);
    }

    //
    // AbstractAttributeSet methods
    //
    @Override
    protected void copyInto(AbstractAttributeSet destination) {
        listener = new Listener();
        selection.addSelectionListener(listener);
    }

    @Override
    public List<Attribute<?>> getAttributes() {
        return attributesView;
    }

    @Override
    public <V> V getValue(Attribute<V> attribute) {
        Attribute<?>[] attributes = this.selectedAttributes;
        Object[] values = this.selectedValues;
        for (int i = 0; i < attributes.length; i++) {
            if (attributes[i] == attribute) {
                @SuppressWarnings("unchecked")
                V ret = (V) values[i];
                return ret;
            }
        }
        return null;
    }

    @Override
    public <V> void setValue(Attribute<V> attribute, V value) {
        Attribute<?>[] attributes = this.selectedAttributes;
        Object[] values = this.selectedValues;
        for (int i = 0; i < attributes.length; i++) {
            if (attributes[i] == attribute) {
                boolean isSame = Objects.equals(value, values[i]);
                if (!isSame) {
                    values[i] = value;
                    for (AttributeSet objectAttributes : selected.keySet()) {
                        objectAttributes.setValue(attribute, value);
                    }
                }
                break;
            }
        }
    }

    private class Listener implements SelectionListener, AttributeListener {

        //
        // SelectionListener
        //
        public void selectionChanged(SelectionEvent event) {
            Map<AttributeSet, CanvasObject> oldSelected = selected;
            Map<AttributeSet, CanvasObject> newSelected = new HashMap<>();
            for (CanvasObject selectedObject : selection.getSelected()) {
                newSelected.put(selectedObject.getAttributeSet(), selectedObject);
            }
            selected = newSelected;
            boolean isChanged = false;
            for (AttributeSet attributes : oldSelected.keySet()) {
                if (!newSelected.containsKey(attributes)) {
                    isChanged = true;
                    attributes.removeAttributeListener(this);
                }
            }
            for (AttributeSet attrs : newSelected.keySet()) {
                if (!oldSelected.containsKey(attrs)) {
                    isChanged = true;
                    attrs.addAttributeListener(this);
                }
            }
            if (isChanged) {
                computeAttributeList(newSelected.keySet());
                fireAttributeListChanged();
            }
        }

        private void computeAttributeList(Set<AttributeSet> attrsSet) {
            Set<Attribute<?>> attributeSet = new LinkedHashSet<>();
            Iterator<AttributeSet> sit = attrsSet.iterator();
            if (sit.hasNext()) {
                AttributeSet first = sit.next();
                attributeSet.addAll(first.getAttributes());
                while (sit.hasNext()) {
                    AttributeSet next = sit.next();
                    attributeSet.removeIf(attribute -> !next.containsAttribute(attribute));
                }
            }

            Attribute<?>[] attributes = new Attribute[attributeSet.size()];
            Object[] values = new Object[attributes.length];
            int i = 0;
            for (Attribute<?> attribute : attributeSet) {
                attributes[i] = attribute;
                values[i] = getSelectionValue(attribute, attrsSet);
                i++;
            }
            SelectionAttributes.this.selectedAttributes = attributes;
            SelectionAttributes.this.selectedValues = values;
            SelectionAttributes.this.attributesView = Collections.unmodifiableList(Arrays.asList(attributes));
            fireAttributeListChanged();
        }

        //
        // AttributeSet listener
        //
        public void attributeListChanged(AttributeEvent event) {
            // show selection attributes
            computeAttributeList(selected.keySet());
        }

        public void attributeValueChanged(AttributeEvent event) {
            if (selected.containsKey(event.getSource())) {
                @SuppressWarnings("unchecked")
                Attribute<Object> attribute = (Attribute<Object>) event.getAttribute();
                Attribute<?>[] attributes = SelectionAttributes.this.selectedAttributes;
                Object[] values = SelectionAttributes.this.selectedValues;
                for (int i = 0; i < attributes.length; i++) {
                    if (attributes[i] == attribute) {
                        values[i] = getSelectionValue(attribute, selected.keySet());
                    }
                }
            }
        }
    }
}
