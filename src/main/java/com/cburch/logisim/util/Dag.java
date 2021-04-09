/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

public class Dag {

    private final HashMap<Object, Node> nodes = new HashMap<>();

    public Dag() {
    }

    public boolean hasPredecessors(Object data) {
        Node from = findNode(data);
        return from != null && from.numPreds != 0;
    }

    public boolean hasSuccessors(Object data) {
        Node to = findNode(data);
        return to != null && !to.successions.isEmpty();
    }

    public boolean canFollow(Object query, Object base) {
        Node queryNode = findNode(query);
        Node baseNode = findNode(base);
        if (baseNode == null || queryNode == null) {
            return !base.equals(query);
        } else {
            return canFollow(queryNode, baseNode);
        }
    }

    public boolean addEdge(Object srcData, Object dstData) {
        if (!canFollow(dstData, srcData)) {
            return false;
        }

        Node src = createNode(srcData);
        Node dst = createNode(dstData);
        if (src.successions.add(dst)) {
            ++dst.numPreds; // add since not already present
        }
        return true;
    }

    public boolean removeEdge(Object srcData, Object dstData) {
        // returns true if the edge could be removed
        Node src = findNode(srcData);
        Node dst = findNode(dstData);
        if (src == null || dst == null) {
            return false;
        }
        if (!src.successions.remove(dst)) {
            return false;
        }

        --dst.numPreds;
        if (dst.numPreds == 0 && dst.successions.isEmpty()) {
            nodes.remove(dstData);
        }
        if (src.numPreds == 0 && src.successions.isEmpty()) {
            nodes.remove(srcData);
        }
        return true;
    }

    public void removeNode(Object data) {
        Node node = findNode(data);
        if (node == null) {
            return;
        }

        for (Iterator<Node> it = node.successions.iterator(); it.hasNext(); ) {
            Node succeeding = it.next();
            --(succeeding.numPreds);
            if (succeeding.numPreds == 0 && succeeding.successions.isEmpty()) {
                it.remove();
            }
        }

        if (node.numPreds > 0) {
            nodes.values().removeIf(q -> q.successions.remove(node) && q.numPreds == 0 && q.successions.isEmpty());
        }
    }

    private Node findNode(Object data) {
        if (data == null) {
            return null;
        }
        return nodes.get(data);
    }

    private Node createNode(Object data) {
        Node node = findNode(data);
        if (node != null) {
            return node;
        }
        if (data == null) {
            return null;
        }

        node = new Node(data);
        nodes.put(data, node);
        return node;
    }

    private boolean canFollow(Node query, Node base) {
        if (base == query) {
            return false;
        }

        // mark all as unvisited
        for (Node node : nodes.values()) {
            node.isMarked = false; // will become true once reached
        }

        // Search starting at query: If base is found, then it follows
        // the query already, and so query cannot follow base.
        LinkedList<Node> fringe = new LinkedList<>();
        fringe.add(query);
        while (!fringe.isEmpty()) {
            Node n = fringe.removeFirst();
            for (Node next : n.successions) {
                if (!next.isMarked) {
                    if (next == base) {
                        return false;
                    }
                    next.isMarked = true;
                    fringe.addLast(next);
                }
            }
        }
        return true;
    }

    private static class Node {

        Object data;
        HashSet<Node> successions = new HashSet<>(); // of Nodes
        int numPreds = 0;
        boolean isMarked;

        Node(Object data) {
            this.data = data;
        }
    }
}
