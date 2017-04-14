/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.gaming.common;

import edu.rutgers.winlab.simulator.algorithms.DijkstraCalculator;
import edu.rutgers.winlab.simulator.algorithms.ReportObject;
import edu.rutgers.winlab.simulator.core.EventQueue;
import edu.rutgers.winlab.simulator.core.FIFOQueue;
import edu.rutgers.winlab.simulator.core.ISerializable;
import edu.rutgers.winlab.simulator.core.Node;
import edu.rutgers.winlab.simulator.core.RandomDropQueue;
import edu.rutgers.winlab.simulator.core.SimulatorQueue;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ubuntu
 */
public class SimulationRunner {

    public static final String GAME_NAME = "G1";
    public static final String CENTER_ROUTER_NAME = "I_C";
    public static final String GAME_DOWN_NAME = "G1" + GameServer.GAME_NAME_SUFFIX;
    public static final int SERVER_BW_BPS = 1000 * ISerializable.M_BIT;
    public static final long SERVER_LATENCY = 2 * EventQueue.MILLI_SECOND;
    public static final long EDGE_LATENCY = 1 * EventQueue.MILLI_SECOND;
    public static final int EDGE_BW_BPS = 1000 * ISerializable.M_BIT;
    public static final long CLIENT_LATENCY = 3 * EventQueue.MILLI_SECOND;
    public static final int CLIENT_BW_BPS = 50 * ISerializable.M_BIT;
    public static final int RO_KEY_REDUNDANT = 0;
    public static final int RO_KEY_MISS = 1;
    public static final int RO_KEY_FINISHED_USER = 2;
    public static final int RO_KEY_FINISHED_GAME = 3;
    public static String dataFolder = "./SF/vehicle0531_filt2/";

    public static void main(String[] prams) throws IOException {
        if (prams.length < 2) {
            System.out.println("Usage: java ... %type% %time%");
            System.out.println("   type: th (traditional HEC), tl (traditional LEC)");
            return;
        }
        String time = prams[1];
        SimulationRunner runner = new SimulationRunner();
        switch (prams[0]) {
            case "th":
                runner.runTraditionalGame(time, true);
                break;
            case "tl":
                runner.runTraditionalGame(time, false);
                break;
            case "vs":
                runner.runVideoStreamGame(time);
                break;
        }
    }

    public static SimulatorQueue<ISerializable> getInfiniteQueue(String name) {
        return new FIFOQueue<>(name, Integer.MAX_VALUE);
    }

    public static SimulatorQueue<ISerializable> get200PktRDQueue(String name) {
        return new RandomDropQueue<>(name, 200);
//     return new FIFOQueue<>(name, 100);
    }

    public static long getLinkDistance(Node.Link l) {
        return l.getDelay();
    }

    public static int getMinIndex(long[] array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("Input array is null or of 0 size");
        }
        long min = array[0];
        int ret = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) {
                ret = i;
                min = array[i];
            }
        }
        return ret;
    }

    private class RouterInfo {

        public Router router;
        // destination is router
        public DijkstraCalculator dijkstra;
        public long totalDistance;

        public RouterInfo(Router router) {
            this.router = router;
        }

    }

    private class ReceiveInfo {

        public final int id;
        public final long start;
        public int received = 0;
        public long totalLatency = 0;
        public boolean isGame;
        public HashSet<GameClient> toReceives = new HashSet<>();

        private ReceiveInfo(int id, boolean isGame) {
            start = EventQueue.now();
            this.id = id;
            this.isGame = isGame;
        }

        public ReceiveInfo(Collection<GameClient> clients, int id, boolean isGame) {
            this(id, isGame);
            toReceives.addAll(clients);
//            System.out.println(toReceives.size());
        }

        public ReceiveInfo(GameClient client, int id, boolean isGame) {
            this(id, isGame);
            toReceives.add(client);
//            System.out.println(toReceives.size());
        }

        public void clientReceived(GameClient client) {
            if (toReceives == null || !toReceives.remove(client)) {
                receiveErrorWriter.printf("%d\t%s\t+\t%d%n", EventQueue.now(), client.getName(), id);
                ro.accumulateCountAndGet(RO_KEY_REDUNDANT, 1);
            } else {
                received++;
                totalLatency += EventQueue.now() - start;
                if (toReceives.isEmpty()) {
                    PrintStream ps = isGame ? gameReceiveWriter : userReceiveWriter;
                    ps.printf("%d\t%d\t%f%n", id, received, getAverageLatency());
                    ro.accumulateCountAndGet(isGame ? RO_KEY_FINISHED_GAME : RO_KEY_FINISHED_USER, 1);
                    receives.remove(id);
                    if (traceFinished && receives.isEmpty()) {
                        allEventSatisfied();
                    }
                }
            }
        }

        public void clientReceivedVideoStream(GameClient client) {
            if (toReceives != null && toReceives.remove(client)) {
                received++;
                totalLatency += EventQueue.now() - start;
                if (toReceives.isEmpty()) {
                    ((edu.rutgers.winlab.simulator.gaming.videostream.GameServer) server).removePendingUE(GAME_NAME, id);
                    PrintStream ps = isGame ? gameReceiveWriter : userReceiveWriter;
                    ps.printf("%d\t%d\t%f%n", id, received, getAverageLatency());
                    ro.accumulateCountAndGet(isGame ? RO_KEY_FINISHED_GAME : RO_KEY_FINISHED_USER, 1);
                    receives.remove(id);
                    if (traceFinished && receives.isEmpty()) {
                        allEventSatisfied();
                    }
                }
            }
        }

        public double getAverageLatency() {
            return ((double) totalLatency) / received;
        }
    }

    private class EventReader {

        private final BufferedReader _reader;
        private int _eventID = 0;

        public EventReader(String eventFileName) throws IOException {
            _reader = new BufferedReader(new FileReader(eventFileName));
            _addNextEvent();
        }

        private void _addNextEvent() throws IOException {
            String line = _reader.readLine();
//            if (line == null || EventQueue.now() > 100 * EventQueue.SECOND) {
            if (line == null) {
                _finishReader();
            } else {
                String[] parts = line.split("\t");
                long time = Long.parseLong(parts[0]);
                EventQueue.addEvent(time, this::_executeEvent, (Object[]) parts);
            }
        }

        private void _executeEvent(Object... parts) {
            String player = (String) parts[1];
            int size = Integer.parseInt((String) parts[2]) * ISerializable.BYTE;
            boolean isGameEvent = parts[3].equals("1");
            int id = _eventID++;
            putEvent(player, id, size, isGameEvent);
            try {
                _addNextEvent();
            } catch (IOException ex) {
                Logger.getLogger(SimulationRunner.class.getName()).log(Level.SEVERE, null, ex);
                // cannot read, finish trace.
                traceFinished = true;
            }
        }

        private void _finishReader() throws IOException {
            _reader.close();
            traceFinished = true;
        }
    }

    private final HashMap<String, RouterInfo> routers = new HashMap<>();
    private final HashMap<String, GameClient> clients = new HashMap<>();
    private final HashMap<Integer, ReceiveInfo> receives = new HashMap<>();
    private PrintStream userReceiveWriter, gameReceiveWriter, receiveErrorWriter;
    private GameServer server;
    private ReportObject ro;
    private boolean traceFinished = false;

    private void putEvent(String playerName, int id, int size, boolean isGameEvent) {
        GameClient c = clients.get(playerName);
        UserEvent ue = new UserEvent(playerName, id, size, isGameEvent);
        if (isGameEvent) {
            receives.put(ue.getId(), new ReceiveInfo(clients.values(), ue.getId(), isGameEvent));
        } else {
            receives.put(ue.getId(), new ReceiveInfo(c, ue.getId(), isGameEvent));
        }
        c.handleUserEvent(ue);
    }

    private void allEventSatisfied() {
        clients.values().forEach((gc) -> {
            gc.stop();
        });
        server.stop();
    }

    // inits routers, server and clients
    // add routing for GAME_NAME (towards server)
    private RouterInfo initNodesCommon(String time, String connectionFolder, String apConnectionFile,
            Function<String, GameServer> getServerFunction,
            BiFunction<String, String, GameClient> getClientFunction,
            Function<String, SimulatorQueue<ISerializable>> getQueueFunction) throws IOException {
        ///////////////////////// Routers /////////////////////////
        try (BufferedReader br = new BufferedReader(new FileReader(apConnectionFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\t");
                int bw = Integer.parseInt(parts[2]) * ISerializable.M_BIT;
                long latency = Long.parseLong(parts[3]) * EventQueue.MICRO_SECOND;
                String r1Name = parts[0];
//            System.out.printf("%s: %s%n", line, Arrays.toString(parts));
                RouterInfo r1 = routers.get(r1Name);
                if (r1 == null) {
                    routers.put(r1Name, r1 = new RouterInfo(new Router(r1Name, getQueueFunction.apply(r1Name + "_IN"))));
                }
                String r2Name = parts[1];
                RouterInfo r2 = routers.get(r2Name);
                if (r2 == null) {
                    routers.put(r2Name, r2 = new RouterInfo(new Router(r2Name, getQueueFunction.apply(r2Name + "_IN"))));
                }
                Node.connectNodes(r1.router, r2.router, getQueueFunction.apply(r1Name + "->" + r2Name), getQueueFunction.apply(r2Name + "->" + r1Name), bw, latency);
            }
        }
        System.out.printf("# of routers: %d%n", routers.size());

        ///////////////////////// Routing and Center (Server) /////////////////////////
        routers.values().parallelStream().forEach(r -> {
            r.dijkstra = new DijkstraCalculator(r.router, SimulationRunner::getLinkDistance);
            AtomicInteger count = new AtomicInteger();
            AtomicLong totalDistance = new AtomicLong();
            r.dijkstra.forEachDistance((n, di) -> {
                totalDistance.addAndGet(di.getDistance());
                count.incrementAndGet();
            });
            if (count.get() != routers.size()) {
                System.out.println(String.format("ERR: Router %s can only reach %d of %d routers", r.router.getName(), count, routers.size()));
            }
            r.totalDistance = totalDistance.get();
        });
//        routers.values().forEach(r -> System.out.printf("%s: %d%n", r.router.getName(), r.totalDistance));
//        RouterInfo center = routers.values().stream().min((r1, r2) -> Long.compare(r1.totalDistance, r2.totalDistance)).get();
        RouterInfo center = routers.get(CENTER_ROUTER_NAME);
        System.out.printf("Center at: %s (%d)%n", center.router.getName(), center.totalDistance);
        center.dijkstra.forEachDistance((n, di) -> {
            if (n != center.router) {
                ((Router) n).addRouting(GAME_NAME, di.getPrev());
            }
        });

        server = getServerFunction.apply("Server");
        server.move(center.router,
                getQueueFunction.apply("S->" + center.router.getName()),
                getQueueFunction.apply(center.router.getName() + "->S"),
                SERVER_BW_BPS, SERVER_LATENCY);
        center.router.addRouting(GAME_NAME, server);

        ///////////////////////// Clients /////////////////////////
        // Cannot new clients in parallel, since it adds "start render" events to queues.
        File[] clientFiles = new File(connectionFolder).listFiles((dir, name) -> name.startsWith("new_") && name.endsWith(".txt"));
        String[] tmpClients = new String[clientFiles.length];
        RouterInfo[] aps = new RouterInfo[clientFiles.length];
        Arrays.parallelSetAll(aps, i -> {
            File clientFile = clientFiles[i];
            String fname = clientFile.getName();
//            // remove ".txt"
            String name = fname.substring(0, fname.length() - 4);
            tmpClients[i] = name;
//            System.out.println(name);
            try (BufferedReader br = new BufferedReader(new FileReader(clientFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("\t");
                    if (!parts[0].equals(time)) {
                        continue;
                    }
                    String router = parts[1];
                    return routers.get(router);
                }
            } catch (IOException ex) {
                Logger.getLogger(SimulationRunner.class.getName()).log(Level.SEVERE, null, ex);
            }
            throw new RuntimeException(String.format("Should not reach here, cannot find time %s in file %s", time, clientFile));
        });

        for (int i = 0; i < clientFiles.length; i++) {
            GameClient gc = getClientFunction.apply(tmpClients[i], GAME_NAME);
            String gcName = gc.getName();
            RouterInfo ri = aps[i];
            Router r = ri.router;
            String routerName = r.getName();
            clients.put(gc.getName(), gc);
//            System.out.printf("%s->%s%n", gcName, routerName);
            gc.move(r, getQueueFunction.apply(gcName + "->" + routerName), getQueueFunction.apply(routerName + "->" + gcName), EDGE_BW_BPS, EDGE_LATENCY);

        }
        System.gc();
        System.out.printf("Total # of clients: %d%n", clients.size());
        return center;
    }

    private void startupCommon(String userEventOutput, String gameEventOutput, String eventMiss) throws IOException {
        userReceiveWriter = new PrintStream(userEventOutput);
        gameReceiveWriter = new PrintStream(gameEventOutput);
        receiveErrorWriter = new PrintStream(eventMiss);

        ro = new ReportObject();
        ro.setKey("now", () -> String.format("%,d", EventQueue.now()));
        ro.setKey("eventqueue", () -> String.format("%,d", EventQueue.size()));
        ro.setKey("pending", () -> String.format("%,d", receives.size()));
        ro.setKey("redundant", RO_KEY_REDUNDANT);
        ro.setKey("miss", RO_KEY_MISS);
        ro.setKey("finished_u", RO_KEY_FINISHED_USER);
        ro.setKey("finished_g", RO_KEY_FINISHED_GAME);
    }

    private void cleanupCommon(String fr) throws IOException {
        System.out.println();
        System.out.println("Writing content back...");

        try (PrintStream ps = new PrintStream(new FileOutputStream(fr), false)) {
            clients.values().forEach(c -> {
                ps.printf("%s\t%f%n", c.getName(), c.getAvgFrameLatency());
            });
            ps.flush();
        }

        receives.values().forEach((rcv) -> {
            if (rcv.toReceives != null) {
                rcv.toReceives.forEach((gc) -> {
                    receiveErrorWriter.printf("%d\t%s\t-\t%d%n", EventQueue.now(), gc.getName(), rcv.id);
                    ro.accumulateCountAndGet(RO_KEY_MISS, 1);
                    ((Router) gc.getFirstHop()).printRouting(System.out);
                });
            }
        });

        userReceiveWriter.flush();
        userReceiveWriter.close();
        gameReceiveWriter.flush();
        gameReceiveWriter.close();
        receiveErrorWriter.flush();
        receiveErrorWriter.close();

        ro.endReport();

        routers.values().forEach((ri) -> {
            ri.router.reportLinksStat();
        });

        clients.values().forEach((client) -> {
            client.reportLinksStat();
        });

        server.reportLinksStat();
    }

    private void eventReceiveHandlerTraditional(GameClient client, UserEvent evt) {
//        System.out.printf("[%d] RCV %s received %s%n", EventQueue.now(), client.getName(), evt.getId());

        ReceiveInfo ri = receives.get(evt.getId());
        if (ri == null) {
            receiveErrorWriter.printf("%d\t%s\t+\t%d%n", EventQueue.now(), client.getName(), evt.getId());
            ro.accumulateCountAndGet(RO_KEY_REDUNDANT, 1);
        } else {
            ri.clientReceived(client);
        }
    }

    public void runTraditionalGame(String time, boolean hec) throws IOException {
        String userEventOutput, gameEventOutput, eventMiss, fr, link,
                connectionFolder = dataFolder + "connections",
                apConnectionFile = dataFolder + "aps_links.txt",
                eventFile = dataFolder + "events.txt";
        String timePrefix = time.replace(':', '_');
        if (hec) {
            System.out.println("This is high end client scenario");
            userEventOutput = timePrefix + "_u_HEC.txt";
            gameEventOutput = timePrefix + "_g_HEC.txt";
            eventMiss = timePrefix + "_miss_HEC.txt";
            fr = timePrefix + "_fr_HEC.txt";
            link = timePrefix + "_lnks_HEC.txt";
            edu.rutgers.winlab.simulator.gaming.traditional.GameClient.getGameLogicProcessingTime = edu.rutgers.winlab.simulator.gaming.traditional.GameClient::getGameLogicProcessingTime_HEC;
            edu.rutgers.winlab.simulator.gaming.traditional.GameClient.getRenderProcessingTime = edu.rutgers.winlab.simulator.gaming.traditional.GameClient::getRenderProcessingTime_HEC;
            edu.rutgers.winlab.simulator.gaming.traditional.GameClient.getUpdateProcessingTime = edu.rutgers.winlab.simulator.gaming.traditional.GameClient::getUpdateProcessingTime_HEC;
        } else {
            System.out.println("This is low end client scenario");
            userEventOutput = timePrefix + "_u_LEC.txt";
            gameEventOutput = timePrefix + "_g_LEC.txt";
            eventMiss = timePrefix + "_miss_LEC.txt";
            fr = timePrefix + "_fr_LEC.txt";
            link = timePrefix + "_lnks_LEC.txt";
            edu.rutgers.winlab.simulator.gaming.traditional.GameClient.getGameLogicProcessingTime = edu.rutgers.winlab.simulator.gaming.traditional.GameClient::getGameLogicProcessingTime_LEC;
            edu.rutgers.winlab.simulator.gaming.traditional.GameClient.getRenderProcessingTime = edu.rutgers.winlab.simulator.gaming.traditional.GameClient::getRenderProcessingTime_LEC;
            edu.rutgers.winlab.simulator.gaming.traditional.GameClient.getUpdateProcessingTime = edu.rutgers.winlab.simulator.gaming.traditional.GameClient::getUpdateProcessingTime_LEC;
        }

        System.out.printf("time: %s, output: %s%n", time, userEventOutput);
        try (PrintStream linkPs = new PrintStream(link)) {
            Node.NetTrafficWriter = linkPs;

            EventQueue.reset();
            startupCommon(userEventOutput, gameEventOutput, eventMiss);

            RouterInfo center = initNodesCommon(time, connectionFolder, apConnectionFile,
                    name -> new edu.rutgers.winlab.simulator.gaming.traditional.GameServer(name, getInfiniteQueue(name + "_IN")),
                    (name, game) -> new edu.rutgers.winlab.simulator.gaming.traditional.GameClient(name, getInfiniteQueue(name + "_IN"), game, this::eventReceiveHandlerTraditional),
                    SimulationRunner::getInfiniteQueue);

            ///////////////////////// Routing for downstream /////////////////////////
            clients.values().forEach(client -> {
                DijkstraCalculator d = center.dijkstra;
                Router r = (Router) client.getFirstHop();

                Node curr = r;
                while (curr != center.router) {
                    DijkstraCalculator.DistanceInfo di = d.getDistanceInfo(curr);
                    Node prev = di.getPrev();
                    ((Router) prev).addRouting(GAME_DOWN_NAME, curr);
                    curr = prev;
                }
                r.addRouting(GAME_DOWN_NAME, client);
            });

            ///////////////////////// Trace /////////////////////////
//        EventQueue.addEvent(EventQueue.now() + EventQueue.SECOND, (args) -> {
//            putEvent("new_abcoij", 0, 100 * ISerializable.BYTE, true);
//        });
//        EventQueue.addEvent(EventQueue.now() + EventQueue.SECOND * 2, (args) -> {
//            putEvent("new_abcoij", 1, 100 * ISerializable.BYTE, false);
//            traceFinished = true;
//        });
            EventReader reader = new EventReader(eventFile);

            ro.beginReport();
            EventQueue.run();

            cleanupCommon(fr);

            linkPs.flush();
        }
    }

    private void eventReceiveHandlerVideoStream(GameClient client, UserEvent evt) {
//        System.out.printf("[%d] RCV %s received %s%n", EventQueue.now(), client.getName(), evt.getId());
        ReceiveInfo ri = receives.get(evt.getId());
        if (ri != null) {
            ri.clientReceivedVideoStream(client);
        }
    }

    private void runVideoStreamGame(String time) throws IOException {
        String timePrefix = time.replace(':', '_');
        String userEventOutput = timePrefix + "_u_VS.txt",
                gameEventOutput = timePrefix + "_g_VS.txt",
                eventMiss = timePrefix + "_miss_VS.txt",
                fr = timePrefix + "_fr_VS.txt",
                link = timePrefix + "_lnks_VS.txt",
                connectionFolder = dataFolder + "connections",
                apConnectionFile = dataFolder + "aps_links.txt",
                eventFile = dataFolder + "events.txt";
        System.out.println("This is video stream scenario");
        System.out.printf("time: %s, output: %s%n", time, userEventOutput);
        try (PrintStream linkPs = new PrintStream(link)) {
            Node.NetTrafficWriter = linkPs;
            EventQueue.reset();
            startupCommon(userEventOutput, gameEventOutput, eventMiss);
            ro.setKey("ServerPE", () -> String.format("%,d", ((edu.rutgers.winlab.simulator.gaming.videostream.GameServer) server).getPendingUESize(GAME_NAME)));
            ro.setKey("QueueDrop", () -> String.format("%,d", SimulatorQueue.getTotalDropCount()));

            RouterInfo center = initNodesCommon(time, connectionFolder, apConnectionFile,
                    name -> new edu.rutgers.winlab.simulator.gaming.videostream.GameServer(name, get200PktRDQueue(name + "_IN")),
                    (name, game) -> new edu.rutgers.winlab.simulator.gaming.videostream.GameClient(name, get200PktRDQueue(name + "_IN"), game, this::eventReceiveHandlerVideoStream),
                    SimulationRunner::get200PktRDQueue);

            ///////////////////////// Routing for downstream /////////////////////////
            clients.values().forEach(client -> {
                Router r = (Router) client.getFirstHop();
                RouterInfo ri = routers.get(r.getName());
                DijkstraCalculator d = ri.dijkstra;

                server.addGameClient(GAME_NAME, client.getName());

                Node curr = center.router;
                while (curr != r) {
                    DijkstraCalculator.DistanceInfo di = d.getDistanceInfo(curr);
                    Node prev = di.getPrev();
                    ((Router) curr).addRouting(client.getName(), prev);
                    curr = prev;
                }
                r.addRouting(client.getName(), client);
            });

            ///////////////////////// Trace /////////////////////////
//            EventQueue.addEvent(EventQueue.now() + EventQueue.SECOND, (args) -> {
//                putEvent("new_abcoij", 0, 100 * ISerializable.BYTE, true);
//            });
//            EventQueue.addEvent(EventQueue.now() + EventQueue.SECOND * 2, (args) -> {
//                putEvent("new_abcoij", 1, 100 * ISerializable.BYTE, false);
//                traceFinished = true;
//            });
            EventReader reader = new EventReader(eventFile);

            ro.beginReport();
            EventQueue.run();

            cleanupCommon(fr);
            linkPs.flush();
        }
    }
}
