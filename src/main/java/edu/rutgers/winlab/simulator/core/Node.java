package edu.rutgers.winlab.simulator.core;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class Node {

    public static final long DEFAULT_PROCESS_DELAY = 30 * EventQueue.MICRO_SECOND;

    public static final long getSendTime(ISerializable packet, int bandwidthInBps) {
        return packet.getSize() * EventQueue.SECOND / bandwidthInBps;
    }

    public static void connectNodes(Node n1, Node n2,
            SimulatorQueue<ISerializable> n1ToN2Queue,
            SimulatorQueue<ISerializable> n2ToN1Queue,
            int bandwidthInBps, long delay) {
        if (n1._neighbors.containsKey(n2)) {
            throw new IllegalArgumentException(String.format("%s and %s are neighbors already", n1, n2));
        }
        n1._addLink(n2, n1ToN2Queue, bandwidthInBps, delay);
        n2._addLink(n1, n2ToN1Queue, bandwidthInBps, delay);
    }

    public static void disconnectNodes(Node n1, Node n2) {
        if (!n1._neighbors.containsKey(n2)) {
            throw new IllegalArgumentException(String.format("%s and %s not neighbors", n1, n2));
        }
        n1._removeLink(n2);
        n2._removeLink(n1);
    }

    private final String _name;
    private final HashMap<Node, Link> _neighbors = new HashMap<>();
    private final EventHandlerQueue<ISerializable> _incomingQueue;

    public Node(String name, SimulatorQueue<ISerializable> innerIncomingQueue) {
        _name = name;
        _incomingQueue = new EventHandlerQueue<>(innerIncomingQueue, this::_innerProcessPacket);
    }

    public String getName() {
        return _name;
    }

    public void forEachNeighbor(BiConsumer<Node, Link> consumer) {
        _neighbors.forEach(consumer);
    }

    @Override
    public String toString() {
        return String.format("Node:{Name:%s}", _name);
    }

    protected abstract long _processPacket(Serial<ISerializable> s, ISerializable param);

    protected abstract void _handleFailedPacket(ISerializable packet);

    protected void _sendPacket(ISerializable packet, Node target, boolean prioritized) {
        Link l = _neighbors.get(target);
        if (l == null) {
//            throw new IllegalArgumentException(String.format("%s cannot send packet to %s, not neighbor", this, target));
            _handleFailedPacket(packet);
        } else {
            l._sendPacket(packet, prioritized);
        }
    }

    private void _receivePacket(ISerializable packet) {
        _incomingQueue.enqueue(packet, false);
    }

    private long _innerProcessPacket(Serial<ISerializable> s, ISerializable param) {
        s.addEvent(this::_processPacket, param);
        return DEFAULT_PROCESS_DELAY;
    }

    private void _addLink(Node target, SimulatorQueue<ISerializable> innerOutgoingQueue, int bandwidthInBps, long delay) {
        _neighbors.put(target, new Link(target, innerOutgoingQueue, bandwidthInBps, delay));
    }

    private void _removeLink(Node target) {
        Link l = _neighbors.remove(target);
        l._disConnect(this::_handleFailedPacket);
    }

    public class Link {

        private final Node _targetNode;
        private final int _bandwidthInBps;
        private final long _delay;
        private final EventHandlerQueue<ISerializable> _outgoingQueue;
        private final LinkedList<ISerializable> _inTransit = new LinkedList<>();
        private long _totalTraffic = 0;
        private int _totalPacketCount = 0;
        private boolean _expired;

        public Link(Node targetNode, SimulatorQueue<ISerializable> innerOutgoingQueue, int bandwidthInBps, long delay) {
            this._targetNode = targetNode;
            this._bandwidthInBps = bandwidthInBps;
            this._delay = delay;
            this._expired = false;
            _outgoingQueue = new EventHandlerQueue<>(innerOutgoingQueue, this::_processSendPacket);
        }

        public boolean isExpired() {
            return _expired;
        }

        public Node getTargetNode() {
            return _targetNode;
        }

        public long getDelay() {
            return _delay;
        }

        public int getBandwidthInBps() {
            return _bandwidthInBps;
        }

        @Override
        public String toString() {
            return String.format("L{Target:%s,Delay:%d,BW:%d,QSIZE:%d}", _targetNode, _delay, _bandwidthInBps, _outgoingQueue.getSize());
        }

        public long getTotalTraffic() {
            return _totalTraffic;
        }

        public int getTotalPacketCount() {
            return _totalPacketCount;
        }

        private long _processSendPacket(Serial<ISerializable> s, ISerializable param) {
            _inTransit.addLast(param);
            long transmitTime = getSendTime(param, _bandwidthInBps);
            EventQueue.addEvent(EventQueue.now() + transmitTime + _delay, this::_processPacketArriveOnOther, param);

//                System.out.printf("[%d] L %s->%s, finish:%d, receive:%d%n", EventQueue.now(), Node.this, _targetNode,
//                        EventQueue.now() + transmitTime, EventQueue.now() + transmitTime + _delay);
            return transmitTime;
        }

        private void _processPacketArriveOnOther(Object... args) {
            if (_expired) {
                return;
            }
            ISerializable pkt = _inTransit.removeFirst();
            if (pkt != args[0]) {
                throw new RuntimeException("Should not reach here... ");
            }
            _totalPacketCount++;
            _totalTraffic += pkt.getSize();
            _targetNode._receivePacket(pkt);
        }

        private void _sendPacket(ISerializable packet, boolean proiritized) {
            _outgoingQueue.enqueue(packet, proiritized);
        }

        private void _disConnect(Consumer<ISerializable> failedPacketHandler) {
            _expired = true;
            while (!_inTransit.isEmpty()) {
                failedPacketHandler.accept(_inTransit.removeFirst());
            }
            // iterate through _outgoing queue and then call failed packet handler, clear outgoing queue
            //_outgoingQueue.clear();
            _outgoingQueue.clear(failedPacketHandler);
        }
    }

}
