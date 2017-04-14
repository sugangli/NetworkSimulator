/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.common;

import edu.rutgers.winlab.simulator.algorithms.PoisonRandom;
import edu.rutgers.winlab.simulator.algorithms.ReportObject;
import edu.rutgers.winlab.simulator.core.EventQueue;
import edu.rutgers.winlab.simulator.core.Serial;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author ubuntu
 */
public class TraceGenerator {

    public TraceGenerator() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private static class Player {

        private static final Random RAND = new Random(0);
        private static final PoisonRandom SIZE_POISON = new PoisonRandom(39.72);

        private final String _name;
        private final double _lambda, _gePortion;
        private final long _endTime;
        private final PoisonRandom _rand;
        private final PrintStream _ps;
        private final ReportObject _ro;

        public Player(String _name, double _lambda, double _gePortion, long _endTime, PrintStream ps, ReportObject ro) {
            this._name = _name;
            this._lambda = _lambda;
            this._gePortion = _gePortion;
            this._endTime = _endTime;
            this._ps = ps;
            this._ro = ro;
            _rand = new PoisonRandom(_lambda);
            new Serial<Integer>(this::firstEvent, 0);
        }

        private long firstEvent(Serial<Integer> s, Integer val) {
            if (EventQueue.now() > _endTime) {
                return 0;
            }
            long delay = _rand.getNext() * EventQueue.MILLI_SECOND + RAND.nextInt((int) EventQueue.MILLI_SECOND);
            s.addEvent(this::getNextEvent, val);
            return delay;
        }

        private long getNextEvent(Serial<Integer> s, Integer val) {
            if (EventQueue.now() > _endTime) {
                return 0;
            }
            _ro.accumulateCountAndGet(0, 1);
            boolean isGameEvent = RAND.nextDouble() < _gePortion;
            if(isGameEvent) _ro.accumulateCountAndGet(1, 1);
            _ps.printf("%d\t%s\t%d\t%d%n", EventQueue.now(), _name, SIZE_POISON.getNext(), isGameEvent ? 1 : 0);
            long delay = _rand.getNext() * EventQueue.MILLI_SECOND + RAND.nextInt((int) EventQueue.MILLI_SECOND);
            s.addEvent(this::getNextEvent, val);
            return delay;
        }
    }

//    @Test
    public void generateTrace() throws IOException {
        String clientFile = "SF/vehicle0531_filt2/clients.txt";
        String traceOutput = "SF/vehicle0531_filt2/events.txt";
        EventQueue.reset();
        long endTime = 60 * 60 * EventQueue.SECOND;
        ReportObject ro = new ReportObject();
        ro.setKey("ue", 0);
        ro.setKey("ge", 1);
        ro.setKey("now", ()->String.format("%,d", EventQueue.now()));
        ro.beginReport();
        try (BufferedReader br = new BufferedReader(new FileReader(clientFile))) {
            try (PrintStream ps = new PrintStream(traceOutput)) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("#")) {
                        continue;
                    }
                    String[] parts = line.split("\t");
                    Player p = new Player(parts[0], Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), endTime, ps, ro);
                }
                EventQueue.run();
                ro.endReport();
                ps.flush();
            }
        }
    }
}
