/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.gaming.common;

import edu.rutgers.winlab.simulator.core.EventQueue;
import edu.rutgers.winlab.simulator.core.ISerializable;
import edu.rutgers.winlab.simulator.core.ReliableEndHost;
import edu.rutgers.winlab.simulator.core.Serial;
import edu.rutgers.winlab.simulator.core.SimulatorQueue;
import java.util.function.Consumer;

/**
 *
 * @author wuyang
 */
public abstract class GameClient extends ReliableEndHost {

    public static final long FRAME_INTERVAL = 1 * EventQueue.SECOND / 30;

    protected Consumer<UserEvent> _eventReceivedHandler;
    private boolean _running = true;
    private final String _serverName;

    public GameClient(String name, SimulatorQueue<ISerializable> innerIncomingQueue, String serverName, Consumer<UserEvent> eventReceivedHandler) {
        super(name, innerIncomingQueue);
        _serverName = serverName;
        _eventReceivedHandler = eventReceivedHandler;
    }

    public String getServerName() {
        return _serverName;
    }

    protected long sendEventToServer(Serial<UserEvent> s, UserEvent parameter) {
        Packet pkt = new Packet(getName(), _serverName, parameter);
        sendPacket(pkt, false);
        System.out.printf("[%d] CS %s send %s%n", EventQueue.now(), getName(), pkt);
        return 0;
    }

    public abstract void handleUserEvent(UserEvent e);

    public void stop() {
        _running = false;
    }

    public boolean isRunning() {
        return _running;
    }

}
