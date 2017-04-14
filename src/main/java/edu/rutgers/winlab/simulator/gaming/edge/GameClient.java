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
import java.util.function.BiConsumer;

/**
 *
 * @author ubuntu
 */
public class GameClient extends edu.rutgers.winlab.simulator.gaming.common.GameClient {
    
    private static long getGameLogicProcessingTime() {
        return 10 * EventQueue.MILLI_SECOND;
    }
    
    private static long getDecoderProcessingTime() {
        return 10 * EventQueue.MILLI_SECOND;
    }
    
    private final EventHandlerQueue<UserEvent> _userInputQueue;
    
    public GameClient(String name, SimulatorQueue<ISerializable> innerIncomingQueue,
            String serverName, BiConsumer<edu.rutgers.winlab.simulator.gaming.common.GameClient, UserEvent> eventReceivedHandler) {
        super(name, innerIncomingQueue, serverName, eventReceivedHandler);
        _userInputQueue = new EventHandlerQueue<>(new FIFOQueue(name + "_INPUT", Integer.MAX_VALUE), this::_gameLogic);
    }
    
    private long _gameLogic(Serial<UserEvent> s, UserEvent parameter) {
        // if you send user event, it is the double size of the original event
        UserEvent ue = new UserEvent(parameter.getSender(), parameter.getId(), parameter.getSize() * 2, parameter.isGameEvent());
        s.addEvent(this::sendEventToServer, ue);
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
        Frame frame = (Frame) ((Packet) param).getPayload();
        
        EventQueue.addEvent(EventQueue.now() + processTime, this::_showFrame, frame);
        return getDecoderProcessingTime();
    }
    
    private long lastFrame = -1;
    private long totalFrameDelays = 0;
    private int totalFrames = 0;
    
    private void _showFrame(Object... args) {
        long now = EventQueue.now();
        if (lastFrame != -1) {
            totalFrameDelays += now - lastFrame;
            totalFrames++;
        }
        lastFrame = now;
        Frame f = (Frame) args[0];
        f.forEachContainedEvent(e -> _eventReceivedHandler.accept(this, e));
    }
    
    @Override
    public double getAvgFrameLatency() {
        return ((double) totalFrameDelays) / totalFrames;
    }
}
