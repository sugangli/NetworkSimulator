/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.core;

/**
 *
 * @author wuyang
 */
public abstract class EndHost extends Node {

    private Node _firstHop = null;

    public EndHost(String name, SimulatorQueue<ISerializable> innerIncomingQueue) {
        super(name, innerIncomingQueue);
    }

    public void move(Node newFirstHop, SimulatorQueue<ISerializable> thisToFirstHopQueue,
            SimulatorQueue<ISerializable> firstHopToThisQueue,
            int bandwidth, long delay) {
        disconnect();
        Node.connectNodes(this, _firstHop = newFirstHop, thisToFirstHopQueue, firstHopToThisQueue, bandwidth, delay);
    }

    public void disconnect() {
        if (_firstHop != null) {
            Node.disconnectNodes(this, _firstHop);
            _firstHop = null;
        }
    }

    public void sendPacket(ISerializable packet, boolean prioritized) {
        super.sendPacket(packet, _firstHop, prioritized);
    }
}
