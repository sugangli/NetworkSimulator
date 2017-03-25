package edu.rutgers.winlab.simulator.core;

import java.util.ArrayList;
import java.util.List;

public final class Serial<T> {

    @FunctionalInterface
    public interface SerialAction<T> {

        public long execute(Serial<T> s, T parameter);
    }

    private static class SerialEventDataStructure<T> {

        public SerialAction<T> e;
        public T parameter;

        public SerialEventDataStructure(SerialAction<T> e, T parameter) {
            this.e = e;
            this.parameter = parameter;
        }

        public SerialAction<T> getE() {
            return e;
        }

        public T getParameter() {
            return parameter;
        }
    }

    private final List<SerialEventDataStructure<T>> _events = new ArrayList<>();
    private final List<SerialEventDataStructure<T>> _lastEvents = new ArrayList<>();
    private final List<Action> _serialFinishedHandlers = new ArrayList<>();

    public Serial(SerialAction<T> firstEvent, T parameter) {
        addEvent(firstEvent, parameter);
        _scheduleNextEvent(EventQueue.now());
    }

    public void addEvent(SerialAction<T> e, T parameter) {
        _events.add(new SerialEventDataStructure<>(e, parameter));
    }

    public void addLastEvent(SerialAction<T> e, T parameter) {
        _lastEvents.add(new SerialEventDataStructure<>(e, parameter));
    }

    public void addSerialFinishedHandler(Action a) {
        _serialFinishedHandlers.add(a);
    }

    public void removeSerialFinishedHandler(Action a) {
        _serialFinishedHandlers.remove(a);
    }

    private void _fireSerialFinished() {
        _serialFinishedHandlers.forEach((a) -> {
            a.execute(this);
        });
    }

    private void _scheduleNextEvent(long time) {
        EventQueue.addEvent(time, this::_runEvent);
    }

    private void _runEvent(Object... args) {
        if (_events.size() > 0) {

            SerialEventDataStructure<T> seds = _events.get(0);
            long execute_time = seds.getE().execute(this, seds.parameter);
            long nextEventTime = EventQueue.now() + execute_time;
            _events.remove(0);
//	    		System.out.printf("RunEventAction.execute: now:%f %f %s%n", EventQueue.Now(), execute_time, seds.getParameter());
            _scheduleNextEvent(nextEventTime);

        } else {

            if (_lastEvents.size() > 0) {

                SerialEventDataStructure<T> seds = _lastEvents.get(0);
                long execute_time = seds.getE().execute(this, seds.parameter);
                long nextEventTime = EventQueue.now() + execute_time;
                _lastEvents.remove(0);
//	    			System.out.printf("RunEventAction.execute: now:%f %f %s%n", EventQueue.Now(), execute_time, seds.getParameter());
                _scheduleNextEvent(nextEventTime);

            } else {
                _fireSerialFinished();
            }

        }
    }
}
