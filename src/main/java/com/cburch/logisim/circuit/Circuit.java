/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.circuit;

import com.cburch.logisim.circuit.appear.CircuitAppearance;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentEvent;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentListener;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.std.wiring.Clock;
import com.cburch.logisim.util.CollectionUtil;
import com.cburch.logisim.util.EventSourceWeakSupport;
import java.awt.Graphics;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class Circuit {

    private static final PrintStream DEBUG_STREAM = null;
    CircuitWires wires = new CircuitWires();
    private MyComponentListener myComponentListener = new MyComponentListener();
    private EventSourceWeakSupport<CircuitListener> listeners = new EventSourceWeakSupport<>();
    private HashSet<Component> components = new HashSet<>(); // doesn't include wires
    // wires is package-protected for CircuitState and Analyze only.
    private ArrayList<Component> clocks = new ArrayList<>();
    private CircuitLocker locker;
    private WeakHashMap<Component, Circuit> circuitsUsingThis;
    private CircuitAppearance appearance;
    private AttributeSet staticAttributes;
    private SubcircuitFactory subcircuitFactory;

    public Circuit(String name) {
        appearance = new CircuitAppearance(this);
        staticAttributes = CircuitAttributes.createBaseAttrs(this, name);
        subcircuitFactory = new SubcircuitFactory(this);
        locker = new CircuitLocker();
        circuitsUsingThis = new WeakHashMap<>();
    }

    //
    // helper methods for other classes in package
    //
    public static boolean isInput(Component component) {
        return component.getEnd(0).getType() != EndData.INPUT_ONLY;
    }

    CircuitLocker getLocker() {
        return locker;
    }

    public Collection<Circuit> getCircuitsUsingThis() {
        return circuitsUsingThis.values();
    }

    public void mutatorClear() {
        locker.checkForWritePermission("clear");

        Set<Component> oldComponents = components;
        components = new HashSet<>();
        wires = new CircuitWires();
        clocks.clear();
        for (Component comp : oldComponents) {
            if (comp.getFactory() instanceof SubcircuitFactory) {
                SubcircuitFactory sub = (SubcircuitFactory) comp.getFactory();
                sub.getSubcircuit().circuitsUsingThis.remove(comp);
            }
        }
        fireEvent(CircuitEvent.ACTION_CLEAR, oldComponents);
    }

    @Override
    public String toString() {
        return staticAttributes.getValue(CircuitAttributes.NAME_ATTRIBUTE);
    }

    public AttributeSet getStaticAttributes() {
        return staticAttributes;
    }

    //
    // Listener methods
    //
    public void addCircuitListener(CircuitListener listener) {
        listeners.add(listener);
    }

    public void removeCircuitListener(CircuitListener listener) {
        listeners.remove(listener);
    }

    void fireEvent(int action, Object data) {
        fireEvent(new CircuitEvent(action, this, data));
    }

    private void fireEvent(CircuitEvent event) {
        for (CircuitListener listener : listeners) {
            listener.circuitChanged(event);
        }
    }

    //
    // access methods
    //
    public String getName() {
        return staticAttributes.getValue(CircuitAttributes.NAME_ATTRIBUTE);
    }

    //
    // action methods
    //
    public void setName(String name) {
        staticAttributes.setValue(CircuitAttributes.NAME_ATTRIBUTE, name);
    }

    public CircuitAppearance getAppearance() {
        return appearance;
    }

    public SubcircuitFactory getSubcircuitFactory() {
        return subcircuitFactory;
    }

    public Set<WidthIncompatibilityData> getWidthIncompatibilityData() {
        return wires.getWidthIncompatibilityData();
    }

    public BitWidth getWidth(Location point) {
        return wires.getWidth(point);
    }

    public Location getWidthDeterminant(Location point) {
        return wires.getWidthDeterminant(point);
    }

    public boolean hasConflict(Component component) {
        return wires.points.hasConflict(component);
    }

    public Component getExclusive(Location location) {
        return wires.points.getExclusive(location);
    }

    private Set<Component> getComponents() {
        return CollectionUtil.createUnmodifiableSetUnion(components, wires.getWires());
    }

    public boolean contains(Component component) {
        //noinspection SuspiciousMethodCalls
        return components.contains(component) || wires.getWires().contains(component);
    }

    public Set<Wire> getWires() {
        return wires.getWires();
    }

    public Set<Component> getNonWires() {
        return components;
    }

    public Collection<? extends Component> getComponents(Location location) {
        return wires.points.getComponents(location);
    }

    public Collection<? extends Component> getSplitCauses(Location location) {
        return wires.points.getSplitCauses(location);
    }

    public Collection<Wire> getWires(Location location) {
        return wires.points.getWires(location);
    }

    public Collection<? extends Component> getNonWires(Location location) {
        return wires.points.getNonWires(location);
    }

    public boolean isConnected(Location location, Component ignore) {
        for (Component component : wires.points.getComponents(location)) {
            if (component != ignore) {
                return true;
            }
        }
        return false;
    }

    public Set<Location> getSplitLocations() {
        return wires.points.getSplitLocations();
    }

    public Collection<Component> getAllContaining(Location point) {
        HashSet<Component> components = new HashSet<>();
        for (Component component : getComponents()) {
            if (component.contains(point)) {
                components.add(component);
            }
        }
        return components;
    }

    public Collection<Component> getAllContaining(Location point, Graphics graphics) {
        HashSet<Component> components = new HashSet<>();
        for (Component component : getComponents()) {
            if (component.contains(point, graphics)) {
                components.add(component);
            }
        }
        return components;
    }

    public Collection<Component> getAllWithin(Bounds bounds) {
        HashSet<Component> components = new HashSet<>();
        for (Component component : getComponents()) {
            if (bounds.contains(component.getBounds())) {
                components.add(component);
            }
        }
        return components;
    }

    public Collection<Component> getAllWithin(Bounds bounds, Graphics graphics) {
        HashSet<Component> components = new HashSet<>();
        for (Component component : getComponents()) {
            if (bounds.contains(component.getBounds(graphics))) {
                components.add(component);
            }
        }
        return components;
    }

    public WireSet getWireSet(Wire start) {
        return wires.getWireSet(start);
    }

    public Bounds getBounds() {
        Bounds wireBounds = wires.getWireBounds();
        Iterator<Component> iterator = components.iterator();
        if (!iterator.hasNext()) {
            return wireBounds;
        }
        Component first = iterator.next();
        Bounds firstBounds = first.getBounds();
        int xMin = firstBounds.getX();
        int yMin = firstBounds.getY();
        int xMax = xMin + firstBounds.getWidth();
        int yMax = yMin + firstBounds.getHeight();
        while (iterator.hasNext()) {
            Component component = iterator.next();
            Bounds bounds = component.getBounds();
            int x0 = bounds.getX();
            int x1 = x0 + bounds.getWidth();
            int y0 = bounds.getY();
            int y1 = y0 + bounds.getHeight();
            if (x0 < xMin) {
                xMin = x0;
            }
            if (x1 > xMax) {
                xMax = x1;
            }
            if (y0 < yMin) {
                yMin = y0;
            }
            if (y1 > yMax) {
                yMax = y1;
            }
        }
        Bounds componentBounds = Bounds.create(xMin, yMin, xMax - xMin, yMax - yMin);
        if (wireBounds.getWidth() == 0 || wireBounds.getHeight() == 0) {
            return componentBounds;
        } else {
            return componentBounds.add(wireBounds);
        }
    }

    public Bounds getBounds(Graphics graphics) {
        Bounds wireBounds = wires.getWireBounds();
        int xMin = wireBounds.getX();
        int yMin = wireBounds.getY();
        int xMax = xMin + wireBounds.getWidth();
        int yMax = yMin + wireBounds.getHeight();
        if (wireBounds == Bounds.EMPTY_BOUNDS) {
            xMin = Integer.MAX_VALUE;
            yMin = Integer.MAX_VALUE;
            xMax = Integer.MIN_VALUE;
            yMax = Integer.MIN_VALUE;
        }
        for (Component component : components) {
            Bounds bounds = component.getBounds(graphics);
            if (bounds != null && bounds != Bounds.EMPTY_BOUNDS) {
                int x0 = bounds.getX();
                int x1 = x0 + bounds.getWidth();
                int y0 = bounds.getY();
                int y1 = y0 + bounds.getHeight();
                if (x0 < xMin) {
                    xMin = x0;
                }
                if (x1 > xMax) {
                    xMax = x1;
                }
                if (y0 < yMin) {
                    yMin = y0;
                }
                if (y1 > yMax) {
                    yMax = y1;
                }
            }
        }
        if (xMin > xMax || yMin > yMax) {
            return Bounds.EMPTY_BOUNDS;
        }
        return Bounds.create(xMin, yMin, xMax - xMin, yMax - yMin);
    }

    ArrayList<Component> getClocks() {
        return clocks;
    }

    private void showDebug(String message, Object component) {
        PrintStream debugStream = DEBUG_STREAM;
        if (debugStream != null) {
            debugStream.println("mutatorAdd"); //OK
            try {
                throw new Exception();
            } catch (Exception e) {
                e.printStackTrace(debugStream); //OK
            }
        }
    }

    void mutatorAdd(Component component) {
        showDebug("mutatorAdd", component);
        locker.checkForWritePermission("add");

        if (component instanceof Wire) {
            Wire wire = (Wire) component;
            if (wire.getEnd0().equals(wire.getEnd1())) {
                return;
            }
            boolean added = wires.add(wire);
            if (!added) {
                return;
            }
        } else {
            // add it into the circuit
            boolean added = components.add(component);
            if (!added) {
                return;
            }

            wires.add(component);
            ComponentFactory factory = component.getFactory();
            if (factory instanceof Clock) {
                clocks.add(component);
            } else if (factory instanceof SubcircuitFactory) {
                SubcircuitFactory subcircuit = (SubcircuitFactory) factory;
                subcircuit.getSubcircuit().circuitsUsingThis.put(component, this);
            }
            component.addComponentListener(myComponentListener);
        }
        fireEvent(CircuitEvent.ACTION_ADD, component);
    }

    void mutatorRemove(Component component) {
        showDebug("mutatorRemove", component);
        locker.checkForWritePermission("remove");

        if (component instanceof Wire) {
            wires.remove(component);
        } else {
            wires.remove(component);
            components.remove(component);
            ComponentFactory factory = component.getFactory();
            if (factory instanceof Clock) {
                clocks.remove(component);
            } else if (factory instanceof SubcircuitFactory) {
                SubcircuitFactory subcircuit = (SubcircuitFactory) factory;
                subcircuit.getSubcircuit().circuitsUsingThis.remove(component);
            }
            component.removeComponentListener(myComponentListener);
        }
        fireEvent(CircuitEvent.ACTION_REMOVE, component);
    }

    //
    // Graphics methods
    //
    public void draw(ComponentDrawContext context, Collection<Component> hiddenComponents) {
        Graphics contextGraphics = context.getGraphics();
        Graphics graphics = contextGraphics.create();
        context.setGraphics(graphics);
        wires.draw(context, hiddenComponents);

        if (hiddenComponents == null || hiddenComponents.size() == 0) {
            for (Component component : components) {
                Graphics newGraphics = contextGraphics.create();
                context.setGraphics(newGraphics);
                graphics.dispose();
                graphics = newGraphics;

                component.draw(context);
            }
        } else {
            for (Component component : components) {
                if (!hiddenComponents.contains(component)) {
                    Graphics newGraphics = contextGraphics.create();
                    context.setGraphics(newGraphics);
                    graphics.dispose();
                    graphics = newGraphics;

                    try {
                        component.draw(context);
                    } catch (RuntimeException e) {
                        // this is a JAR developer error - display it and move on
                        e.printStackTrace();
                    }
                }
            }
        }
        context.setGraphics(contextGraphics);
        graphics.dispose();
    }

    private class EndChangedTransaction extends CircuitTransaction {

        private Component component;
        private Map<Location, EndData> toRemove;
        private Map<Location, EndData> toAdd;

        EndChangedTransaction(Component component, Map<Location, EndData> toRemove,
                Map<Location, EndData> toAdd) {
            this.component = component;
            this.toRemove = toRemove;
            this.toAdd = toAdd;
        }

        @Override
        protected Map<Circuit, Integer> getAccessedCircuits() {
            return Collections.singletonMap(Circuit.this, READ_WRITE);
        }

        @Override
        protected void run(CircuitMutator mutator) {
            for (Location location : toRemove.keySet()) {
                EndData removed = toRemove.get(location);
                EndData replaced = toAdd.remove(location);
                if (replaced == null) {
                    wires.remove(component, removed);
                } else if (!replaced.equals(removed)) {
                    wires.replace(component, removed, replaced);
                }
            }
            for (EndData end : toAdd.values()) {
                wires.add(component, end);
            }
            ((CircuitMutatorImpl) mutator).markModified(Circuit.this);
        }
    }

    private class MyComponentListener implements ComponentListener {

        public void endChanged(ComponentEvent event) {
            locker.checkForWritePermission("ends changed");
            Component component = event.getSource();
            HashMap<Location, EndData> toRemove = toMap(event.getOldData());
            HashMap<Location, EndData> toAdd = toMap(event.getData());
            EndChangedTransaction transaction = new EndChangedTransaction(component, toRemove, toAdd);
            locker.execute(transaction);
            fireEvent(CircuitEvent.ACTION_INVALIDATE, component);
        }

        private HashMap<Location, EndData> toMap(Object object) {
            HashMap<Location, EndData> map = new HashMap<>();
            if (object instanceof List) {
                @SuppressWarnings("unchecked")
                List<EndData> endData = (List<EndData>) object;
//                int i = -1;
                for (EndData data : endData) {
//                    i++;
                    if (data != null) {
                        map.put(data.getLocation(), data);
                    }
                }
            } else if (object instanceof EndData) {
                EndData end = (EndData) object;
                map.put(end.getLocation(), end);
            }
            return map;
        }

        public void componentInvalidated(ComponentEvent event) {
            fireEvent(CircuitEvent.ACTION_INVALIDATE, event.getSource());
        }
    }
}
