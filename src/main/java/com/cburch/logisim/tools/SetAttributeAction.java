/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.tools;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.CircuitTransaction;
import com.cburch.logisim.circuit.CircuitTransactionResult;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.StringGetter;
import java.util.ArrayList;
import java.util.List;

public class SetAttributeAction extends Action {

    private final StringGetter nameGetter;
    private final Circuit circuit;
    private final List<Component> components;
    private final List<Attribute<Object>> attrs;
    private final List<Object> values;
    private final List<Object> oldValues;
    private CircuitTransaction xnReverse;

    public SetAttributeAction(Circuit circuit, StringGetter nameGetter) {
        this.nameGetter = nameGetter;
        this.circuit = circuit;
        this.components = new ArrayList<>();
        this.attrs = new ArrayList<>();
        this.values = new ArrayList<>();
        this.oldValues = new ArrayList<>();
    }

    public void set(Component component, Attribute<?> attr, Object value) {
        @SuppressWarnings("unchecked")
        Attribute<Object> objectAttr = (Attribute<Object>) attr;
        components.add(component);
        attrs.add(objectAttr);
        values.add(value);
    }

    public boolean isEmpty() {
        return components.isEmpty();
    }

    @Override
    public String getName() {
        return nameGetter.get();
    }

    @Override
    public void doIt(Project project) {
        CircuitMutation mutation = new CircuitMutation(circuit);
        int size = values.size();
        oldValues.clear();
        for (int i = 0; i < size; i++) {
            Component component = components.get(i);
            Attribute<Object> attr = attrs.get(i);
            Object value = values.get(i);
            if (circuit.contains(component)) {
                oldValues.add(null);
                mutation.set(component, attr, value);
            } else {
                AttributeSet componentAttrs = component.getAttributeSet();
                oldValues.add(componentAttrs.getValue(attr));
                componentAttrs.setValue(attr, value);
            }
        }

        if (!mutation.isEmpty()) {
            CircuitTransactionResult result = mutation.execute();
            xnReverse = result.getReverseTransaction();
        }
    }

    @Override
    public void undo(Project project) {
        if (xnReverse != null) {
            xnReverse.execute();
        }
        for (int i = oldValues.size() - 1; i >= 0; i--) {
            Component component = components.get(i);
            Attribute<Object> attr = attrs.get(i);
            Object value = oldValues.get(i);
            if (value != null) {
                component.getAttributeSet().setValue(attr, value);
            }
        }
    }
}
