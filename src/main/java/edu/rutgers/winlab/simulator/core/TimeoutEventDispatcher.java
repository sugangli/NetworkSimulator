package edu.rutgers.winlab.simulator.core;

public class TimeoutEventDispatcher {

    private long _timeoutTime;
    private boolean _active;
    private final Object[] _args;
    private final Action _action;
    
    private final Action _timeoutHandle = new Action() {
        @Override
        public void execute(Object... args) {
            if (!_active) {
                System.out.printf("[%d] Cancelled!%n", EventQueue.now());
                return;
            }
            if (_timeoutTime == EventQueue.now()) {
                System.out.printf("[%d] It's Now ! timeoutime :%d%n", EventQueue.now(), _timeoutTime);
                _action.execute(_args);
                _active = false;
            } else {
                System.out.printf("[%d] Set timeoutime : %d%n", EventQueue.now(), _timeoutTime);
                EventQueue.addEvent(_timeoutTime, this);
            }
        }
    };

    public TimeoutEventDispatcher(long timeouttime, Action action, Object... args) {
        this._timeoutTime = timeouttime;
        this._args = args;
        this._active = true;
        this._action = action;
        EventQueue.addEvent(timeouttime, _timeoutHandle);
    }

    public long getTimeoutTime() {
        return _timeoutTime;
    }

    public boolean isActive() {
        return _active;
    }

    public void delay(long newTime) {
        if (newTime <= this._timeoutTime)
            throw new IllegalArgumentException(String.format("NewTime(%d) <= TimeoutTime(%d)", newTime, _timeoutTime));
        this._timeoutTime = newTime;
    }

    public void cancel() {
        this._active = false;
    }


}
