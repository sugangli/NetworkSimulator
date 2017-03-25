package edu.rutgers.winlab.simulator.core;

@FunctionalInterface
public interface Action {

    public void execute(Object... args);

}
