package edu.rutgers.winlab.simulator.core;

import java.util.HashMap;
import java.util.function.BiConsumer;

public abstract class Node {

    public static final long DEFAULT_PROCESS_DELAY = 30 * EventQueue.MICRO_SECOND;

    public static final long getSendTime(ISerializable packet, int bandwidthInBps) {
        return packet.getSize() * EventQueue.SECOND / bandwidthInBps;
    }

    public static void linkNodes(Node n1, Node n2,
            SimulatorQueue<ISerializable> n1ToN2Queue,
            SimulatorQueue<ISerializable> n2ToN1Queue,
            int bandwidthInBps, long delay) {
        if (n1._neighbors.containsKey(n2)) {
            throw new IllegalArgumentException(String.format("%s and %s are neighbors already", n1, n2));
        }
        n1.addLink(n2, n1ToN2Queue, bandwidthInBps, delay);
        n2.addLink(n1, n2ToN1Queue, bandwidthInBps, delay);
    }

    public static void disconnectNodes(Node n1, Node n2) {
        if (!n1._neighbors.containsKey(n2)) {
            throw new IllegalArgumentException(String.format("%s and %s not neighbors", n1, n2));
        }
        //TODO
    }

    private final String _name;
    private final HashMap<Node, Link> _neighbors = new HashMap<>();
    private final EventHandlerQueue<ISerializable> _incomingQueue;
    private final Action _recevePacketAction;

    public Node(String name, SimulatorQueue<ISerializable> innerIncomingQueue) {
        _name = name;
        Serial.SerialAction<ISerializable> processPacketAction = (s, param) -> {
            return processPacket(s, param);
        };
        _incomingQueue = new EventHandlerQueue<>(innerIncomingQueue, (s, parameter) -> {
            s.addEvent(processPacketAction, parameter);
            return DEFAULT_PROCESS_DELAY;
        });
        _recevePacketAction = (args) -> {
            _incomingQueue.enqueue((ISerializable) args[0], false);
        };
    }

    protected abstract long processPacket(Serial<ISerializable> s, ISerializable param);

    public String getName() {
        return _name;
    }

    protected void sendPacket(ISerializable packet, Node target, boolean prioritized) {
        Link l = _neighbors.get(target);
        if (l == null) {
            throw new IllegalArgumentException(String.format("%s cannot send packet to %s, not neighbor", this, target));
        }
        l.sendPacket(packet, prioritized);
    }

    public void forEachNeighbor(BiConsumer<Node, Link> consumer) {
        _neighbors.forEach(consumer);
    }

    @Override
    public String toString() {
        return String.format("Node:{Name:%s}", _name);
    }

    private void addLink(Node target, SimulatorQueue<ISerializable> innerOutgoingQueue, int bandwidthInBps, long delay) {
        _neighbors.put(target, new Link(target, innerOutgoingQueue, bandwidthInBps, delay));
    }

    public class Link {

        private final Node _targetNode;
        private final int _bandwidthInBps;
        private final long _delay;
        private final EventHandlerQueue<ISerializable> _outgoingQueue;
        private final Serial.SerialAction<ISerializable> _linkPacketProcessor;
        private long _totalTraffic = 0;
        private int _totalPacketCount = 0;
        private boolean _expired;

        public Link(Node targetNode, SimulatorQueue<ISerializable> innerOutgoingQueue, int bandwidthInBps, long delay) {
            this._targetNode = targetNode;
            this._bandwidthInBps = bandwidthInBps;
            this._delay = delay;
            this._expired = false;
            this._linkPacketProcessor = (s, param) -> {
                _totalPacketCount++;
                _totalTraffic += param.getSize();
                long transmitTime = getSendTime(param, _bandwidthInBps);
                EventQueue.addEvent(EventQueue.now() + transmitTime + _delay, _targetNode._recevePacketAction, param);

//                System.out.printf("[%d] L %s->%s, finish:%d, receive:%d%n", EventQueue.now(), Node.this, _targetNode,
//                        EventQueue.now() + transmitTime, EventQueue.now() + transmitTime + _delay);
                return transmitTime;
            };
            _outgoingQueue = new EventHandlerQueue<>(innerOutgoingQueue, _linkPacketProcessor);
        }

        public boolean isExpired() {
            return _expired;
        }

        public Node getTargetNode() {
            return _targetNode;
        }

        public double getDelay() {
            return _delay;
        }

        @Override
        public String toString() {
            return String.format("L{Target:%s,Delay:%d,BW:%d,QSIZE:%d}", _targetNode, _delay, _bandwidthInBps, _outgoingQueue.getSize());
        }

        public void sendPacket(ISerializable packet, boolean proiritized) {
            _outgoingQueue.enqueue(packet, proiritized);
        }

        public long getTotalTraffic() {
            return _totalTraffic;
        }

        public int getTotalPacketCount() {
            return _totalPacketCount;
        }

        //TODO
        public void disConnect() {
            _expired = true;
        }
    }

}
