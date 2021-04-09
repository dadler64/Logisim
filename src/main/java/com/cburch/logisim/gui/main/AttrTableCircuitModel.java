/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.gui.generic.AttrTableSetException;
import com.cburch.logisim.gui.generic.AttributeSetTableModel;
import com.cburch.logisim.proj.Project;

public class AttrTableCircuitModel extends AttributeSetTableModel {

    private final Project project;
    private final Circuit circuit;

    public AttrTableCircuitModel(Project project, Circuit circuit) {
        super(circuit.getStaticAttributes());
        this.project = project;
        this.circuit = circuit;
    }

    @Override
    public String getTitle() {
        return Strings.get("circuitAttrTitle", circuit.getName());
    }

    @Override
    public void setValueRequested(Attribute<Object> attribute, Object value)
        throws AttrTableSetException {
        if (!project.getLogisimFile().contains(circuit)) {
            String msg = Strings.get("cannotModifyCircuitError");
            throw new AttrTableSetException(msg);
        } else {
            CircuitMutation xn = new CircuitMutation(circuit);
            xn.setForCircuit(attribute, value);
            project.doAction(xn.toAction(Strings.getter("changeCircuitAttrAction")));
        }
    }
}

