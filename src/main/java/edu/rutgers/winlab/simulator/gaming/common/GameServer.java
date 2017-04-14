/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.gaming.common;

import edu.rutgers.winlab.simulator.core.EndHost;
import edu.rutgers.winlab.simulator.core.ISerializable;
import edu.rutgers.winlab.simulator.core.SimulatorQueue;

/**
 *
 * @author ubuntu
 */
public abstract class GameServer extends EndHost {

    public static final String GAME_NAME_SUFFIX = "_DOWN";

    private boolean _running;

    public GameServer(String name, SimulatorQueue<ISerializable> innerIncomingQueue) {
        super(name, innerIncomingQueue);
        _running = true;
    }

    public void stop() {
        _running = false;
    }

    public boolean isRunning() {
        return _running;
    }

    public abstract void addGameClient(String game, String client);
}
