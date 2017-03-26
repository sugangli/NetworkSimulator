/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.common;

import edu.rutgers.winlab.simulator.core.Action;
import edu.rutgers.winlab.simulator.core.EventQueue;
import edu.rutgers.winlab.simulator.core.FIFOQueue;
import edu.rutgers.winlab.simulator.core.ISerializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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

    @Test
    public void test1() {
        EventQueue.reset();
        final HashMap<Integer, LinkedList<Long>> packetDurations = new HashMap<>();

        final GameClient c1 = new edu.rutgers.winlab.simulator.traditionalgaming.GameClient("C1", new FIFOQueue<>("C1_IN", Integer.MAX_VALUE), (UserEvent evt) -> {
            LinkedList<Long> l = packetDurations.get(evt.getId());
            l.addLast(EventQueue.now());
        });
        for (int i = 0; i < 100; i++) {
            EventQueue.addEvent(EventQueue.now() + i * EventQueue.MILLI_SECOND * 10, (Object[] args) -> {
                int v = (Integer) args[0];
                LinkedList<Long> l;
                packetDurations.put(v, l = new LinkedList<>());
                l.addLast(EventQueue.now());
                c1.handleUserEvent(new UserEvent(v, 100 * ISerializable.BYTE, false));
            }, i);
        }
        EventQueue.addEvent(EventQueue.now() + 5 * EventQueue.SECOND, (args) -> ((edu.rutgers.winlab.simulator.traditionalgaming.GameClient) args[0]).stop(), c1);
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
