package edu.rutgers.winlab.simulator.core;

@FunctionalInterface
public interface Action {

    public abstract void execute(Object... args);

}
