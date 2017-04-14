package edu.rutgers.winlab.simulator.core;

import edu.rutgers.winlab.simulator.gaming.common.Packet;
import edu.rutgers.winlab.simulator.gaming.common.UserEvent;
import java.util.LinkedList;

// TODO: should add handler when queue drops packets
public class FIFOQueue<T> extends SimulatorQueue<T> {

    private final int _capacity;

    private final LinkedList<T> _innerQueuePrioritized = new LinkedList<>();
    private final LinkedList<T> _innerQueue = new LinkedList<>();

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
        return _innerQueuePrioritized.size() + _innerQueue.size();
    }

    @Override
    public void enqueue(T item, boolean prioritized) {

        if (prioritized) {
            _innerQueuePrioritized.addLast(item);
        } else {
            _innerQueue.addLast(item);
        }
        
        /*
         * if queue is full ( size > capacity ), we pop element out from queue
        */
        int diff = getSize() - _capacity;
        while (diff > 0 && _innerQueue.size() > 0) {
            T node = _innerQueue.getLast();
            if (node instanceof Packet && ((Packet) node).getPayload() instanceof UserEvent) {
                System.out.printf("Drop Packet(%s): %s%n", getName(), node.toString());
            }
//            System.out.printf("Drop Packet: %s%n", node.toString());
            _innerQueue.removeLast();
            diff--;
            totalDropCount++;
        }

        while (diff > 0) {
            T node = _innerQueuePrioritized.getLast();
//            System.out.printf("Drop Packet: %s%n", node.toString());
            _innerQueuePrioritized.removeLast();
            diff--;
            totalDropCount++;
        }
    }

    @Override
    public T dequeue() {
        return _innerQueuePrioritized.isEmpty()
                ? _innerQueue.removeFirst()
                : _innerQueuePrioritized.removeFirst();
    }

}
