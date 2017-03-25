/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.core;

import java.util.HashMap;
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
            _set(prev, distance);
        }

        public Node getPrev() {
            return _prev;
        }

        public long getDistance() {
            return _distance;
        }

        private void _set(Node prev, long distance) {
            this._prev = prev;
            this._distance = distance;
        }

    }

    private Node _minNode;
    private long _minDist;
    private final HashMap<Node, DistanceInfo> _linkedNodes = new HashMap<>();

    public CalculateNodeDijkstra(Node from, Function<Node.Link, Long> distanceCalculator) {
        HashMap<Node, DistanceInfo> pending = new HashMap<>();
        _minNode = from;
        _minDist = 0;
        pending.put(from, new DistanceInfo(from, _minDist));
        do {
            _linkedNodes.put(_minNode, pending.remove(_minNode));
            _minNode.forEachNeighbor((neighbor, link) -> {
                if (!_linkedNodes.containsKey(neighbor)) {
                    DistanceInfo di = pending.get(neighbor);
                    long newDistance = _minDist + distanceCalculator.apply(link);
                    if (di == null) {
                        pending.put(neighbor, new DistanceInfo(_minNode, newDistance));
                    } else if (newDistance < di.getDistance()) {
                        di._set(_minNode, newDistance);
                    }
                }
            });
            _minNode = null;
            pending.entrySet().forEach((entry) -> {
                long distance = entry.getValue().getDistance();
                if (_minNode == null || _minDist > distance) {
                    _minNode = entry.getKey();
                    _minDist = distance;
                }
            });
        } while (_minNode != null);

    }

    public void forEachDistance(BiConsumer<Node, DistanceInfo> con) {
        _linkedNodes.forEach(con);
    }

    public DistanceInfo getDistanceInfo(Node n) {
        return _linkedNodes.get(n);
    }
}
