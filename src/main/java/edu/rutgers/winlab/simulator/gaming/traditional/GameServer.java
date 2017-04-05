/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.gaming.traditional;

import edu.rutgers.winlab.simulator.core.EventQueue;
import edu.rutgers.winlab.simulator.core.ISerializable;
import edu.rutgers.winlab.simulator.core.Serial;
import edu.rutgers.winlab.simulator.core.SimulatorQueue;
import edu.rutgers.winlab.simulator.gaming.common.Frame;
import edu.rutgers.winlab.simulator.gaming.common.Packet;
import edu.rutgers.winlab.simulator.gaming.common.UserEvent;
import java.util.HashMap;
import java.util.LinkedList;

/**
 *
 * @author ubuntu
 */
public class GameServer extends edu.rutgers.winlab.simulator.gaming.common.GameServer {

    private static long getGameEventProcessingTime() {
        return 1 * EventQueue.MILLI_SECOND;
    }

    private static int getFrameSize(Iterable<UserEvent> pendingEvents) {
        return 75;
    }

    private final HashMap<String, LinkedList<UserEvent>> _pendingUEs = new HashMap<>();

    public GameServer(String name, SimulatorQueue<ISerializable> innerIncomingQueue) {
        super(name, innerIncomingQueue);
    }
   
    //wait a frame before real logic starts
    private long _beforeServerGameLogic(Serial<String> s, String gameName) {
        s.addEvent(this::_serverGameLogic, gameName);
        return EventQueue.SECOND / MAX_REFRESH_FPS;
    }

    private long _serverGameLogic(Serial<String> s, String gameName) {
        if (!isRunning()) {
            return 0;
        }
        LinkedList<UserEvent> pendingUEs = _pendingUEs.get(gameName);
        System.out.printf("[%d] SH %s[%s] UEs=%s%n", EventQueue.now(), getName(), gameName, pendingUEs);
        if (!pendingUEs.isEmpty()) {
            Frame f = new Frame(getFrameSize(pendingUEs), pendingUEs);
            Packet pkt = new Packet(gameName, gameName + GAME_NAME_SUFFIX, f);
            pendingUEs.clear();
            
            EventQueue.addEvent(EventQueue.now() + getGameEventProcessingTime(), 
                    this::_sendFrame, pkt);
        }
        s.addEvent(this::_serverGameLogic, gameName);
        return EventQueue.SECOND / MAX_REFRESH_FPS;
    }
    
    private void _sendFrame(Object... parameters) {
        System.out.printf("[%d] SS %s send %s", EventQueue.now(), getName(), parameters[0]);
        sendPacket((Packet)parameters[0], false);
    }

    @Override
    protected long _processPacket(Serial<ISerializable> s, ISerializable param) {
        System.out.printf("[%d] SR %s received %s%n", EventQueue.now(), getName(), param);
        Packet pkt = (Packet) param;
        UserEvent ue = (UserEvent) pkt.getPayload();
        String dst = pkt.getDst();

        LinkedList<UserEvent> pendingUEs = _pendingUEs.get(dst);
        if (pendingUEs == null) {
            _pendingUEs.put(dst, pendingUEs = new LinkedList<>());
            new Serial<String>(this::_beforeServerGameLogic, dst);
        }
        pendingUEs.add(ue);
        return getGameEventProcessingTime();
    }

    @Override
    protected void _handleFailedPacket(ISerializable packet) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addGameClient(String game, String client) {
        // do not need to do anything, since using multicast
    }


}
