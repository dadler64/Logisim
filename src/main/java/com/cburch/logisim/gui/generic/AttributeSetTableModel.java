/* Copyright (c) 2011, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.generic;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import java.awt.Component;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

public abstract class AttributeSetTableModel
        implements AttrTableModel, AttributeListener {

    private ArrayList<AttrTableModelListener> listeners;
    private AttributeSet attributes;
    private HashMap<Attribute<?>, AttrRow> rowMap;
    private ArrayList<AttrRow> rows;

    public AttributeSetTableModel(AttributeSet attributes) {
        this.attributes = attributes;
        this.listeners = new ArrayList<>();
        this.rowMap = new HashMap<>();
        this.rows = new ArrayList<>();
        if (attributes != null) {
            for (Attribute<?> attribute : attributes.getAttributes()) {
                AttrRow row = new AttrRow(attribute);
                rowMap.put(attribute, row);
                rows.add(row);
            }
        }
    }

    public abstract String getTitle();

    public AttributeSet getAttributes() {
        return attributes;
    }

    public void setAttributes(AttributeSet attributes) {
        if (!Objects.equals(this.attributes, attributes)) {
            if (!listeners.isEmpty()) {
                this.attributes.removeAttributeListener(this);
            }
            this.attributes = attributes;
            if (!listeners.isEmpty()) {
                this.attributes.addAttributeListener(this);
            }
            attributeListChanged(null);
        }
    }

    public void addAttrTableModelListener(AttrTableModelListener listener) {
        if (listeners.isEmpty() && attributes != null) {
            attributes.addAttributeListener(this);
        }
        listeners.add(listener);
    }

    public void removeAttrTableModelListener(AttrTableModelListener listener) {
        listeners.remove(listener);
        if (listeners.isEmpty() && attributes != null) {
            attributes.removeAttributeListener(this);
        }
    }

    protected void fireTitleChanged() {
        AttrTableModelEvent event = new AttrTableModelEvent(this);
        for (AttrTableModelListener listener : listeners) {
            listener.attrTitleChanged(event);
        }
    }

    private void fireStructureChanged() {
        AttrTableModelEvent event = new AttrTableModelEvent(this);
        for (AttrTableModelListener listener : listeners) {
            listener.attrStructureChanged(event);
        }
    }

    private void fireValueChanged(int index) {
        AttrTableModelEvent event = new AttrTableModelEvent(this, index);
        for (AttrTableModelListener listener : listeners) {
            listener.attrValueChanged(event);
        }
    }

    public int getRowCount() {
        return rows.size();
    }

    public AttrTableModelRow getRow(int rowIndex) {
        return rows.get(rowIndex);
    }

    protected abstract void setValueRequested(Attribute<Object> attribute, Object value)
            throws AttrTableSetException;

    //
    // AttributeListener methods
    //
    public void attributeListChanged(AttributeEvent event) {
        // if anything has changed, don't do anything
        int index = 0;
        boolean match = true;
        int rowsSize = rows.size();
        for (Attribute<?> attribute : attributes.getAttributes()) {
            if (index >= rowsSize || rows.get(index).attribute != attribute) {
                match = false;
                break;
            }
            index++;
        }
        if (match && index == rows.size()) {
            return;
        }

        // compute the new list of rows, possible adding into hash map
        ArrayList<AttrRow> newRows = new ArrayList<>();
        HashSet<Attribute<?>> missing = new HashSet<>(rowMap.keySet());
        for (Attribute<?> attribute : attributes.getAttributes()) {
            AttrRow row = rowMap.get(attribute);
            if (row == null) {
                row = new AttrRow(attribute);
                rowMap.put(attribute, row);
            } else {
                missing.remove(attribute);
            }
            newRows.add(row);
        }
        rows = newRows;
        for (Attribute<?> attribute : missing) {
            rowMap.remove(attribute);
        }

        fireStructureChanged();
    }

    public void attributeValueChanged(AttributeEvent event) {
        Attribute<?> attribute = event.getAttribute();
        AttrTableModelRow row = rowMap.get(attribute);
        if (row != null) {
            int index = rows.indexOf(row);
            if (index >= 0) {
                fireValueChanged(index);
            }
        }
    }

    private class AttrRow implements AttrTableModelRow {

        private Attribute<Object> attribute;

        private AttrRow(Attribute<?> attribute) {
            @SuppressWarnings("unchecked")
            Attribute<Object> objectAttribute = (Attribute<Object>) attribute;
            this.attribute = objectAttribute;
        }

        public String getLabel() {
            return attribute.getDisplayName();
        }

        public String getValue() {
            Object value = attributes.getValue(attribute);
            if (value == null) {
                return "";
            } else {
                try {
                    return attribute.toDisplayString(value);
                } catch (Exception e) {
                    return "???\n\t" + e.getMessage();
                }
            }
        }

        public void setValue(Object value) throws AttrTableSetException {
            Attribute<Object> attribute = this.attribute;
            if (attribute == null || value == null) {
                return;
            }

            try {
                if (value instanceof String) {
                    value = attribute.parse((String) value);
                }
                setValueRequested(attribute, value);
            } catch (ClassCastException e) {
                String error = Strings.get("attributeChangeInvalidError") + ": " + e;
                throw new AttrTableSetException(error);
            } catch (NumberFormatException e) {
                String error = Strings.get("attributeChangeInvalidError");
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.length() > 0) {
                    error += ": " + errorMessage;
                }
                error += ".";
                throw new AttrTableSetException(error);
            }
        }

        public boolean isValueEditable() {
            return !attributes.isReadOnly(attribute);
        }

        public Component getEditor(Window parent) {
            Object value = attributes.getValue(attribute);
            return attribute.getCellEditor(parent, value);
        }
    }

}
