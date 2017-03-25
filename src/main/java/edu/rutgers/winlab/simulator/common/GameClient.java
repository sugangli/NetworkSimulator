/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.common;

import edu.rutgers.winlab.simulator.core.EventQueue;
import edu.rutgers.winlab.simulator.core.ISerializable;
import edu.rutgers.winlab.simulator.core.ReliableEndHost;
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

    public GameClient(String name, SimulatorQueue<ISerializable> innerIncomingQueue, Consumer<UserEvent> eventReceivedHandler) {
        super(name, innerIncomingQueue);
        _eventReceivedHandler = eventReceivedHandler;
    }
    
    

    public abstract void handleUserEvent(UserEvent e);
    public void stop() {
        _running = false;
    }

    public boolean isRunning() {
        return _running;
    }
    

}
