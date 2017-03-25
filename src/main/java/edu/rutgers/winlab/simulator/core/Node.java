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
        n1.addLink(n2, n1ToN2Queue, bandwidthInBps, delay);
        n2.addLink(n1, n2ToN1Queue, bandwidthInBps, delay);
    }

    public static void disconnectNodes(Node n1, Node n2) {
        if (!n1._neighbors.containsKey(n2)) {
            throw new IllegalArgumentException(String.format("%s and %s not neighbors", n1, n2));
        }
        n1.removeLink(n2);
        n2.removeLink(n1);
    }

    private final String _name;
    private final HashMap<Node, Link> _neighbors = new HashMap<>();
    private final EventHandlerQueue<ISerializable> _incomingQueue;

    public Node(String name, SimulatorQueue<ISerializable> innerIncomingQueue) {
        _name = name;
        Serial.SerialAction<ISerializable> processPacketAction = (s, param) -> {
            return processPacket(s, param);
        };
        _incomingQueue = new EventHandlerQueue<>(innerIncomingQueue, (s, parameter) -> {
            s.addEvent(processPacketAction, parameter);
            return DEFAULT_PROCESS_DELAY;
        });
    }

    protected abstract long processPacket(Serial<ISerializable> s, ISerializable param);

    protected abstract void handleFailedPacket(ISerializable packet);

    private void receivePacket(ISerializable packet) {
        _incomingQueue.enqueue(packet, false);
    }

    public String getName() {
        return _name;
    }

    protected void sendPacket(ISerializable packet, Node target, boolean prioritized) {
        Link l = _neighbors.get(target);
        if (l == null) {
//            throw new IllegalArgumentException(String.format("%s cannot send packet to %s, not neighbor", this, target));
            handleFailedPacket(packet);
        } else {
            l.sendPacket(packet, prioritized);
        }
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

    private void removeLink(Node target) {
        Link l = _neighbors.remove(target);
        l.disConnect((s) -> handleFailedPacket(s));
    }

    public class Link {

        private final Node _targetNode;
        private final int _bandwidthInBps;
        private final long _delay;
        private final EventHandlerQueue<ISerializable> _outgoingQueue;
        private final Serial.SerialAction<ISerializable> _linkPacketProcessor;
        private final LinkedList<ISerializable> _inTransit = new LinkedList<>();
        private long _totalTraffic = 0;
        private int _totalPacketCount = 0;
        private boolean _expired;

        public Link(Node targetNode, SimulatorQueue<ISerializable> innerOutgoingQueue, int bandwidthInBps, long delay) {
            this._targetNode = targetNode;
            this._bandwidthInBps = bandwidthInBps;
            this._delay = delay;
            this._expired = false;
            this._linkPacketProcessor = (s, param) -> processSendPacket(s, param);
            _outgoingQueue = new EventHandlerQueue<>(innerOutgoingQueue, _linkPacketProcessor);
        }

        private long processSendPacket(Serial<ISerializable> s, ISerializable param) {
            _inTransit.addLast(param);
            long transmitTime = getSendTime(param, _bandwidthInBps);
            EventQueue.addEvent(EventQueue.now() + transmitTime + _delay, (args) -> processPacketArriveOnOther(args), param);

//                System.out.printf("[%d] L %s->%s, finish:%d, receive:%d%n", EventQueue.now(), Node.this, _targetNode,
//                        EventQueue.now() + transmitTime, EventQueue.now() + transmitTime + _delay);
            return transmitTime;
        }

        private void processPacketArriveOnOther(Object... args) {
            if (_expired) {
                return;
            }
            ISerializable pkt = _inTransit.removeFirst();
            if (pkt != args[0]) {
                throw new RuntimeException("Should not reach here... ");
            }
            _totalPacketCount++;
            _totalTraffic += pkt.getSize();
            _targetNode.receivePacket(pkt);
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
        public void disConnect(Consumer<ISerializable> failedPacketHandler) {
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
