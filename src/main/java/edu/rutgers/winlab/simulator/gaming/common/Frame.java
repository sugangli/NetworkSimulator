/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.gaming.common;

import edu.rutgers.winlab.simulator.core.ISerializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Consumer;

/**
 *
 * @author wuyang
 */
public class Frame implements ISerializable {

    private final int _size;
    private final LinkedList<UserEvent> _containedEvents;

    public Frame(int size, Collection<UserEvent> events) {
        this._size = size;
        _containedEvents = new LinkedList<>(events);
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

    @Override
    public String toString() {
        return String.format("Frame{size:%d,UEs:%s}", _size, _containedEvents);
    }

}
