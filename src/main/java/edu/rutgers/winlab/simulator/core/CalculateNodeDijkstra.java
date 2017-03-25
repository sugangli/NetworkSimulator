/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 *
 * @author wuyang
 */
public class CalculateNodeDijkstra {

    public static final class DistanceInfo {

        private Node _prev;
        private long _distance;

        public DistanceInfo(Node prev, long distance) {
            set(prev, distance);
        }

        public Node getPrev() {
            return _prev;
        }

        public long getDistance() {
            return _distance;
        }

        private void set(Node prev, long distance) {
            this._prev = prev;
            this._distance = distance;
        }

    }

    private Node minNode;
    private long minDist;
    private final HashMap<Node, DistanceInfo> linkedNodes = new HashMap<>();

    public CalculateNodeDijkstra(Node from, Function<Node.Link, Long> distanceCalculator) {
        HashMap<Node, DistanceInfo> pending = new HashMap<>();
        minNode = from;
        minDist = 0;
        pending.put(from, new DistanceInfo(from, minDist));
        do {
            linkedNodes.put(minNode, pending.remove(minNode));
            minNode.forEachNeighbor((neighbor, link) -> {
                if (!linkedNodes.containsKey(neighbor)) {
                    DistanceInfo di = pending.get(neighbor);
                    long newDistance = minDist + distanceCalculator.apply(link);
                    if (di == null) {
                        pending.put(neighbor, new DistanceInfo(minNode, newDistance));
                    } else if (newDistance < di.getDistance()) {
                        di.set(minNode, newDistance);
                    }
                }
            });
            minNode = null;
            pending.entrySet().forEach((entry) -> {
                long distance = entry.getValue().getDistance();
                if (minNode == null || minDist > distance) {
                    minNode = entry.getKey();
                    minDist = distance;
                }
            });
        } while (minNode != null);

    }

    public void forEachDistance(BiConsumer<Node, DistanceInfo> con) {
        linkedNodes.forEach(con);
    }
    
    public DistanceInfo getDistanceInfo(Node n) {
        return linkedNodes.get(n);
    }
}
