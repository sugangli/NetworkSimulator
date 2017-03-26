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
public class SerialTest {

    public SerialTest() {
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
     * Test of AddEvent method, of class Serial.
     */
    @Test
    public void test1() {
        EventQueue.reset();

        Serial.SerialAction a1 = (ss, param) -> {
            System.out.printf("[%d] A1: %d%n", EventQueue.now(), param);
            return 1 * EventQueue.SECOND;
        };
        Serial<Integer> s = new Serial<>(a1, 3);
        s.addEvent(new Serial.SerialAction<Integer>() {

            @Override
            public long execute(Serial<Integer> ss, Integer parameter) {
                int count = parameter;
                System.out.printf("[%d] A2: %d%n", EventQueue.now(), count);
                if (count > 0) {
                    ss.addEvent(this, count - 1);
                }
                if (count > 0) {
                    ss.addEvent(this, count - 2);
                }
                return 2 * EventQueue.SECOND;
            }
        }, 3);
        s.addLastEvent(new Serial.SerialAction<Integer>() {

            @Override
            public long execute(Serial<Integer> ss, Integer parameter) {
                int count = parameter;
                System.out.printf("[%d] A3: %d%n", EventQueue.now(), parameter);
                if (count > 0) {
                    ss.addLastEvent(this, count - 1);
                }
                if (count > 0) {
                    ss.addEvent(a1, count - 1);
                }
                return 3 * EventQueue.SECOND;
            }
        }, 3);
        EventQueue.run();
    }

}
