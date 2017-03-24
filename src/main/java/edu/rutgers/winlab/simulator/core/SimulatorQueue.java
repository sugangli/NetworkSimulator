package edu.rutgers.winlab.simulator.core;

public abstract class SimulatorQueue<T> {

    private final String Name;

    public String getName() {
        return Name;
    }

    public SimulatorQueue(String Name) {
        this.Name = Name;
    }

    public abstract int getCapacity();

    public abstract int getSize();

    public abstract void enqueue(T item, boolean isPrioritized);

    public abstract T dequeue();
}
