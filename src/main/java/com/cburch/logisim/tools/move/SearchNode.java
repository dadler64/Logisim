/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.tools.move;

import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import java.util.Objects;

class SearchNode implements Comparable<SearchNode> {

    private static final int CROSSING_PENALTY = 20;
    private static final int TURN_PENALTY = 50;

    private final Location location;
    private final Direction direction;
    private final Location destination;
    private final ConnectionData connection;
    private final int distance;
    private final int heuristic;
    private final boolean extendsWire;
    private final SearchNode previous;

    public SearchNode(ConnectionData connection, Location source, Direction sourceDirection, Location destination) {
        this(source, sourceDirection, connection, destination, 0, sourceDirection != null, null);
    }

    private SearchNode(Location location, Direction direction, ConnectionData connection, Location destination, int distance,
        boolean extendsWire, SearchNode previous) {
        this.location = location;
        this.direction = direction;
        this.connection = connection;
        this.destination = destination;
        this.distance = distance;
        this.heuristic = distance + this.getHeuristic();
        this.extendsWire = extendsWire;
        this.previous = previous;
    }

    private int getHeuristic() {
        Location currentLocation = location;
        Location destination = this.destination;
        Direction currentDirection = direction;
        int dx = destination.getX() - currentLocation.getX();
        int dy = destination.getY() - currentLocation.getY();
        int ret = -1;
        if (extendsWire) {
            ret = -1;
            if (currentDirection == Direction.EAST) {
                if (dx > 0) {
                    ret = dx / 10 * 9 + Math.abs(dy);
                }
            } else if (currentDirection == Direction.WEST) {
                if (dx < 0) {
                    ret = -dx / 10 * 9 + Math.abs(dy);
                }
            } else if (currentDirection == Direction.SOUTH) {
                if (dy > 0) {
                    ret = Math.abs(dx) + dy / 10 * 9;
                }
            } else if (currentDirection == Direction.NORTH) {
                if (dy < 0) {
                    ret = Math.abs(dx) - dy / 10 * 9;
                }
            }
        }
        if (ret < 0) {
            ret = Math.abs(dx) + Math.abs(dy);
        }
        boolean penalizeDoubleTurn = false;
        if (currentDirection == Direction.EAST) {
            penalizeDoubleTurn = dx < 0;
        } else if (currentDirection == Direction.WEST) {
            penalizeDoubleTurn = dx > 0;
        } else if (currentDirection == Direction.NORTH) {
            penalizeDoubleTurn = dy > 0;
        } else if (currentDirection == Direction.SOUTH) {
            penalizeDoubleTurn = dy < 0;
        } else if (currentDirection == null) {
            if (dx != 0 || dy != 0) {
                ret += TURN_PENALTY;
            }
        }
        if (penalizeDoubleTurn) {
            ret += 2 * TURN_PENALTY;
        } else if (dx != 0 && dy != 0) {
            ret += TURN_PENALTY;
        }
        return ret;
    }

    public SearchNode next(Direction moveDirection, boolean crossing) {
        int newDistance = distance;
        Direction connectionDirection = connection.getDirection();
        Location nextLocation = location.translate(moveDirection, 10);
        boolean extendsWire = this.extendsWire && moveDirection == connectionDirection;
        if (extendsWire) {
            newDistance += 9;
        } else {
            newDistance += 10;
        }
        if (crossing) {
            newDistance += CROSSING_PENALTY;
        }
        if (moveDirection != direction) {
            newDistance += TURN_PENALTY;
        }
        if (nextLocation.getX() < 0 || nextLocation.getY() < 0) {
            return null;
        } else {
            return new SearchNode(nextLocation, moveDirection, connection, destination,
                newDistance, extendsWire, this);
        }
    }

    public boolean isStart() {
        return previous == null;
    }

    public boolean isDestination() {
        return destination.equals(location);
    }

    public SearchNode getPrevious() {
        return previous;
    }

    public int getDistance() {
        return distance;
    }

    public Location getLocation() {
        return location;
    }

    public Direction getDirection() {
        return direction;
    }

    public int getHeuristicValue() {
        return heuristic;
    }

    public Location getDestination() {
        return destination;
    }

    public boolean isExtendingWire() {
        return extendsWire;
    }

    public ConnectionData getConnection() {
        return connection;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof SearchNode) {
            SearchNode node = (SearchNode) other;
            return this.location.equals(node.location)
                && (Objects.equals(this.direction, node.direction))
                && this.destination.equals(node.destination);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int directionHash = direction == null ? 0 : direction.hashCode();
        return ((location.hashCode() * 31) + directionHash) * 31 + destination.hashCode();
    }

    public int compareTo(SearchNode node) {
        int ret = this.heuristic - node.heuristic;

        if (ret == 0) {
            return this.hashCode() - node.hashCode();
        } else {
            return ret;
        }
    }

    @Override
    public String toString() {
        return location + "/" + (direction == null ? "null" : direction.toString()) + (extendsWire ? "+" : "-")
            + "/" + destination + ":" + distance + "+" + (heuristic - distance);
    }
}
