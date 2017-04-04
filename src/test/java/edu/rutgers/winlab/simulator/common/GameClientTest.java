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
import java.util.HashMap;
import java.util.LinkedList;
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

    @Test
    public void test1() {
        EventQueue.reset();

        Router r1 = new Router("R1", getDefaultQueue("R1_IN"));

        final HashMap<Integer, LinkedList<Long>> packetDurations = new HashMap<>();

        final GameClient c1 = new edu.rutgers.winlab.simulator.gaming.traditional.GameClient("C1",
                getDefaultQueue("C1_IN"), "G1",
                (UserEvent evt) -> {
                    LinkedList<Long> l = packetDurations.get(evt.getId());
                    l.addLast(EventQueue.now());
                });
        c1.move(r1, getDefaultQueue("C1->R1"), getDefaultQueue("R1->C1"), 10 * ISerializable.M_BIT, 2 * EventQueue.MILLI_SECOND);

        final GameClient c2 = new edu.rutgers.winlab.simulator.gaming.traditional.GameClient("C2",
                getDefaultQueue("C2_IN"), "G1",
                (UserEvent evt) -> {
                    LinkedList<Long> l = packetDurations.get(evt.getId());
                    l.addLast(EventQueue.now());
                });
        c2.move(r1, getDefaultQueue("C2->R1"), getDefaultQueue("R1->C2"), 10 * ISerializable.M_BIT, 30 * EventQueue.MILLI_SECOND);

        
        final GameServer s = new edu.rutgers.winlab.simulator.gaming.traditional.GameServer("S", getDefaultQueue("S_IN"));
        s.move(r1, getDefaultQueue("S->R1"), getDefaultQueue("R1->S"), 10 * ISerializable.M_BIT, 2 * EventQueue.MILLI_SECOND);
        
        r1.addRouting("G1", s);
        r1.addRouting("G1_DOWN", c1);
        r1.addRouting("G1_DOWN", c2);

        for (int i = 0; i < 100; i++) {
            EventQueue.addEvent(EventQueue.now() + i * EventQueue.MILLI_SECOND * 10, (Object[] args) -> {
                int v = (Integer) args[0];
                LinkedList<Long> l;
                packetDurations.put(v, l = new LinkedList<>());
                l.addLast(EventQueue.now());
                c1.handleUserEvent(new UserEvent(v, 100 * ISerializable.BYTE, (v - 9) % 10 != 0));
            }, i);
        }
        EventQueue.addEvent(EventQueue.now() + 1200 * EventQueue.MILLI_SECOND, (args) -> ((GameClient) args[0]).stop(), c1);
        EventQueue.addEvent(EventQueue.now() + 1200 * EventQueue.MILLI_SECOND, (args) -> ((GameClient) args[0]).stop(), c2);
        EventQueue.addEvent(EventQueue.now() + 1200 * EventQueue.MILLI_SECOND, (args) -> ((GameServer) args[0]).stop(), s);
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
