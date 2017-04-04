package edu.rutgers.winlab.simulator.core;

// TODO: should add handler when queue drops packets
public abstract class SimulatorQueue<T> {

    private final String _name;

    public String getName() {
        return _name;
    }

    public SimulatorQueue(String name) {
        this._name = name;
    }

    public abstract int getCapacity();

    public abstract int getSize();

    public abstract void enqueue(T item, boolean isPrioritized);

    public abstract T dequeue();
}
