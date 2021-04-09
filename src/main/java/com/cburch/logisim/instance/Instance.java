/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.instance;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import java.awt.Font;
import java.util.List;

public class Instance {

    private final InstanceComponent component;

    Instance(InstanceComponent component) {
        this.component = component;
    }

    public static Instance getInstanceFor(Component comp) {
        if (comp instanceof InstanceComponent) {
            return ((InstanceComponent) comp).getInstance();
        } else {
            return null;
        }
    }

    public static Component getComponentFor(Instance instance) {
        return instance.component;
    }

    InstanceComponent getComponent() {
        return component;
    }

    public InstanceFactory getFactory() {
        return (InstanceFactory) component.getFactory();
    }

    public Location getLocation() {
        return component.getLocation();
    }

    public Bounds getBounds() {
        return component.getBounds();
    }

    public void setAttributeReadOnly(Attribute<?> attr, boolean value) {
        component.getAttributeSet().setReadOnly(attr, value);
    }

    public <E> E getAttributeValue(Attribute<E> attr) {
        return component.getAttributeSet().getValue(attr);
    }

    public void addAttributeListener() {
        component.addAttributeListener(this);
    }

    public AttributeSet getAttributeSet() {
        return component.getAttributeSet();
    }

    public List<Port> getPorts() {
        return component.getPorts();
    }

    public void setPorts(Port[] ports) {
        component.setPorts(ports);
    }

    public Location getPortLocation(int index) {
        return component.getEnd(index).getLocation();
    }

    public void recomputeBounds() {
        component.recomputeBounds();
    }

    public void setTextField(Attribute<String> labelAttr, Attribute<Font> fontAttr, int x, int y, int halign, int valign) {
        component.setTextField(labelAttr, fontAttr, x, y, halign, valign);
    }

    public InstanceData getData(CircuitState state) {
        return (InstanceData) state.getData(component);
    }

    public void setData(CircuitState state, InstanceData data) {
        state.setData(component, data);
    }

    public void fireInvalidated() {
        component.fireInvalidated();
    }
}
