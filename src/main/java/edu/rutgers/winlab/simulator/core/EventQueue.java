package edu.rutgers.winlab.simulator.core;

import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicLong;

public class EventQueue {

    public final static long MICRO_SECOND = 1;
    public final static long MILLI_SECOND = MICRO_SECOND * 1000;
    public final static long SECOND = MILLI_SECOND * 1000;

    private static final EventQueue DEFAULT = new EventQueue();

    public static void addEvent(long time, Action action, Object... args) {
        DEFAULT._addEvent(time, action, args);
    }

    public static void run() {
        DEFAULT._run();
    }

    public static void reset() {
        DEFAULT._reset();
    }

    public static long now() {
        return DEFAULT._getNow();
    }

    PriorityQueue<Event> _events = new PriorityQueue<>();
    private long _now;
    private final AtomicLong _serial = new AtomicLong();

    private EventQueue() {
    }

    public long _getNow() {
        return this._now;
    }

    private void _run() {
        while (!_events.isEmpty()) {
            Event e = _events.poll();
//            System.out.printf("run() now:%f%s\n", e.Time, e.action);
            _now = e.Time;
            e.doEvent();
        }

    }

    private void _addEvent(long time, Action action, Object... args) {
        if (time < _now) {
            throw new IllegalArgumentException(String.format("Time(%d) < Now", time, _now));
        }
        Event e = new Event(_serial.getAndIncrement(), time, action, args);
        _events.add(e);
    }

    private void _reset() {
        if (!_events.isEmpty()) {
            throw new IllegalArgumentException("Cannot reset event queue when the queue is not empty");
        }
        _now = 0;
    }

    private static class Event implements Comparable<Event> {

        public Long Time;
        public Long Serial;
        public Action Action;
        public Object[] Args;

        public void doEvent() {
            Action.execute(Args);
        }

        public Event(long serial, long time, Action action, Object... args) {
            Serial = serial;
            this.Time = time;
            this.Action = action;
            this.Args = args;
        }

        @Override
        public int compareTo(Event o) {
            int ret = this.Time.compareTo(o.Time);
            return ret == 0 ? (this.Serial.compareTo(o.Serial)) : ret;
        }

    }

}
