/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.core;

import java.util.LinkedList;

/**
 *
 * @author wuyang
 */
public abstract class ReliableEndHost extends EndHost {

    private final LinkedList<ISerializable> pendingPackets = new LinkedList<>();

    public ReliableEndHost(String name, SimulatorQueue<ISerializable> innerIncomingQueue) {
        super(name, innerIncomingQueue);
    }

    @Override
    public void move(Node newFirstHop, SimulatorQueue<ISerializable> thisToFirstHopQueue, SimulatorQueue<ISerializable> firstHopToThisQueue, int bandwidth, long delay) {
        super.move(newFirstHop, thisToFirstHopQueue, firstHopToThisQueue, bandwidth, delay);
        pendingPackets.forEach(p -> {
            sendPacket(p, true);
        });
        pendingPackets.clear();
    }

    @Override
    protected void _handleFailedPacket(ISerializable packet) {
//        System.out.printf("[%d] REH %s PUT into PENDING: %s %n", EventQueue.now(), getName(), packet);

        pendingPackets.add(packet);
    }

}
