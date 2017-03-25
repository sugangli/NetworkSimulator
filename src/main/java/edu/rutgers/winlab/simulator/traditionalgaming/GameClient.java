/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.traditionalgaming;

import edu.rutgers.winlab.simulator.common.UserEvent;
import edu.rutgers.winlab.simulator.core.EventHandlerQueue;
import edu.rutgers.winlab.simulator.core.EventQueue;
import edu.rutgers.winlab.simulator.core.FIFOQueue;
import edu.rutgers.winlab.simulator.core.ISerializable;
import edu.rutgers.winlab.simulator.core.Serial;
import edu.rutgers.winlab.simulator.core.SimulatorQueue;
import java.util.LinkedList;
import java.util.function.Consumer;

/**
 *
 * @author wuyang
 */
public class GameClient extends edu.rutgers.winlab.simulator.common.GameClient {

    private static long getGameLogicProcessingTime() {
        return 10 * EventQueue.MILLI_SECOND;
    }

    private static long getRenderProcessingTime() {
        return 100 * EventQueue.MILLI_SECOND;
    }

    private static long getUpdateProcessingTime() {
        return 20 * EventQueue.MILLI_SECOND;
    }

    private final LinkedList<UserEvent> _toRenders = new LinkedList<>();
    private final Serial _renderThread;
    private final EventHandlerQueue<UserEvent> _userInputQueue;

    public GameClient(String name, SimulatorQueue<ISerializable> innerIncomingQueue, Consumer<UserEvent> eventReceivedHandler) {
        super(name, innerIncomingQueue, eventReceivedHandler);
        _renderThread = new Serial<>((s,p)->{
            s.addEvent(this::render, p);
            return 1 * EventQueue.MICRO_SECOND;
        }, 0);
        _userInputQueue = new EventHandlerQueue<>(new FIFOQueue(name + "_INPUT", Integer.MAX_VALUE), this::gameLogic);
    }

    @Override
    public void handleUserEvent(UserEvent e) {
        _userInputQueue.enqueue(e, false);
    }

    private long render(Serial<Integer> s, Integer parameter) {
        if (!isRunning()) {
            return 0;
        }
        long processTime = getRenderProcessingTime();
        EventQueue.addEvent(EventQueue.now() + processTime, (args) -> {
            LinkedList<UserEvent> l = (LinkedList<UserEvent>) args[0];
            l.forEach(_eventReceivedHandler);
        }, new LinkedList<>(_toRenders));
        _toRenders.clear();
        s.addEvent(this::render, 0);
        return Math.max(getRenderProcessingTime(), FRAME_INTERVAL);
    }

    private long gameLogic(Serial<UserEvent> s, UserEvent parameter) {
        if (parameter.isGameEvent()) {
            s.addEvent((ss, p) -> {
//                 encapsulate p into a network packet
//                 sendPacket(p, false);
                return 0;
            }, parameter);
        } else {
            s.addEvent((ss, p) -> {
                _toRenders.addLast(p);
                return 0;
            }, parameter);
        }
        return getGameLogicProcessingTime();
    }

    @Override
    protected long _processPacket(Serial<ISerializable> s, ISerializable param) {
        //event received, send the event to 
        s.addEvent((ss, p) -> {
            // TODO: get decapsulate evt from network packet p
//            UserEvent evt = (UserEvent) p;
//            _toRenders.addLast(evt);
            return 0;
        }, param);
        return getUpdateProcessingTime();
    }

}
