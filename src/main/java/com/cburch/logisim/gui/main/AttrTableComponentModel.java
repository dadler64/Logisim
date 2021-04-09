/* Copyright (c) 2011, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.gui.generic.AttrTableSetException;
import com.cburch.logisim.gui.generic.AttributeSetTableModel;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.SetAttributeAction;

class AttrTableComponentModel extends AttributeSetTableModel {

    Project project;
    Circuit circuit;
    Component component;

    AttrTableComponentModel(Project project, Circuit circuit, Component component) {
        super(component.getAttributeSet());
        this.project = project;
        this.circuit = circuit;
        this.component = component;
    }

    public Circuit getCircuit() {
        return circuit;
    }

    public Component getComponent() {
        return component;
    }

    @Override
    public String getTitle() {
        return component.getFactory().getDisplayName();
    }

    @Override
    public void setValueRequested(Attribute<Object> attribute, Object value)
        throws AttrTableSetException {
        if (!project.getLogisimFile().contains(circuit)) {
            String msg = Strings.get("cannotModifyCircuitError");
            throw new AttrTableSetException(msg);
        } else {
            SetAttributeAction act = new SetAttributeAction(circuit,
                Strings.getter("changeAttributeAction"));
            act.set(component, attribute, value);
            project.doAction(act);
        }
    }
}


