/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.core;

import java.util.HashMap;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author wuyang
 */
public class NodeTest {

    public NodeTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private static class RandomPayload implements ISerializable {

        private final int _id;
        private final Node _from;
        private final Node _to;
        private final int _size;

        public RandomPayload(int id, Node from, Node to, int size) {
            _id = id;
            _from = from;
            _to = to;
            _size = size;
        }

        public int getId() {
            return _id;
        }

        public Node getFrom() {
            return _from;
        }

        public Node getTo() {
            return _to;
        }

        @Override
        public int getSize() {
            return _size;
        }

        @Override
        public String toString() {
            return String.format("RPLD:{ID=%d,FROM=%s,TO=%s,SIZE=%d}", _id, _from, _to, _size);
        }

    }

    private static class NodeServer extends ReliableEndHost {

        public NodeServer(String name, SimulatorQueue<ISerializable> innerIncomingQueue) {
            super(name, innerIncomingQueue);
        }

        @Override
        protected long _processPacket(Serial<ISerializable> s, ISerializable param) {
            System.out.printf("[%d] AS %s received %s%n", EventQueue.now(), this, param);
            RandomPayload orig = (RandomPayload) param;
            RandomPayload ret = new RandomPayload(orig.getId(), this, orig.getFrom(), orig.getSize() * 2);

            s.addLastEvent((ss, p) -> {
                System.out.printf("[%d] AS %s send: %s%n", EventQueue.now(), NodeServer.this, p);
                sendPacket(p, false);
                return 0 * EventQueue.MILLI_SECOND;
            }, ret);
            return 1 * EventQueue.MILLI_SECOND;
        }
    }

    private static class NodeClient extends EndHost {

        public NodeClient(String name, SimulatorQueue<ISerializable> innerIncomingQueue) {
            super(name, innerIncomingQueue);
        }

        @Override
        protected long _processPacket(Serial<ISerializable> s, ISerializable param) {
            System.out.printf("[%d] AC %s received %s%n", EventQueue.now(), this, param);
            return 0;
        }

        @Override
        protected void _handleFailedPacket(ISerializable packet) {
            System.out.printf("[%d] AC Lost: %s%n", EventQueue.now(), packet);
        }
    }

    public class NodeRouter extends Node {

        //key: target, value: nextHop
        private final HashMap<Node, Node> _routingTable = new HashMap<>();
        private final long _processingDelay;

        public NodeRouter(String name, long processingDelay, SimulatorQueue<ISerializable> innerIncomingQueue) {
            super(name, innerIncomingQueue);
            _processingDelay = processingDelay;
        }

        public void addRouting(Node target, Node nextHop) {
            _routingTable.put(target, nextHop);
        }

        @Override
        protected long _processPacket(Serial<ISerializable> s, ISerializable param) {
            s.addEvent((Serial<ISerializable> ss, ISerializable p) -> {
                RandomPayload rp = (RandomPayload) p;
                Node dest = _routingTable.get(rp.getTo());
                _sendPacket(rp, dest, false);
                return 0;
            }, param);
            return _processingDelay;
        }

        @Override
        protected void _handleFailedPacket(ISerializable packet) {
            System.out.printf("[%d] R Lost: %s%n", EventQueue.now(), packet);
        }
    }

    @Test
    public void test1() {
        EventQueue.reset();

        NodeRouter r1 = new NodeRouter("R1", EventQueue.MILLI_SECOND, new FIFOQueue<>("R1In", Integer.MAX_VALUE));
        NodeRouter r2 = new NodeRouter("R2", EventQueue.MILLI_SECOND, new FIFOQueue<>("R2In", Integer.MAX_VALUE));
        NodeRouter r3 = new NodeRouter("R3", EventQueue.MILLI_SECOND, new FIFOQueue<>("R3In", Integer.MAX_VALUE));

        Node.connectNodes(r1, r2, new FIFOQueue<>("R1->R2", Integer.MAX_VALUE), new FIFOQueue<>("R2->R1", Integer.MAX_VALUE), 1 * ISerializable.M_BIT, 1 * EventQueue.MILLI_SECOND);
        Node.connectNodes(r1, r3, new FIFOQueue<>("R1->R3", Integer.MAX_VALUE), new FIFOQueue<>("R3->R1", Integer.MAX_VALUE), 2 * ISerializable.M_BIT, 6 * EventQueue.MILLI_SECOND);

        System.out.printf("Neighbors of %s%n", r1);
        r1.forEachNeighbor((n, l) -> {
            System.out.printf("%s:%s%n", n, l);
        });
    }

    @Test
    public void test2() {
        EventQueue.reset();

        EndHost n1 = new NodeClient("N1", new FIFOQueue<>("N1In", Integer.MAX_VALUE));
        EndHost n2 = new NodeServer("N2", new FIFOQueue<>("N2In", Integer.MAX_VALUE));
        NodeRouter r = new NodeRouter("R", EventQueue.MILLI_SECOND, new FIFOQueue<>("RIN", Integer.MAX_VALUE));

        n1.move(r, new FIFOQueue<>("N1->R", Integer.MAX_VALUE), new FIFOQueue<>("R->N1", Integer.MAX_VALUE), 1 * ISerializable.M_BIT, 2 * EventQueue.MILLI_SECOND);
        n2.move(r, new FIFOQueue<>("N2->R", Integer.MAX_VALUE), new FIFOQueue<>("R->N2", Integer.MAX_VALUE), 1 * ISerializable.M_BIT, 2 * EventQueue.MILLI_SECOND);
        r.addRouting(n1, n1);
        r.addRouting(n2, n2);

        for (int i = 0; i < 2; i++) {
            n1.sendPacket(new RandomPayload(i, n1, n2, 3 * ISerializable.K_BIT), false);
        }

        EventQueue.addEvent(15000, (args) -> {
            n2.disconnect();
        });

        EventQueue.addEvent(20000, (args) -> {
            n2.move(r, new FIFOQueue<>("N2->R", Integer.MAX_VALUE), new FIFOQueue<>("R->N2", Integer.MAX_VALUE), 1 * ISerializable.M_BIT, 2 * EventQueue.MILLI_SECOND);
        });

        EventQueue.run();
    }
}
