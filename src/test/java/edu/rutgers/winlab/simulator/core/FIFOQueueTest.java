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
public class FIFOQueueTest {
    
    public FIFOQueueTest() {
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
     * Test of getCapacity method, of class FIFOQueue.
     */
   
    
    @Test
    public void testPriorityQueueAdd(){
        FIFOQueue<Integer> fifoQueue = new FIFOQueue<Integer>("test", 10);

        for(int i = 0; i < 50; i++){
            if(i % 2 == 0){
                fifoQueue.enqueue(i, true);
            }else{
                fifoQueue.enqueue(i,false);
            }
           
        }
        
       System.out.printf( String.format("size of this queue is %d\n", fifoQueue.getSize()));
        
       while(fifoQueue.getSize() > 0){
            System.out.printf(String.format("element is %d\n", fifoQueue.dequeue()));
       }
       
       
       
    }
    
}
