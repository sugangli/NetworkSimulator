package edu.rutgers.winlab.simulator.core;

import java.util.LinkedList;

public class FIFOQueue<T> extends SimulatorQueue<T> {

    private final int _capacity;

    private final LinkedList<T> innerQueuePrioritized = new LinkedList<>();
    private final LinkedList<T> innerQueue = new LinkedList<>();

    public FIFOQueue(String name, int capacity) {
        super(name);
        _capacity = capacity;
    }

    @Override
    public int getCapacity() {
        return _capacity;
    }

    @Override
    public int getSize() {
        return innerQueuePrioritized.size() + innerQueue.size();
    }

    @Override
    public void enqueue(T item, boolean prioritized) {

        if (prioritized) {
            innerQueuePrioritized.addLast(item);
        } else {
            innerQueue.addLast(item);
        }
        int diff = getSize() - _capacity;
        while (diff > 0 && innerQueue.size() > 0) {
            T node = innerQueue.getLast();
            System.out.printf("Drop Packet: %s", node.toString());
            innerQueue.removeLast();
            diff--;
        }

        while (diff > 0) {
            T node = innerQueuePrioritized.getLast();
            System.out.printf("Drop Packet: %s", node.toString());
            innerQueuePrioritized.removeLast();
            diff--;
        }
    }

    @Override
    public T dequeue() {
        return innerQueuePrioritized.isEmpty()
                ? innerQueue.removeFirst()
                : innerQueuePrioritized.removeFirst();
    }

}
