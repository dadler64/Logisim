/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.tools.move;

import com.adlerd.logger.Logger;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MoveGesture {

    private final MoveRequestListener listener;
    private final Circuit circuit;
    private final HashSet<Component> selected;
    private final HashMap<MoveRequest, MoveResult> cachedResults;
    private transient Set<ConnectionData> connections;
    private transient AvoidanceMap initAvoid;

    public MoveGesture(MoveRequestListener listener, Circuit circuit, Collection<Component> selected) {
        this.listener = listener;
        this.circuit = circuit;
        this.selected = new HashSet<>(selected);
        this.connections = null;
        this.initAvoid = null;
        this.cachedResults = new HashMap<>();
    }

    private static Set<ConnectionData> computeConnections(Circuit circuit, Set<Component> selected) {
        if (selected == null || selected.isEmpty()) {
            return Collections.emptySet();
        }

        // first identify locations that might be connected
        Set<Location> locations = new HashSet<>();
        for (Component component : selected) {
            for (EndData end : component.getEnds()) {
                locations.add(end.getLocation());
            }
        }

        // now see which of them require connection
        Set<ConnectionData> connections = new HashSet<>();
        for (Location location : locations) {
            boolean found = false;
            for (Component component : circuit.getComponents(location)) {
                if (!selected.contains(component)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                List<Wire> wirePath;
                Location wirePathStart;
                Wire lastOnPath = findWire(circuit, location, selected, null);
                if (lastOnPath == null) {
                    wirePath = Collections.emptyList();
                    wirePathStart = location;
                } else {
                    wirePath = new ArrayList<>();
                    Location currentLocation = location;
                    for (Wire wire = lastOnPath; wire != null; wire = findWire(circuit, currentLocation, selected, wire)) {
                        wirePath.add(wire);
                        currentLocation = wire.getOtherEnd(currentLocation);
                    }
                    Collections.reverse(wirePath);
                    wirePathStart = currentLocation;
                }

                Direction direction = null;
                if (lastOnPath != null) {
                    Location other = lastOnPath.getOtherEnd(location);
                    int dx = location.getX() - other.getX();
                    int dy = location.getY() - other.getY();
                    if (Math.abs(dx) > Math.abs(dy)) {
                        direction = dx > 0 ? Direction.EAST : Direction.WEST;
                    } else {
                        direction = dy > 0 ? Direction.SOUTH : Direction.NORTH;
                    }
                }
                connections.add(new ConnectionData(location, direction, wirePath, wirePathStart));
            }
        }
        return connections;
    }

    private static Wire findWire(Circuit circuit, Location location, Set<Component> ignore, Wire ignoreWire) {
        Wire wire = null;
        for (Component comp : circuit.getComponents(location)) {
            if (!ignore.contains(comp) && comp != ignoreWire) {
                if (wire == null && comp instanceof Wire) {
                    wire = (Wire) comp;
                } else {
                    return null;
                }
            }
        }
        return wire;
    }

    HashSet<Component> getSelected() {
        return selected;
    }

    AvoidanceMap getFixedAvoidanceMap() {
        AvoidanceMap initAvoid = this.initAvoid;
        if (initAvoid == null) {
            HashSet<Component> components = new HashSet<>(circuit.getNonWires());
            components.addAll(circuit.getWires());
            components.removeAll(selected);
            initAvoid = AvoidanceMap.create(components, 0, 0);
            this.initAvoid = initAvoid;
        }
        return initAvoid;
    }

    Set<ConnectionData> getConnections() {
        Set<ConnectionData> connections = this.connections;
        if (connections == null) {
            connections = computeConnections(circuit, selected);
            this.connections = connections;
        }
        return connections;
    }

    public MoveResult findResult(int dx, int dy) {
        MoveRequest request = new MoveRequest(this, dx, dy);
        synchronized (cachedResults) {
            return cachedResults.get(request);
        }
    }

    public boolean enqueueRequest(int dx, int dy) {
        MoveRequest request = new MoveRequest(this, dx, dy);
        synchronized (cachedResults) {
            Object result = cachedResults.get(request);
            if (result == null) {
                ConnectorThread.enqueueRequest(request, false);
                return true;
            } else {
                return false;
            }
        }
    }

    public MoveResult forceRequest(int dx, int dy) {
        MoveRequest request = new MoveRequest(this, dx, dy);
        ConnectorThread.enqueueRequest(request, true);
        synchronized (cachedResults) {
            MoveResult result = cachedResults.get(request);
            while (result == null) {
                try {
                    cachedResults.wait();
                } catch (InterruptedException e) {
                    Logger.debugln(e.getMessage());
                    Thread.currentThread().interrupt();
                    return null;
                }
                result = cachedResults.get(request);
            }
            return result;
        }
    }

    void notifyResult(MoveRequest request, MoveResult result) {
        synchronized (cachedResults) {
            cachedResults.put(request, result);
            cachedResults.notifyAll();
        }
        if (listener != null) {
            listener.requestSatisfied(this, request.getDeltaX(), request.getDeltaY());
        }
    }

}
