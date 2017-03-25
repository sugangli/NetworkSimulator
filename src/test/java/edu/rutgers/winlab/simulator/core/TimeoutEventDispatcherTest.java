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
public class TimeoutEventDispatcherTest {

    public TimeoutEventDispatcherTest() {
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

    /**
     * Test of getTimeoutTime method, of class TimeoutEventDispatcher.
     */
    @Test
    public void test1() {
        EventQueue.reset();

        TimeoutEventDispatcher ted = new TimeoutEventDispatcher(EventQueue.now() + EventQueue.MILLI_SECOND, (args) -> {
            System.out.printf("[%d] Timeout! Test\n", EventQueue.now());
        });
        ted.delay(EventQueue.now() + 2 * EventQueue.MILLI_SECOND);

        EventQueue.addEvent(EventQueue.now() + 1500 * EventQueue.MICRO_SECOND, (args) -> {
            long newTime = EventQueue.now() + 2 * EventQueue.MILLI_SECOND;
            System.out.printf("[%d] Test2 delay to %d\n", EventQueue.now(), newTime);
            ted.delay(newTime);
        });

        EventQueue.addEvent(EventQueue.now() + 3499 * EventQueue.MICRO_SECOND, (args) -> {
            System.out.printf("[%d] Test3 Cancel it!\n", EventQueue.now());
            ted.cancel();
        });
        EventQueue.run();
    }

}
