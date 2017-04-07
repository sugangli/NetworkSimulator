/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.gaming.edge;

import edu.rutgers.winlab.simulator.core.EventHandlerQueue;
import edu.rutgers.winlab.simulator.core.EventQueue;
import edu.rutgers.winlab.simulator.core.FIFOQueue;
import edu.rutgers.winlab.simulator.core.ISerializable;
import edu.rutgers.winlab.simulator.core.Serial;
import edu.rutgers.winlab.simulator.core.SimulatorQueue;
import edu.rutgers.winlab.simulator.gaming.common.Frame;
import edu.rutgers.winlab.simulator.gaming.common.Packet;
import edu.rutgers.winlab.simulator.gaming.common.UserEvent;
import java.util.function.Consumer;

/**
 *
 * @author ubuntu
 */
public class GameClient extends edu.rutgers.winlab.simulator.gaming.common.GameClient{
    private static long getGameLogicProcessingTime() {
        return 10 * EventQueue.MILLI_SECOND;
    }

    private static long getDecoderProcessingTime() {
        return 10 * EventQueue.MILLI_SECOND;
    }

    private final EventHandlerQueue<UserEvent> _userInputQueue;

    public GameClient(String name, SimulatorQueue<ISerializable> innerIncomingQueue,
            String serverName, Consumer<UserEvent> eventReceivedHandler) {
        super(name, innerIncomingQueue, serverName, eventReceivedHandler);
        _userInputQueue = new EventHandlerQueue<>(new FIFOQueue(name + "_INPUT", Integer.MAX_VALUE), this::_gameLogic);
    }

    private long _gameLogic(Serial<UserEvent> s, UserEvent parameter) {
        s.addEvent(this::sendEventToServer, parameter);
        return getGameLogicProcessingTime();
    }

    @Override
    public void handleUserEvent(UserEvent e) {
        _userInputQueue.enqueue(e, false);
    }

    @Override
    protected long _processPacket(Serial<ISerializable> s, ISerializable param) {
        System.out.printf("[%d] CR %s received %s%n", EventQueue.now(), getName(), param);
        long processTime = getDecoderProcessingTime();
        Frame frame = (Frame)((Packet)param).getPayload();
        
        EventQueue.addEvent(EventQueue.now() + processTime, this::_showFrame, frame);
        return getDecoderProcessingTime();
    }
    
    private void _showFrame(Object... args) {
        Frame f = (Frame)args[0];
        f.forEachContainedEvent(_eventReceivedHandler);
    }
}
