/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.algorithms;

import edu.rutgers.winlab.simulator.core.EventQueue;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ubuntu
 */
public class PoisonRandomTest {

    public PoisonRandomTest() {
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
     * Test of getNext method, of class PoisonRandom.
     */
    @Test
    public void testGetNext() {

        PoisonRandom instance = new PoisonRandom(10);
        Random rand = new Random(0);
        try (PrintStream ps = new PrintStream("poisonRandom.txt")) {
            long val = 0;
            int[] ks = new int[50];
            while (val < EventQueue.SECOND * 3600) {
                Arrays.parallelSetAll(ks, i -> instance.getNext());
                for (int k : ks) {
                    val += k * EventQueue.MILLI_SECOND + rand.nextInt((int)EventQueue.MILLI_SECOND);
                    if (val > EventQueue.SECOND * 3600) {
                        break;
                    }
                    ps.println(val);
                }
            }
            ps.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
