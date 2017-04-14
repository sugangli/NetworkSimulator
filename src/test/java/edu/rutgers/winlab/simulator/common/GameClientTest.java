/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.common;

import edu.rutgers.winlab.simulator.gaming.common.UserEvent;
import edu.rutgers.winlab.simulator.gaming.common.GameClient;
import edu.rutgers.winlab.simulator.core.EventQueue;
import edu.rutgers.winlab.simulator.core.FIFOQueue;
import edu.rutgers.winlab.simulator.core.ISerializable;
import edu.rutgers.winlab.simulator.core.SimulatorQueue;
import edu.rutgers.winlab.simulator.gaming.common.GameServer;
import edu.rutgers.winlab.simulator.gaming.common.Router;
import edu.rutgers.winlab.simulator.gaming.edge.GameEdge;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.BiConsumer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author wuyang
 */
public class GameClientTest {

    public GameClientTest() {
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

    private SimulatorQueue<ISerializable> getDefaultQueue(String name) {
        return new FIFOQueue<>(name, Integer.MAX_VALUE);
    }

    private enum GameType {
        Traditional, VideoStreaming, Edge
    }

    @Test
    public void test1() {
        EventQueue.reset();
        final HashMap<Integer, LinkedList<Long>> packetDurations = new HashMap<>();
        GameType type = GameType.Edge;

        Router r1 = new Router("R1", getDefaultQueue("R1_IN"));
        GameClient c1, c2;
        GameServer s;
        GameEdge e1 = null, e2 = null;

        BiConsumer<GameClient, UserEvent> handler = (c, evt) -> {
            LinkedList<Long> l = packetDurations.get(evt.getId());
            l.addLast(EventQueue.now());
        };

        switch (type) {
            case Traditional:
                c1 = new edu.rutgers.winlab.simulator.gaming.traditional.GameClient("C1",
                        getDefaultQueue("C1_IN"), "G1", handler);
                c2 = new edu.rutgers.winlab.simulator.gaming.traditional.GameClient("C2",
                        getDefaultQueue("C2_IN"), "G1", handler);
                s = new edu.rutgers.winlab.simulator.gaming.traditional.GameServer("S", getDefaultQueue("S_IN"));
                break;
            case VideoStreaming:
                c1 = new edu.rutgers.winlab.simulator.gaming.videostream.GameClient("C1",
                        getDefaultQueue("C1_IN"), "G1", handler);
                c2 = new edu.rutgers.winlab.simulator.gaming.videostream.GameClient("C2",
                        getDefaultQueue("C2_IN"), "G1", handler);
                s = new edu.rutgers.winlab.simulator.gaming.videostream.GameServer("S", getDefaultQueue("S_IN"));
                break;
            default: // edge
                c1 = new edu.rutgers.winlab.simulator.gaming.edge.GameClient("C1",
                        getDefaultQueue("C1_IN"), "C1_E", handler);
                c2 = new edu.rutgers.winlab.simulator.gaming.edge.GameClient("C2",
                        getDefaultQueue("C2_IN"), "C2_E", handler);
                s = new edu.rutgers.winlab.simulator.gaming.edge.GameServer("S", getDefaultQueue("S_IN"));
                e1 = new GameEdge("E1", getDefaultQueue("E1_IN"), "G1", "C1");
                e1.move(r1, getDefaultQueue("S->E1"), getDefaultQueue("E1->S"), 100 * ISerializable.M_BIT, 2 * EventQueue.MILLI_SECOND);
                e2 = new GameEdge("E2", getDefaultQueue("E2_IN"), "G1", "C2");
                e2.move(r1, getDefaultQueue("S->E2"), getDefaultQueue("E2->S"), 100 * ISerializable.M_BIT, 2 * EventQueue.MILLI_SECOND);
                break;
        }

        c1.move(r1, getDefaultQueue("C1->R1"), getDefaultQueue("R1->C1"), 100 * ISerializable.M_BIT, 2 * EventQueue.MILLI_SECOND);
        c2.move(r1, getDefaultQueue("C2->R1"), getDefaultQueue("R1->C2"), 100 * ISerializable.M_BIT, 30 * EventQueue.MILLI_SECOND);

        s.move(r1, getDefaultQueue("S->R1"), getDefaultQueue("R1->S"), 10 * ISerializable.M_BIT, 2 * EventQueue.MILLI_SECOND);

        // useful only for videostream gaming
        s.addGameClient("G1", "C1");
        s.addGameClient("G1", "C2");

        r1.addRouting("G1", s);
        switch (type) {
            case Traditional:
                // for traditional gaming, using multicast
                r1.addRouting("G1_DOWN", c1);
                r1.addRouting("G1_DOWN", c2);
                break;
            case VideoStreaming:
                // for videostream gaming, using unicast
                r1.addRouting("C1", c1);
                r1.addRouting("C2", c2);
                break;
            default: // Edge
                r1.addRouting("C1", c1);
                r1.addRouting("C2", c2);
                r1.addRouting("C1_E", e1);
                r1.addRouting("C2_E", e2);
                r1.addRouting("G1_DOWN", e1);
                r1.addRouting("G1_DOWN", e2);
                break;
        }

        for (int i = 0; i < 100; i++) {
            EventQueue.addEvent(EventQueue.now() + i * EventQueue.MILLI_SECOND * 10, (Object[] args) -> {
                int v = (Integer) args[0];
                LinkedList<Long> l;
                packetDurations.put(v, l = new LinkedList<>());
                l.addLast(EventQueue.now());
                c1.handleUserEvent(new UserEvent(c1.getName(), v, 100 * ISerializable.BYTE, (v - 9) % 10 != 0));
            }, i);
        }
        EventQueue.addEvent(EventQueue.now() + 1200 * EventQueue.MILLI_SECOND, (args) -> ((GameClient) args[0]).stop(), c1);
        EventQueue.addEvent(EventQueue.now() + 1200 * EventQueue.MILLI_SECOND, (args) -> ((GameClient) args[0]).stop(), c2);
        EventQueue.addEvent(EventQueue.now() + 1200 * EventQueue.MILLI_SECOND, (args) -> ((GameServer) args[0]).stop(), s);
        if (type == GameType.Edge) {
            EventQueue.addEvent(EventQueue.now() + 1200 * EventQueue.MILLI_SECOND, (args) -> ((GameEdge) args[0]).stop(), e1);
            EventQueue.addEvent(EventQueue.now() + 1200 * EventQueue.MILLI_SECOND, (args) -> ((GameEdge) args[0]).stop(), e2);
        }
        EventQueue.run();

        packetDurations.forEach((Integer k, LinkedList<Long> v) -> {
            System.out.printf("[ID=%d] duration:", k);
            long start = v.removeFirst();
            while (!v.isEmpty()) {
                long next = v.removeFirst();
                System.out.printf(" %d", next - start);
            }
            System.out.println();
        });

    }

}
