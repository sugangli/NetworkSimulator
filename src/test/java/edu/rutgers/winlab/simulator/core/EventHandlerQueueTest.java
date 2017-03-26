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
import org.junit.Test;

/**
 *
 * @author wuyang
 */
public class EventHandlerQueueTest {

    public EventHandlerQueueTest() {
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

        Serial.SerialAction<Integer> a = new Serial.SerialAction<Integer>() {
            @Override
            public long execute(Serial<Integer> s, Integer parameter) {
                System.out.printf("[%d] Event triggered, val=%d%n", EventQueue.now(), parameter);
                if (parameter > 0) {
                    s.addEvent(this, parameter - 1);
                }
                return parameter * EventQueue.MILLI_SECOND;
            }
        };

        EventHandlerQueue<Integer> queue = new EventHandlerQueue<>(new FIFOQueue("Test", Integer.MAX_VALUE), a);
        queue.enqueue(3, false);
        queue.enqueue(2, false);
        queue.enqueue(5, true);
        EventQueue.run();
    }

}
