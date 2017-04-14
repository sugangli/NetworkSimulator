/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.gaming.traditional;

import edu.rutgers.winlab.simulator.gaming.common.UserEvent;
import edu.rutgers.winlab.simulator.core.EventHandlerQueue;
import edu.rutgers.winlab.simulator.core.EventQueue;
import edu.rutgers.winlab.simulator.core.FIFOQueue;
import edu.rutgers.winlab.simulator.core.ISerializable;
import edu.rutgers.winlab.simulator.core.Serial;
import edu.rutgers.winlab.simulator.core.SimulatorQueue;
import edu.rutgers.winlab.simulator.gaming.common.Frame;
import edu.rutgers.winlab.simulator.gaming.common.Packet;
import java.util.LinkedList;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 *
 * @author wuyang
 */
public class GameClient extends edu.rutgers.winlab.simulator.gaming.common.GameClient {

    public static Supplier<Long> getGameLogicProcessingTime;
    public static Supplier<Long> getRenderProcessingTime;
    public static Supplier<Long> getUpdateProcessingTime;

    public static long getGameLogicProcessingTime_LEC() {
        return 10 * EventQueue.MILLI_SECOND;
    }

    public static long getRenderProcessingTime_LEC() {
        return 200 * EventQueue.MILLI_SECOND;
    }

    public static long getUpdateProcessingTime_LEC() {
        return 8 * EventQueue.MILLI_SECOND;
    }

    public static long getGameLogicProcessingTime_HEC() {
        return 3 * EventQueue.MILLI_SECOND;
    }

    public static long getRenderProcessingTime_HEC() {
        return 10 * EventQueue.MILLI_SECOND;
    }

    public static long getUpdateProcessingTime_HEC() {
        return 2 * EventQueue.MILLI_SECOND;
    }

    public static long getFirstRenderDelayTime() {
        return FRAME_INTERVAL;
    }

    private final LinkedList<UserEvent> _toRenders = new LinkedList<>();
    private final EventHandlerQueue<UserEvent> _userInputQueue;

    public GameClient(String name, SimulatorQueue<ISerializable> innerIncomingQueue,
            String serverName,
            BiConsumer<edu.rutgers.winlab.simulator.gaming.common.GameClient, UserEvent> eventReceivedHandler) {
        super(name, innerIncomingQueue, serverName, eventReceivedHandler);
        new Serial<>(this::_delayBeforeFirstRender, 0);
        _userInputQueue = new EventHandlerQueue<>(new FIFOQueue(name + "_INPUT", Integer.MAX_VALUE), this::_gameLogic);
    }

    @Override
    public void handleUserEvent(UserEvent e) {
        _userInputQueue.enqueue(e, false);
    }

    // wait till the end of 1st frame to render
    private long _delayBeforeFirstRender(Serial<Integer> s, Integer parameter) {
        s.addEvent(this::_renderLogic, parameter);
        return getFirstRenderDelayTime();
    }

    private long lastFrame = -1;
    private long totalFrameDelays = 0;
    private int totalFrames = 0;

    private long _renderLogic(Serial<Integer> s, Integer parameter) {
        if (!isRunning()) {
            return 0;
        }
        long now = EventQueue.now();
        if (lastFrame != -1) {
            totalFrameDelays += now - lastFrame;
            totalFrames++;
        }
        lastFrame = now;
        long processTime = getRenderProcessingTime.get();
        EventQueue.addEvent(now + processTime, (args) -> {
            LinkedList<UserEvent> l = (LinkedList<UserEvent>) args[0];
            l.forEach(e -> _eventReceivedHandler.accept(this, e));
        }, new LinkedList<>(_toRenders));
//        System.out.printf("[%d] CH %s UEs=%s%n", EventQueue.now(), getName(), _toRenders);
        _toRenders.clear();
        s.addEvent(this::_renderLogic, 0);
        return Math.max(processTime, FRAME_INTERVAL);
    }

    @Override
    public double getAvgFrameLatency() {
        return ((double) totalFrameDelays) / totalFrames;
    }

    private long _gameLogic(Serial<UserEvent> s, UserEvent parameter) {
        if (parameter.isGameEvent()) {
            s.addEvent(this::sendEventToServer, parameter);
        } else {
            s.addEvent(this::_forwardEventToRender, parameter);
        }
        return getGameLogicProcessingTime.get();
    }

    // from event handler
    private long _forwardEventToRender(Serial<UserEvent> s, UserEvent parameter) {
        _toRenders.addLast(parameter);
        return 0;
    }

    // from packet processor
    private long _forwardEventsToRenderer(Serial<ISerializable> s, ISerializable param) {
        Packet pkt = (Packet) param;
        Frame f = (Frame) pkt.getPayload();
        f.forEachContainedEvent(ue -> _toRenders.add(ue));
        return 0;
    }

    @Override
    protected long _processPacket(Serial<ISerializable> s, ISerializable param) {
        //event received, send the event to 
//        System.out.printf("[%d] CR %s received %s%n", EventQueue.now(), getName(), param);
        s.addEvent(this::_forwardEventsToRenderer, param);
//        s.addEvent((ss, p) -> {
//            // TODO: get decapsulate evt from network packet p
////            UserEvent evt = (UserEvent) p;
////            _toRenders.addLast(evt);
//            return 0;
//        }, param);
        return getUpdateProcessingTime.get();
    }

}
