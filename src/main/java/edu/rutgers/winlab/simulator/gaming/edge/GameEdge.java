/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.gaming.edge;

import edu.rutgers.winlab.simulator.core.EndHost;
import edu.rutgers.winlab.simulator.core.EventQueue;
import edu.rutgers.winlab.simulator.core.ISerializable;
import edu.rutgers.winlab.simulator.core.Serial;
import edu.rutgers.winlab.simulator.core.SimulatorQueue;
import edu.rutgers.winlab.simulator.gaming.common.Frame;
import static edu.rutgers.winlab.simulator.gaming.common.GameClient.FRAME_INTERVAL;
import edu.rutgers.winlab.simulator.gaming.common.Packet;
import edu.rutgers.winlab.simulator.gaming.common.UserEvent;
import java.util.LinkedList;

/**
 *
 * @author ubuntu
 */
public class GameEdge extends EndHost {

    private static long getGameEventProcessingTime() {
        return 10 * EventQueue.MILLI_SECOND;
    }

    private static long getRenderAndEncodingProcessingTime() {
        return 30 * EventQueue.MILLI_SECOND;
    }

    private static long getFirstRenderDelayTime() {
        return FRAME_INTERVAL;
    }

    private static int getFrameSize(Iterable<UserEvent> pendingEvents) {
        // 100Mb/s / 60f/s
        return 100 * ISerializable.M_BIT / 60;
    }

    private final String _gameName, _client;
    private boolean _running;
    private final LinkedList<UserEvent> _toRenders = new LinkedList<>();

    public boolean isRunning() {
        return _running;
    }

    public GameEdge(String name, SimulatorQueue<ISerializable> innerIncomingQueue, String gameName, String client) {
        super(name, innerIncomingQueue);
        _gameName = gameName;
        _client = client;
        _running = true;
        new Serial<Integer>(this::_delayBeforeFirstRender, 0);
    }

    public void stop() {
        _running = false;
    }

    // wait till the end of 1st frame to render
    private long _delayBeforeFirstRender(Serial<Integer> s, Integer parameter) {
        s.addEvent(this::_renderAndEncodeLogic, parameter);
        return getFirstRenderDelayTime();
    }

    private long _renderAndEncodeLogic(Serial<Integer> s, Integer parameter) {
        if (!isRunning()) {
            return 0;
        }
        long processTime = getRenderAndEncodingProcessingTime();

        Frame f = new Frame(getFrameSize(_toRenders), _toRenders);
        Packet pkt = new Packet(getName(), _client, f);
        EventQueue.addEvent(EventQueue.now() + processTime, this::_forwardFrameToClient, pkt);
        System.out.printf("[%d] EH %s UEs=%s%n", EventQueue.now(), getName(), _toRenders);
        _toRenders.clear();
        s.addEvent(this::_renderAndEncodeLogic, 0);
        return Math.max(getRenderAndEncodingProcessingTime(), FRAME_INTERVAL);
    }

    @Override
    protected long _processPacket(Serial<ISerializable> s, ISerializable param) {
        System.out.printf("[%d] ER %s received %s%n", EventQueue.now(), getName(), param);
        Packet pkt = (Packet) param;
        ISerializable pld = pkt.getPayload();
        if (pld instanceof UserEvent) {
            UserEvent ue = (UserEvent) pld;
            if (ue.isGameEvent()) {
                s.addEvent(this::_forwardEventToServer, pld);
            } else {
                s.addEvent(this::_forwardEventToRenderer, pld);
            }
            return getGameEventProcessingTime();
        } else if (pld instanceof Frame) {
            s.addEvent(this::_forwardEventsToRenderer, pld);
        } else {
            throw new IllegalArgumentException(String.format("[%d] %s should not receivej %s", EventQueue.now(), getName(), param));
        }
        return 0;
    }

    private long _forwardEventToServer(Serial<ISerializable> s, ISerializable param) {
        Packet pkt = new Packet(getName(), _gameName, param);
        System.out.printf("[%d] ES %s sent %s%n", EventQueue.now(), getName(), pkt);
        sendPacket(pkt, false);
        return 0;
    }

    private long _forwardEventToRenderer(Serial<ISerializable> s, ISerializable param) {
        _toRenders.add((UserEvent) param);
        return 0;
    }

    // from packet processor
    private long _forwardEventsToRenderer(Serial<ISerializable> s, ISerializable param) {
        Frame f = (Frame) param;
        f.forEachContainedEvent(ue -> _toRenders.add(ue));
        return 0;
    }

    private void _forwardFrameToClient(Object... args) {
        Packet pkt = (Packet)args[0];
        sendPacket(pkt, false);
    }

    @Override
    protected void _handleFailedPacket(ISerializable packet) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
