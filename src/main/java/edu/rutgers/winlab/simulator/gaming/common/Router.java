/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.gaming.common;

import edu.rutgers.winlab.simulator.core.EventQueue;
import edu.rutgers.winlab.simulator.core.ISerializable;
import edu.rutgers.winlab.simulator.core.Node;
import edu.rutgers.winlab.simulator.core.Serial;
import edu.rutgers.winlab.simulator.core.SimulatorQueue;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author ubuntu
 */
public class Router extends Node {

    private static long _getPacketProcesingTIme() {
        return 50 * EventQueue.MICRO_SECOND;
    }

    private final HashMap<String, HashSet<Node>> routing = new HashMap<>();

    public Router(String name, SimulatorQueue<ISerializable> innerIncomingQueue) {
        super(name, innerIncomingQueue);
    }

    public void addRouting(String dest, Node nextHop) {
        HashSet<Node> nextHops = routing.get(dest);
        if (nextHops == null) {
            routing.put(dest, nextHops = new HashSet<>());
        }
        nextHops.add(nextHop);
    }

    public void removeRouting(String dest, Node nextHop) {
        HashSet<Node> nextHops = routing.get(dest);
        if (nextHops != null) {
            nextHops.remove(nextHop);
        }
    }

    public void clearRouting(String dest) {
        routing.remove(dest);
    }
    
    public void printRouting(PrintStream ps){
        ps.printf("=====Routing on %s=====%n", getName());
        routing.forEach((name, nextHops)->{
            ps.printf("%s:", name);
            nextHops.forEach(nh->{
                ps.printf(" %s", nh.getName());
            });
            ps.println();
        });
    }

    @Override
    protected long _processPacket(Serial<ISerializable> s, ISerializable param) {
//        System.out.printf("[%d] RR %s received %s%n", EventQueue.now(), getName(), param);
        s.addEvent(this::_innerSendPacket, param);
        return _getPacketProcesingTIme();
    }

    private long _innerSendPacket(Serial<ISerializable> s, ISerializable param) {
        Packet pkt = (Packet) param;
        String dst = pkt.getDst();
        HashSet<Node> nextHops = routing.get(dst);
        if (nextHops != null) {
            nextHops.forEach((nextHop) -> {
//                System.out.printf("[%d] RS %s sent %s %s%n", EventQueue.now(), getName(), nextHop.getName(), param);
                _sendPacket(param, nextHop, false);
            });
        } else {
            System.out.printf("[%d] RD %s discard %s%n", EventQueue.now(), getName(), param);
        }
        return 0;
    }

    @Override
    protected void _handleFailedPacket(ISerializable packet) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
