/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.circuit.appear;

import com.cburch.draw.model.CanvasModelEvent;
import com.cburch.draw.model.CanvasModelListener;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Drawing;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.util.EventSourceWeakSupport;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class CircuitAppearance extends Drawing {

    private final Circuit circuit;
    private final EventSourceWeakSupport<CircuitAppearanceListener> listeners;
    private final CircuitPins circuitPins;
    private boolean isDefault;
    private boolean suppressRecompute;

    public CircuitAppearance(Circuit circuit) {
        this.circuit = circuit;
        listeners = new EventSourceWeakSupport<>();
        PortManager portManager = new PortManager(this);
        circuitPins = new CircuitPins(portManager);
        MyListener myListener = new MyListener();
        suppressRecompute = false;
        addCanvasModelListener(myListener);
        setDefaultAppearance(true);
    }

    public CircuitPins getCircuitPins() {
        return circuitPins;
    }

    public void addCircuitAppearanceListener(CircuitAppearanceListener listener) {
        listeners.add(listener);
    }

    public void removeCircuitAppearanceListener(CircuitAppearanceListener listener) {
        listeners.remove(listener);
    }

    private void fireCircuitAppearanceChanged(int affected) {
        CircuitAppearanceEvent event = new CircuitAppearanceEvent(circuit, affected);
        for (CircuitAppearanceListener listener : listeners) {
            listener.circuitAppearanceChanged(event);
        }
    }

    void replaceAutomatically(List<AppearancePort> objectsToRemove, List<AppearancePort> objectsToAdd) {
        // this should be called only when substituting ports via PortManager
        boolean oldSuppress = suppressRecompute;
        try {
            suppressRecompute = true;
            removeObjects(objectsToRemove);
            addObjects(getObjectsFromBottom().size() - 1, objectsToAdd);
            recomputeDefaultAppearance();
        } finally {
            suppressRecompute = oldSuppress;
        }
        fireCircuitAppearanceChanged(CircuitAppearanceEvent.ALL_TYPES);
    }

    public boolean isDefaultAppearance() {
        return isDefault;
    }

    public void setDefaultAppearance(boolean value) {
        if (isDefault != value) {
            isDefault = value;
            if (value) {
                recomputeDefaultAppearance();
            }
        }
    }

    void recomputePorts() {
        if (isDefault) {
            recomputeDefaultAppearance();
        } else {
            fireCircuitAppearanceChanged(CircuitAppearanceEvent.ALL_TYPES);
        }
    }

    private void recomputeDefaultAppearance() {
        if (isDefault) {
            List<CanvasObject> shapes = DefaultAppearance.build(circuitPins.getPins());
            setObjectsForce(shapes);
        }
    }

    public Direction getFacing() {
        AppearanceAnchor anchor = findAnchor();
        if (anchor == null) {
            return Direction.EAST;
        } else {
            return anchor.getFacing();
        }
    }

    public void setObjectsForce(List<? extends CanvasObject> shapesBase) {
        // This shouldn't ever be an issue, but just to make doubly sure, we'll
        // check that the anchor and all ports are in their proper places.
        List<CanvasObject> shapes = new ArrayList<>(shapesBase);
        int size = shapes.size();
        int ports = 0;
        for (int i = size - 1; i >= 0; i--) { // count ports, move anchor to end
            CanvasObject shape = shapes.get(i);
            if (shape instanceof AppearanceAnchor) {
                if (i != size - 1) {
                    shapes.remove(i);
                    shapes.add(shape);
                }
            } else if (shape instanceof AppearancePort) {
                ports++;
            }
        }
        for (int i = (size - ports - 1) - 1; i >= 0; i--) { // move ports to top
            CanvasObject shape = shapes.get(i);
            if (shape instanceof AppearancePort) {
                shapes.remove(i);
                shapes.add(size - ports - 1, shape);
                i--;
            }
        }

        try {
            suppressRecompute = true;
            super.removeObjects(new ArrayList<>(getObjectsFromBottom()));
            super.addObjects(0, shapes);
        } finally {
            suppressRecompute = false;
        }
        fireCircuitAppearanceChanged(CircuitAppearanceEvent.ALL_TYPES);
    }

    public void paintSubcircuit(Graphics graphics, Direction facing) {
        Direction defaultFacing = getFacing();
        double rotate = 0.0;
        if (!facing.equals(defaultFacing) && graphics instanceof Graphics2D) {
            rotate = defaultFacing.toRadians() - facing.toRadians();
            ((Graphics2D) graphics).rotate(rotate);
        }
        Location offset = findAnchorLocation();
        graphics.translate(-offset.getX(), -offset.getY());
        for (CanvasObject shape : getObjectsFromBottom()) {
            if (!(shape instanceof AppearanceElement)) {
                Graphics duplicateGraphics = graphics.create();
                shape.paint(duplicateGraphics, null);
                duplicateGraphics.dispose();
            }
        }
        graphics.translate(offset.getX(), offset.getY());
        if (rotate != 0.0) {
            ((Graphics2D) graphics).rotate(-rotate);
        }
    }

    private Location findAnchorLocation() {
        AppearanceAnchor anchor = findAnchor();
        if (anchor == null) {
            return Location.create(100, 100);
        } else {
            return anchor.getLocation();
        }
    }

    private AppearanceAnchor findAnchor() {
        for (CanvasObject shape : getObjectsFromBottom()) {
            if (shape instanceof AppearanceAnchor) {
                return (AppearanceAnchor) shape;
            }
        }
        return null;
    }

    public Bounds getOffsetBounds() {
        return getBounds(true);
    }

    public Bounds getAbsoluteBounds() {
        return getBounds(false);
    }

    private Bounds getBounds(boolean relativeToAnchor) {
        Bounds bounds = null;
        Location offset = null;
        for (CanvasObject shape : getObjectsFromBottom()) {
            if (shape instanceof AppearanceElement) {
                Location location = ((AppearanceElement) shape).getLocation();
                if (shape instanceof AppearanceAnchor) {
                    offset = location;
                }
                if (bounds == null) {
                    bounds = Bounds.create(location);
                } else {
                    bounds = bounds.add(location);
                }
            } else {
                if (bounds == null) {
                    bounds = shape.getBounds();
                } else {
                    bounds = bounds.add(shape.getBounds());
                }
            }
        }
        if (bounds == null) {
            return Bounds.EMPTY_BOUNDS;
        } else if (relativeToAnchor && offset != null) {
            return bounds.translate(-offset.getX(), -offset.getY());
        } else {
            return bounds;
        }
    }

    public boolean contains(Location location) {
        Location query;
        AppearanceAnchor anchor = findAnchor();
        if (anchor == null) {
            query = location;
        } else {
            Location anchorLocation = anchor.getLocation();
            query = location.translate(anchorLocation.getX(), anchorLocation.getY());
        }
        for (CanvasObject shape : getObjectsFromBottom()) {
            if (!(shape instanceof AppearanceElement) && shape.contains(query, true)) {
                return true;
            }
        }
        return false;
    }

    public SortedMap<Location, Instance> getPortOffsets(Direction facing) {
        Location anchor = null;
        Direction defaultFacing = Direction.EAST;
        List<AppearancePort> ports = new ArrayList<>();
        for (CanvasObject shape : getObjectsFromBottom()) {
            if (shape instanceof AppearancePort) {
                ports.add((AppearancePort) shape);
            } else if (shape instanceof AppearanceAnchor) {
                AppearanceAnchor o = (AppearanceAnchor) shape;
                anchor = o.getLocation();
                defaultFacing = o.getFacing();
            }
        }

        SortedMap<Location, Instance> treeMap = new TreeMap<>();
        for (AppearancePort port : ports) {
            Location portLocation = port.getLocation();
            if (anchor != null) {
                portLocation = portLocation.translate(-anchor.getX(), -anchor.getY());
            }
            if (facing != defaultFacing) {
                portLocation = portLocation.rotate(defaultFacing, facing, 0, 0);
            }
            treeMap.put(portLocation, port.getPin());
        }
        return treeMap;
    }

    @Override
    public void addObjects(int index, Collection<? extends CanvasObject> shapes) {
        super.addObjects(index, shapes);
        checkToFirePortsChanged(shapes);
    }

    @Override
    public void addObjects(Map<? extends CanvasObject, Integer> shapes) {
        super.addObjects(shapes);
        checkToFirePortsChanged(shapes.keySet());
    }

    @Override
    public void removeObjects(Collection<? extends CanvasObject> shapes) {
        super.removeObjects(shapes);
        checkToFirePortsChanged(shapes);
    }

    @Override
    public void translateObjects(Collection<? extends CanvasObject> shapes, int deltaX, int deltaY) {
        super.translateObjects(shapes, deltaX, deltaY);
        checkToFirePortsChanged(shapes);
    }

    private void checkToFirePortsChanged(Collection<? extends CanvasObject> shapes) {
        if (affectsPorts(shapes)) {
            recomputePorts();
        }
    }

    private boolean affectsPorts(Collection<? extends CanvasObject> shapes) {
        for (CanvasObject shape : shapes) {
            if (shape instanceof AppearanceElement) {
                return true;
            }
        }
        return false;
    }

    private class MyListener implements CanvasModelListener {

        public void modelChanged(CanvasModelEvent event) {
            if (!suppressRecompute) {
                setDefaultAppearance(false);
                fireCircuitAppearanceChanged(CircuitAppearanceEvent.ALL_TYPES);
            }
        }
    }
}
