/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.gaming.common;

import edu.rutgers.winlab.simulator.algorithms.MDPCalculator;
import edu.rutgers.winlab.simulator.core.FIFOQueue;
import edu.rutgers.winlab.simulator.core.ISerializable;
import edu.rutgers.winlab.simulator.core.Node;
import edu.rutgers.winlab.simulator.core.Serial;
import edu.rutgers.winlab.simulator.core.SimulatorQueue;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ubuntu
 */
public class SimulationRunner {

    private static class MyNode extends Node {

        private final int _x, _y;

        public MyNode(String name, SimulatorQueue<ISerializable> innerIncomingQueue, int x, int y) {
            super(name, innerIncomingQueue);
            _x = x;
            _y = y;
        }

        public int getX() {
            return _x;
        }

        public int getY() {
            return _y;
        }

        @Override
        protected long _processPacket(Serial<ISerializable> s, ISerializable param) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        protected void _handleFailedPacket(ISerializable packet) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    private static SimulatorQueue<ISerializable> _getInnerIncomingQueue(String name) {
        return new FIFOQueue<>(name + "IN", Integer.MAX_VALUE);
    }

    private static double _calculateReward(Node prevServer, Node nextServer, Node nextClient) {
        MyNode sp = (MyNode) prevServer, sn = (MyNode) nextServer, cn = (MyNode) nextClient;
        int d_sp_cn = Math.abs(sp._x - cn._x) + Math.abs(sp._y - cn._y);
        int d_sp_cent = Math.abs(sp._x - _center._x) + Math.abs(sp._y - _center._y);
        int d_sn_cn = Math.abs(sn._x - cn._x) + Math.abs(sn._y - cn._y);
        int d_sn_cent = Math.abs(sn._x - _center._x) + Math.abs(sn._y - _center._y);
        int d_sp_sn = Math.abs(sp._x - sn._x) + Math.abs(sp._y - sn._y);
        return (d_sp_cn + d_sp_cent) - (d_sn_cn + d_sn_cent) - d_sp_sn * MIGTATE_COST;
    }

    private static double[] _createMoveProbabilityCircle(int locationCount) {
        double[] probabilities = new double[locationCount * locationCount];
        for (int i = 0; i < locationCount; i++) {
            probabilities[i * locationCount + (i + 1) % locationCount] = 1;
        }
        return probabilities;
    }

    public static void _printProbabilities(double[] probabilities, PrintStream ps) {
        int len = (int) Math.sqrt(probabilities.length);
        for (int to = 0; to < len; to++) {
            ps.printf("\t%d", to);
        }
        ps.println();
        for (int from = 0; from < len; from++) {
            ps.printf("%d:", from);
            for (int to = 0; to < len; to++) {
                int i = from * len + to;
//                ps.printf("\t%d(%d)", (int) probabilities[i], i);
                ps.printf("\t%d", (int) probabilities[i]);
            }
            ps.println();
        }
    }

    private static MyNode _center;
    private static final double MIGTATE_COST = 0;

    public static void main(String[] prams) {
        int N = 100;
        double sigma = 0.75;
        double threshold = 0.01;
        MyNode[] nodes = new MyNode[N * N];
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                String name = String.format("%d,%d", x, y);
                nodes[y * N + x] = new MyNode(name, _getInnerIncomingQueue(name), x, y);
            }
        }
        _center = nodes[(N / 2) * N + N / 2];

        LinkedList<Node> _serverLocations = new LinkedList<>();
        for (int i = 0; i < N - 1; i++) {
            _serverLocations.add(nodes[0 * N + i]);
        }
        for (int i = 0; i < N - 1; i++) {
            _serverLocations.add(nodes[i * N + (N - 1)]);
        }
        for (int i = 0; i < N - 1; i++) {
            _serverLocations.add(nodes[(N - 1) * N + (N - i - 1)]);
        }
        for (int i = 0; i < N - 1; i++) {
            _serverLocations.add(nodes[(N - i - 1) * N + 0]);
        }
        LinkedList<Node> _clientLocations = new LinkedList<>(_serverLocations);
//        _clientLocations.forEach(System.out::println);

        double[] probabilities = _createMoveProbabilityCircle(_clientLocations.size());
//        _printProbabilities(probabilities, System.out);

        MDPCalculator c = new MDPCalculator(_serverLocations, _clientLocations, probabilities, SimulationRunner::_calculateReward, sigma);
        System.out.println("Calculating rewards...");
        long prev = System.nanoTime();
        c.calculateRewards();
        long after = System.nanoTime();
        System.out.printf("Finished in %d ns%n", after - prev);

//        try (PrintStream ps = new PrintStream("rewards.txt")) {
//            c.printRewards(ps);
//        } catch (IOException ex) {
//            Logger.getLogger(SimulationRunner.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        System.out.println("Calculating actions...");
//        prev = System.nanoTime();
//        double diff = c.valueIterationOneRound();
//        after = System.nanoTime();
//        System.out.printf("Finished in %d ns, diff=%f%n", after - prev, diff);
//
//        prev = System.nanoTime();
//        diff = c.valueIterationOneRound();
//        after = System.nanoTime();
//        System.out.printf("Finished in %d ns, diff=%f%n", after - prev, diff);
        System.out.println("Calculating actions...");
        prev = System.nanoTime();
        c.valueIteration(threshold);
        after = System.nanoTime();
        System.out.printf("Finished in %d ns%n", after - prev);

        try (PrintStream ps = new PrintStream("ActionsUtils.txt")) {
            c.printUtilityAndActions(ps);
        } catch (Exception ex) {
            Logger.getLogger(SimulationRunner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
