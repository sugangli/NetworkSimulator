/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.common;

import edu.rutgers.winlab.simulator.core.ISerializable;
import java.util.LinkedList;
import java.util.function.Consumer;

/**
 *
 * @author wuyang
 */
public class Frame implements ISerializable {

    private final int _size;
    private final LinkedList<UserEvent> _containedEvents = new LinkedList<>();

    public Frame(int size, LinkedList<UserEvent> events) {
        this._size = size;
        _containedEvents.addAll(events);
    }
    
    public void forEachContainedEvent(Consumer<UserEvent> e) {
        _containedEvents.forEach(e);
    }

    @Override
    public int getSize() {
        return _size;
    }

    public LinkedList<UserEvent> getContainedEvents() {
        return _containedEvents;
    }

}
