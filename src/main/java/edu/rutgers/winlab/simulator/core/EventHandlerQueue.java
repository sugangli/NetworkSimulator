package edu.rutgers.winlab.simulator.core;

import java.util.function.Consumer;

public class EventHandlerQueue<T> {
    
    private final SimulatorQueue<T> _innerQueue;
    private final Serial.SerialAction<T> _eventHandler;
    private final Action _handleItemAction;
    private boolean _busy;
    
    public EventHandlerQueue(SimulatorQueue<T> innerQueue, Serial.SerialAction<T> eventHandler) {
        _innerQueue = innerQueue;
        _eventHandler = eventHandler;
        _busy = false;
        _handleItemAction = (args) -> handleItem(args);
    }
    
    private void handleItem(Object... args) {
        if (_innerQueue.getSize() == 0) {
//                System.out.println("InnerQueue.getSize == 0");
            _busy = false;
            return;
        }
//            System.out.println("InnerQueue.getSize != 0");
        T item = _innerQueue.dequeue();
        Serial<T> s = new Serial<>(_eventHandler, item);
        s.addSerialFinishedHandler(_handleItemAction);
    }
    
    public int getSize() {
        return _innerQueue.getSize();
    }
    
    public boolean isBusy() {
        return _busy;
    }
    
    public void enqueue(T item, boolean isPrioritized) {
        _innerQueue.enqueue(item, isPrioritized);
        if (!_busy) {
            _busy = true;
//			System.out.println("Enqueue: HandleItemAction");
            EventQueue.addEvent(EventQueue.now(), _handleItemAction);
        }
    }
    
    public void clear(Consumer<T> c) {
        while (_innerQueue.getSize() > 0) {
            c.accept(_innerQueue.dequeue());
        }
    }
}
