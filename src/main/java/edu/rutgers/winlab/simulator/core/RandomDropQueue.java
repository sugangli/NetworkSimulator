/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.core;

import edu.rutgers.winlab.simulator.gaming.common.Packet;
import edu.rutgers.winlab.simulator.gaming.common.UserEvent;
import java.util.ArrayList;
import java.util.Random;

/**
 *
 * @author ubuntu
 * @param <T>
 */
public class RandomDropQueue<T> extends SimulatorQueue<T> {

    private static final Random RAND = new Random(0);

    private final int _capacity;
    private final ArrayList<T> _innerQueuePrioritized = new ArrayList<>();
    private final ArrayList<T> _innerQueue = new ArrayList<>();

    public RandomDropQueue(String name, int capacity) {
        super(name);
        _capacity = capacity;
    }

    @Override
    public int getCapacity() {
        return _capacity;
    }

    @Override
    public int getSize() {
        return _innerQueuePrioritized.size() + _innerQueue.size();
    }

    @Override
    public void enqueue(T item, boolean isPrioritized) {
        if (isPrioritized) {
            _innerQueuePrioritized.add(item);
        } else {
            _innerQueue.add(item);
        }
        /*
         * if queue is full ( size > capacity ), we pop element out in a random order from queue
         * but pop non_prioritized first
         */
        int diff = getSize() - _capacity;
        while (diff > 0 && _innerQueue.size() > 0) {
            int id = RAND.nextInt(_innerQueue.size());
            T node = _innerQueue.remove(id);
            if (node instanceof Packet && ((Packet) node).getPayload() instanceof UserEvent) {
                System.out.printf("Drop Packet(%s): %s%n", getName(), node.toString());
            }

//            System.out.printf("Drop Packet: %s%n", node.toString());
            SimulatorQueue.totalDropCount++;

            diff--;
        }

        while (diff > 0) {
            int id = RAND.nextInt(_innerQueuePrioritized.size());
            T node = _innerQueuePrioritized.remove(id);
//            System.out.printf("Drop Packet: %s%n", node.toString());
            SimulatorQueue.totalDropCount++;
            diff--;
        }
    }

    @Override
    public T dequeue() {
        return _innerQueuePrioritized.isEmpty()
                ? _innerQueue.remove(0)
                : _innerQueuePrioritized.remove(0);
    }

}
