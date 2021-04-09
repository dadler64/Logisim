/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.tools.move;

import com.cburch.logisim.circuit.ReplacementMap;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

class Connector {

    static final String ALLOW_NEITHER = "neither";
    static final String ALLOW_VERTICAL = "vert";
    static final String ALLOW_HORIZONTAL = "horz";
    private static final int MAX_SECONDS = 10;
    private static final int MAX_ORDERING_TRIES = 10;
    private static final int MAX_SEARCH_ITERATIONS = 20000;

    private Connector() {
    }

    static MoveResult computeWires(MoveRequest req) {
        MoveGesture gesture = req.getMoveGesture();
        int dx = req.getDeltaX();
        int dy = req.getDeltaY();
        ArrayList<ConnectionData> baseConnects;
        baseConnects = new ArrayList<>(gesture.getConnections());
        ArrayList<ConnectionData> impossible = pruneImpossible(baseConnects, gesture.getFixedAvoidanceMap(), dx, dy);

        AvoidanceMap selAvoid = AvoidanceMap.create(gesture.getSelected(), dx, dy);
        HashMap<ConnectionData, Set<Location>> pathLocations = new HashMap<>();
        HashMap<ConnectionData, List<SearchNode>> initNodes = new HashMap<>();
        for (ConnectionData conn : baseConnects) {
            HashSet<Location> connectionLocations = new HashSet<>();
            ArrayList<SearchNode> connectionNodes = new ArrayList<>();
            processConnection(conn, dx, dy, connectionLocations, connectionNodes, selAvoid);
            pathLocations.put(conn, connectionLocations);
            initNodes.put(conn, connectionNodes);
        }

        MoveResult bestResult = null;
        int tries;
        switch (baseConnects.size()) {
            case 0:
                tries = 0;
                break;
            case 1:
                tries = 1;
                break;
            case 2:
                tries = 2;
                break;
            case 3:
                tries = 8;
                break;
            default:
                tries = MAX_ORDERING_TRIES;
        }
        long stopTime = System.currentTimeMillis() + MAX_SECONDS * 1000;
        for (int tryNum = 0; tryNum < tries && stopTime - System.currentTimeMillis() > 0; tryNum++) {
            if (ConnectorThread.isOverrideRequested()) {
                return null;
            }
            ArrayList<ConnectionData> connects;
            connects = new ArrayList<>(baseConnects);
            if (tryNum < 2) {
                sortConnects(connects, dx, dy);
                if (tryNum == 1) {
                    Collections.reverse(connects);
                }
            } else {
                Collections.shuffle(connects);
            }

            MoveResult candidate = tryList(req, gesture, connects, dx, dy, pathLocations, initNodes, stopTime);
            if (candidate == null) {
                return null;
            } else if (bestResult == null) {
                bestResult = candidate;
            } else {
                int unsatisfied1 = bestResult.getUnsatisfiedConnections().size();
                int unsatisfied2 = candidate.getUnsatisfiedConnections().size();
                if (unsatisfied2 < unsatisfied1) {
                    bestResult = candidate;
                } else if (unsatisfied2 == unsatisfied1) {
                    int dist1 = bestResult.getTotalDistance();
                    int dist2 = candidate.getTotalDistance();
                    if (dist2 < dist1) {
                        bestResult = candidate;
                    }
                }
            }
        }
        if (bestResult == null) { // should only happen for no connections
            bestResult = new MoveResult(req, new ReplacementMap(), impossible, 0);
        } else {
            bestResult.addUnsatisfiedConnections(impossible);
        }
        return bestResult;
    }

    private static ArrayList<ConnectionData> pruneImpossible(
        ArrayList<ConnectionData> connections, AvoidanceMap avoid, int dx, int dy) {
        ArrayList<Wire> pathWires = new ArrayList<>();
        for (ConnectionData connection : connections) {
            pathWires.addAll(connection.getWirePath());
        }

        ArrayList<ConnectionData> impossible = new ArrayList<>();
        for (Iterator<ConnectionData> iterator = connections.iterator(); iterator.hasNext(); ) {
            ConnectionData connection = iterator.next();
            Location destination = connection.getLocation().translate(dx, dy);
            if (avoid.get(destination) != null) {
                boolean isInPath = false;
                for (Wire wire : pathWires) {
                    if (wire.contains(destination)) {
                        isInPath = true;
                        break;
                    }
                }
                if (!isInPath) {
                    iterator.remove();
                    impossible.add(connection);
                }
            }
        }
        return impossible;
    }

    /**
     * Creates a list of the connections to make, sorted according to their
     * location. If, for example, we are moving an east-facing AND gate
     * southeast, then we prefer to connect the inputs from the top down to
     * minimize the chances that the created wires will interfere with each
     * other - but if we are moving that gate northeast, we prefer to connect
     * the inputs from the bottom up.
     */
    private static void sortConnects(ArrayList<ConnectionData> connections, final int dx, final int dy) {
        connections.sort((ac, bc) -> {
            Location a = ac.getLocation();
            Location b = bc.getLocation();
            int abx = a.getX() - b.getX();
            int aby = a.getY() - b.getY();
            return abx * dx + aby * dy;
        });
    }

    private static void processConnection(ConnectionData connection, int dx, int dy, HashSet<Location> connectionLocations,
        ArrayList<SearchNode> connectionNodes, AvoidanceMap selectionAvoid) {
        Location current = connection.getLocation();
        Location destination = current.translate(dx, dy);
        if (selectionAvoid.get(current) == null) {
            Direction preferred = connection.getDirection();
            if (preferred == null) {
                if (Math.abs(dx) > Math.abs(dy)) {
                    preferred = dx > 0 ? Direction.EAST : Direction.WEST;
                } else {
                    preferred = dy > 0 ? Direction.SOUTH : Direction.NORTH;
                }
            }

            connectionLocations.add(current);
            connectionNodes.add(new SearchNode(connection, current, preferred, destination));
        }

        for (Wire wire : connection.getWirePath()) {
            for (Location location : wire) {
                if (selectionAvoid.get(location) == null || location.equals(destination)) {
                    boolean added = connectionLocations.add(location);
                    if (added) {
                        Direction direction = null;
                        if (wire.endsAt(location)) {
                            if (wire.isVertical()) {
                                int y0 = location.getY();
                                int y1 = wire.getOtherEnd(location).getY();
                                direction = y0 < y1 ? Direction.NORTH : Direction.SOUTH;
                            } else {
                                int x0 = location.getX();
                                int x1 = wire.getOtherEnd(location).getX();
                                direction = x0 < x1 ? Direction.WEST : Direction.EAST;
                            }
                        }
                        connectionNodes.add(new SearchNode(connection, location, direction, destination));
                    }
                }
            }
        }
    }

    private static MoveResult tryList(MoveRequest request, MoveGesture gesture, ArrayList<ConnectionData> connections,
        int dx, int dy, HashMap<ConnectionData, Set<Location>> pathLocations,
        HashMap<ConnectionData, List<SearchNode>> initNodes, long stopTime) {
        AvoidanceMap avoid = gesture.getFixedAvoidanceMap().cloneMap();
        avoid.markAll(gesture.getSelected(), dx, dy);

        ReplacementMap replacements = new ReplacementMap();
        ArrayList<ConnectionData> unconnected = new ArrayList<>();
        int totalDistance = 0;
        for (ConnectionData connection : connections) {
            if (ConnectorThread.isOverrideRequested()) {
                return null;
            }
            if (System.currentTimeMillis() - stopTime > 0) {
                unconnected.add(connection);
                continue;
            }
            List<SearchNode> connectionNodes = initNodes.get(connection);
            Set<Location> connectionPathLocations = pathLocations.get(connection);
            SearchNode node = findShortestPath(connectionNodes, connectionPathLocations, avoid);
            if (node != null) { // normal case - a path was found
                totalDistance += node.getDistance();
                ArrayList<Location> path = convertToPath(node);
                processPath(path, connection, avoid, replacements, connectionPathLocations);
            } else if (ConnectorThread.isOverrideRequested()) {
                return null; // search was aborted: return null to indicate this
            } else {
                unconnected.add(connection);
            }
        }
        return new MoveResult(request, replacements, unconnected, totalDistance);
    }

    private static SearchNode findShortestPath(List<SearchNode> nodes, Set<Location> pathLocations, AvoidanceMap avoid) {
        PriorityQueue<SearchNode> queue = new PriorityQueue<>(nodes);
        HashSet<SearchNode> visited = new HashSet<>();
        int iterations = 0;
        while (!queue.isEmpty() && iterations < MAX_SEARCH_ITERATIONS) {
            iterations++;
            SearchNode node = queue.remove();
            if (iterations % 64 == 0 && ConnectorThread.isOverrideRequested() || node == null) {
                return null;
            }
            if (node.isDestination()) {
                return node;
            }
            boolean added = visited.add(node);
            if (!added) {
                continue;
            }
            Location nodeLocation = node.getLocation();
            Direction nodeDirection = node.getDirection();
            int neighbors = 3;
            Object allowed = avoid.get(nodeLocation);
            if (allowed != null && node.isStart() && pathLocations.contains(nodeLocation)) {
                allowed = null;
            }
            if (allowed == ALLOW_NEITHER) {
                neighbors = 0;
            } else if (allowed == ALLOW_VERTICAL) {
                if (nodeDirection == null) {
                    nodeDirection = Direction.NORTH;
                    neighbors = 2;
                } else if (nodeDirection == Direction.NORTH || nodeDirection == Direction.SOUTH) {
                    neighbors = 1;
                } else {
                    neighbors = 0;
                }
            } else if (allowed == ALLOW_HORIZONTAL) {
                if (nodeDirection == null) {
                    nodeDirection = Direction.EAST;
                    neighbors = 2;
                } else if (nodeDirection == Direction.EAST || nodeDirection == Direction.WEST) {
                    neighbors = 1;
                } else {
                    neighbors = 0;
                }
            } else {
                if (nodeDirection == null) {
                    nodeDirection = Direction.NORTH;
                    neighbors = 4;
                } else {
                    neighbors = 3;
                }
            }
            for (int i = 0; i < neighbors; i++) {
                Direction oDirection;
                switch (i) {
                    case 0:
                        oDirection = nodeDirection;
                        break;
                    case 1:
                        oDirection = neighbors == 2 ? nodeDirection.reverse() : nodeDirection.getLeft();
                        break;
                    case 2:
                        oDirection = nodeDirection.getRight();
                        break;
                    default: // must be 3
                        oDirection = nodeDirection.reverse();
                }
                SearchNode oNode = node.next(oDirection, allowed != null);
                if (oNode != null && !visited.contains(oNode)) {
                    queue.add(oNode);
                }
            }
        }
        return null;
    }

    private static ArrayList<Location> convertToPath(SearchNode last) {
        SearchNode next = last;
        SearchNode prev = last.getPrevious();
        ArrayList<Location> ret = new ArrayList<>();
        ret.add(next.getLocation());
        while (prev != null) {
            if (prev.getDirection() != next.getDirection()) {
                ret.add(prev.getLocation());
            }
            next = prev;
            prev = prev.getPrevious();
        }
        if (!ret.get(ret.size() - 1).equals(next.getLocation())) {
            ret.add(next.getLocation());
        }
        Collections.reverse(ret);
        return ret;
    }

    private static void processPath(ArrayList<Location> pathLocations, ConnectionData connection, AvoidanceMap avoid,
        ReplacementMap replacements, Set<Location> unmarkable) {
        Iterator<Location> pathIterator = pathLocations.iterator();
        Location location0 = pathIterator.next();
        if (!location0.equals(connection.getLocation())) {
            Location pathLocation = connection.getWirePathStart();
            boolean found = location0.equals(pathLocation);
            for (Wire wire : connection.getWirePath()) {
                Location nextLocation = wire.getOtherEnd(pathLocation);
                if (found) { // existing wire will be removed
                    replacements.remove(wire);
                    avoid.unmarkWire(wire, nextLocation, unmarkable);
                } else if (wire.contains(location0)) { // wires after this will be removed
                    found = true;
                    if (!location0.equals(nextLocation)) {
                        avoid.unmarkWire(wire, nextLocation, unmarkable);
                        Wire shortenedWire = Wire.create(pathLocation, location0);
                        replacements.replace(wire, shortenedWire);
                        avoid.markWire(shortenedWire, 0, 0);
                    }
                }
                pathLocation = nextLocation;
            }
        }
        while (pathIterator.hasNext()) {
            Location location1 = pathIterator.next();
            Wire newWire = Wire.create(location0, location1);
            replacements.add(newWire);
            avoid.markWire(newWire, 0, 0);
            location0 = location1;
        }
    }
}
