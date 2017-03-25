/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.core;

import java.util.function.BiConsumer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author wuyang
 */
public class CalculateNodeDijkstraTest {

    public CalculateNodeDijkstraTest() {
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

    private static void myConnectNodes(Node n1, Node n2, long delay, int bw) {
        Node.connectNodes(n1, n2,
                new FIFOQueue<>(String.format("%s->%s", n1.getName(), n2.getName()), Integer.MAX_VALUE),
                new FIFOQueue<>(String.format("%s->%s", n2.getName(), n1.getName()), Integer.MAX_VALUE),
                bw, delay);
    }

    class MyNode extends Node {

        public MyNode(String name) {
            super(name, new FIFOQueue<>(name + "_IN", Integer.MAX_VALUE));
        }

        @Override
        protected long processPacket(Serial<ISerializable> s, ISerializable param) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        protected void handleFailedPacket(ISerializable packet) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

    @Test
    public void test1() {
        MyNode[] ns = new MyNode[7];
        for (int i = 1; i < ns.length; i++) {
            ns[i] = new MyNode(i + "");
        }
        myConnectNodes(ns[1], ns[2], 7, 1);
        myConnectNodes(ns[1], ns[3], 9, 1);
        myConnectNodes(ns[1], ns[6], 14, 1);
        myConnectNodes(ns[2], ns[3], 10, 1);
        myConnectNodes(ns[2], ns[4], 15, 1);
        myConnectNodes(ns[3], ns[4], 11, 2);
        myConnectNodes(ns[3], ns[6], 2, 1);
        myConnectNodes(ns[4], ns[5], 6, 1);
        myConnectNodes(ns[5], ns[6], 9, 1);

        CalculateNodeDijkstra c = new CalculateNodeDijkstra(ns[1], (l) -> l.getDelay() * l.getBandwidthInBps());
        c.forEachDistance((n, d)->{
            System.out.printf("%s: %s - %d%n", n, d.getPrev(), d.getDistance());
        });
    }

}
