/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.gaming.common;

import edu.rutgers.winlab.simulator.core.ISerializable;

/**
 *
 * @author ubuntu
 */
public class Packet implements ISerializable {

    public static final int DEFAULT_HEADER_SIZE = 200;

    private final String _src, _dst;
    private final ISerializable _payload;

    public Packet(String src, String dst, ISerializable payload) {
        this._src = src;
        this._dst = dst;
        this._payload = payload;
    }

    public String getSrc() {
        return _src;
    }

    public String getDst() {
        return _dst;
    }

    public ISerializable getPayload() {
        return _payload;
    }

    @Override
    public int getSize() {
        return _payload.getSize() + DEFAULT_HEADER_SIZE;
    }

    @Override
    public String toString() {
        return String.format("Pkt{src:%s,dst:%s,p:%s}", _src, _dst, _payload);
    }

}
