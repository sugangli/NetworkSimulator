/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.gaming.common;

import edu.rutgers.winlab.simulator.core.ISerializable;

/**
 *
 * @author wuyang
 */
public class UserEvent implements ISerializable {

    private final String _sender;
    private final int _id;
    private final int _size;
    private final boolean _gameEvent;

    public UserEvent(String sender, int id, int size, boolean gameEvent) {
        this._sender = sender;
        this._id = id;
        this._size = size;
        this._gameEvent = gameEvent;
    }

    public String getSender() {
        return _sender;
    }

    public int getId() {
        return _id;
    }

    public boolean isGameEvent() {
        return _gameEvent;
    }

    @Override
    public int getSize() {
        return _size;
    }

    @Override
    public String toString() {
        return String.format("UE{id:%d,size:%d,isGame:%b}", _id, _size, _gameEvent);
    }

}
