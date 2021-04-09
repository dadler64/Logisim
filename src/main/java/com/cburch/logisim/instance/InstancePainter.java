/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.instance;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.WireSet;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.proj.Project;
import java.awt.Graphics;

public class InstancePainter implements InstanceState {

    private final ComponentDrawContext context;
    private InstanceComponent component;
    private InstanceFactory factory;
    private AttributeSet attrs;

    public InstancePainter(ComponentDrawContext context, InstanceComponent instance) {
        this.context = context;
        this.component = instance;
    }

    void setFactory(InstanceFactory factory, AttributeSet attrs) {
        this.component = null;
        this.factory = factory;
        this.attrs = attrs;
    }

    public InstanceFactory getFactory() {
        return component == null ? factory : (InstanceFactory) component.getFactory();
    }

    //
    // methods related to the context of the canvas
    //
    public WireSet getHighlightedWires() {
        return context.getHighlightedWires();
    }

    public boolean getShowState() {
        return context.getShowState();
    }

    public boolean isPrintView() {
        return context.isPrintView();
    }

    public boolean shouldDrawColor() {
        return context.shouldDrawColor();
    }

    public java.awt.Component getDestination() {
        return context.getDestination();
    }

    public Graphics getGraphics() {
        return context.getGraphics();
    }

    public Circuit getCircuit() {
        return context.getCircuit();
    }

    public Object getGateShape() {
        return context.getGateShape();
    }

    public boolean isCircuitRoot() {
        return !context.getCircuitState().isSubstate();
    }

    public long getTickCount() {
        return context.getCircuitState().getPropagator().getTickCount();
    }

    //
    // methods related to the circuit state
    //
    public Project getProject() {
        return context.getCircuitState().getProject();
    }

    public Value getPort(int portIndex) {
        InstanceComponent component = this.component;
        CircuitState state = context.getCircuitState();
        if (component != null && state != null) {
            return state.getValue(component.getEnd(portIndex).getLocation());
        } else {
            return Value.UNKNOWN;
        }
    }

    public void setPort(int portIndex, Value value, int delay) {
        throw new UnsupportedOperationException("setValue on InstancePainter");
    }

    public InstanceData getData() {
        CircuitState circuitState = context.getCircuitState();
        if (circuitState == null || component == null) {
            throw new UnsupportedOperationException("setData on InstancePainter");
        } else {
            return (InstanceData) circuitState.getData(component);
        }
    }

    public void setData(InstanceData value) {
        CircuitState circuitState = context.getCircuitState();
        if (circuitState == null || component == null) {
            throw new UnsupportedOperationException("setData on InstancePainter");
        } else {
            circuitState.setData(component, value);
        }
    }

    //
    // methods related to the instance
    //
    public Instance getInstance() {
        InstanceComponent component = this.component;
        return component == null ? null : component.getInstance();
    }

    void setInstance(InstanceComponent value) {
        this.component = value;
    }

    public Location getLocation() {
        InstanceComponent component = this.component;
        return component == null ? Location.create(0, 0) : component.getLocation();
    }

    public boolean isPortConnected(int index) {
        Circuit circuit = context.getCircuit();
        Location location = component.getEnd(index).getLocation();
        return circuit.isConnected(location, component);
    }

    public Bounds getOffsetBounds() {
        InstanceComponent component = this.component;
        if (component == null) {
            return factory.getOffsetBounds(attrs);
        } else {
            Location location = component.getLocation();
            return component.getBounds().translate(-location.getX(), -location.getY());
        }
    }

    public Bounds getBounds() {
        InstanceComponent component = this.component;
        return component == null ? factory.getOffsetBounds(attrs) : component.getBounds();
    }

    public AttributeSet getAttributeSet() {
        InstanceComponent component = this.component;
        return component == null ? attrs : component.getAttributeSet();
    }

    public <E> E getAttributeValue(Attribute<E> attr) {
        InstanceComponent component = this.component;
        AttributeSet attributeSet = component == null ? attrs : component.getAttributeSet();
        return attributeSet.getValue(attr);
    }

    public void fireInvalidated() {
        component.fireInvalidated();
    }

    //
    // helper methods for drawing common elements in components
    //
    public void drawBounds() {
        context.drawBounds(component);
    }

    public void drawRectangle(Bounds bds, String label) {
        context.drawRectangle(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight(), label);
    }

    public void drawRectangle(int x, int y,
        int width, int height, String label) {
        context.drawRectangle(x, y, width, height, label);
    }

    public void drawDongle(int x, int y) {
        context.drawDongle(x, y);
    }

    public void drawPort(int i) {
        context.drawPin(component, i);
    }

    public void drawPort(int i, String label, Direction dir) {
        context.drawPin(component, i, label, dir);
    }

    public void drawPorts() {
        context.drawPins(component);
    }

    public void drawClock(int i, Direction dir) {
        context.drawClock(component, i, dir);
    }

    public void drawHandles() {
        context.drawHandles(component);
    }

    public void drawHandle(Location loc) {
        context.drawHandle(loc);
    }

    public void drawHandle(int x, int y) {
        context.drawHandle(x, y);
    }

    public void drawLabel() {
        if (component != null) {
            component.drawLabel(context);
        }
    }
}
