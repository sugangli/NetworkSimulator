/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.core;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 *
 * @author wuyang
 */
public class EventQueueTest {

    public EventQueueTest() {
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

    public static Action Test1 = new Action() {

        @Override
        public void execute(Object... args) {
            Integer val = (Integer) args[0];
            System.out.printf("[Test1] Now: %d val: %d\n", EventQueue.now(), val);
            if (val > 0) {
                EventQueue.addEvent(EventQueue.now(), Test1, val - 1);
            }
        }

    };

    public static Action Test2 = new Action() {

        @Override
        public void execute(Object... args) {
            Integer val = (Integer) args[0];
            System.out.printf("[Test2] Now: %d val: %d\n", EventQueue.now(), val);
            if (val > 0) {
                EventQueue.addEvent(EventQueue.now() + EventQueue.MILLI_SECOND, Test2, val - 1);
            } else {
                EventQueue.addEvent(EventQueue.now() + EventQueue.MILLI_SECOND, Test1, val + 5);
            }
        }

    };

    /**
     * Test of AddEvent method, of class EventQueue.
     */
    @org.junit.Test
    public void test1() {
        EventQueue.reset();

        EventQueue.addEvent(EventQueue.now() + EventQueue.MILLI_SECOND, Test1, 5);
        EventQueue.addEvent(EventQueue.now() + EventQueue.MILLI_SECOND, Test2, 5);
        EventQueue.run();
    }
}
