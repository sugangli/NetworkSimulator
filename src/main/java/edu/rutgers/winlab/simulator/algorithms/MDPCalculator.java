/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.algorithms;

import edu.rutgers.winlab.simulator.core.Node;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;

/**
 *
 * @author ubuntu
 */
public final class MDPCalculator {

    private final double _sigma;
    // 1d
    private final Node[] _serverLocations, _clientLocations;
    // 3d, row: prev server locations, col: (row: next server locations, col: next client locations)
    private final double[] _rewards;
    // 2d, row: prev client location, col: next client location
    private final double[] _moveProbabilities;
    // 2d: row: prev server location, col: prev client location
    private final double[] _stateUtilties;
    // 2d: row: prev server location, col: prev client location
    private final int[] _stateActions;

    private final TriFunction<Node, Node, Node, Double> _rewardCalculator;

    public MDPCalculator(
            final Collection<Node> serverLocations,
            final Collection<Node> clientLocations,
            final double[] moveProbabilities,
            final TriFunction<Node, Node, Node, Double> rewardCalculator,
            final double sigma) {
        _serverLocations = new Node[serverLocations.size()];
        serverLocations.toArray(_serverLocations);
        _clientLocations = new Node[clientLocations.size()];
        clientLocations.toArray(_clientLocations);
        _moveProbabilities = moveProbabilities;
        _rewardCalculator = rewardCalculator;
        _rewards = new double[_serverLocations.length * _serverLocations.length * _clientLocations.length];
        _stateUtilties = new double[_serverLocations.length * _serverLocations.length * _clientLocations.length];
        _stateActions = new int[_serverLocations.length * _clientLocations.length];
        clearStateResults();
        _sigma = sigma;
    }

    private int calculatePrevServerLocation(int prevServerLocation) {
        int start = prevServerLocation * _serverLocations.length * _clientLocations.length;
        Node prevServer = _serverLocations[prevServerLocation];
        for (Node nextServer : _serverLocations) {
            for (Node _clientLocation : _clientLocations) {
                _rewards[start++] = _rewardCalculator.apply(prevServer, nextServer, _clientLocation);
            }
        }
        return prevServerLocation;
    }

    public synchronized void calculateRewards() {
        // 1d, len= # of server locations, help to do parallel
        double[] _helper = new double[_serverLocations.length];
        Arrays.parallelSetAll(_helper, this::calculatePrevServerLocation);
//        for (int i = 0; i < _serverLocations.length; i++) {
//            calculatePrevServerLocation(i);
//        }
    }

    public synchronized void printRewards(PrintStream ps) {
        for (int sn = 0; sn < _serverLocations.length; sn++) {
            for (int cn = 0; cn < _clientLocations.length; cn++) {
                ps.printf("\tSN:%d,CN:%d", sn, cn);
            }
        }
        System.out.println();
        int id = 0;
        for (int sp = 0; sp < _serverLocations.length; sp++) {
            ps.printf("SP:%d", sp);
            for (int s = 0; s < _serverLocations.length; s++) {
                for (int c = 0; c < _clientLocations.length; c++) {
                    ps.printf("\t%f", _rewards[s * _serverLocations.length + c]);
                }
            }
            ps.println();
        }
    }

    public synchronized void clearStateResults() {
        Arrays.parallelSetAll(_stateUtilties, i -> 0);
        Arrays.parallelSetAll(_stateActions, i -> 0);
    }

    private double[] createHelperForValueIteration() {
        // 2d, row: # of server locations, col: # of client locations, help to do parallel
        return new double[_serverLocations.length * _clientLocations.length];
    }

    private double _valueIterationOneState(int prevState) {
        int prevServerLocation = prevState / _clientLocations.length;
        int prevClientLocation = prevState % _clientLocations.length;
        int mobilityStart = prevClientLocation * _clientLocations.length;
        int rewardNow = prevServerLocation * _serverLocations.length * _clientLocations.length;

        int nextServerLocation = 0;
        int nextStateUtility = 0;
        double actionUtility = 0;
        for (int nextClientLocation = 0, mobilityNow = mobilityStart;
                nextClientLocation < _clientLocations.length;
                nextClientLocation++, mobilityNow++, rewardNow++, nextStateUtility++) {
            actionUtility += (_rewards[rewardNow] + _stateUtilties[nextStateUtility] * _sigma) * _moveProbabilities[mobilityNow];
        }
        int maxAction = nextServerLocation;
        double maxUtility = actionUtility;
        nextServerLocation++;

        for (; nextServerLocation < _serverLocations.length; nextServerLocation++) {
            actionUtility = 0;
            for (int nextClientLocation = 0, mobilityNow = mobilityStart;
                    nextClientLocation < _clientLocations.length;
                    nextClientLocation++, mobilityNow++, rewardNow++, nextStateUtility++) {
                actionUtility += (_rewards[rewardNow] + _stateUtilties[nextStateUtility] * _sigma) * _moveProbabilities[mobilityNow];
            }
            if (actionUtility > maxUtility || (nextServerLocation == prevServerLocation && actionUtility == maxUtility)) {
                maxAction = nextServerLocation;
                maxUtility = actionUtility;
            }
        }
        _stateActions[prevState] = maxAction;
//        System.out.println(String.format("%d\t%d\t%d\t%f", prevServerLocation, prevClientLocation, maxAction, maxUtility));
        return maxUtility;
    }

    private double _innerValueIterationOneRound(double[] helper) {
        Arrays.parallelSetAll(helper, this::_valueIterationOneState);
        double maxDiff = Math.abs(helper[0] - _stateUtilties[0]);
        _stateUtilties[0] = helper[0];

        for (int i = 1; i < helper.length; i++) {
            double diff = Math.abs(helper[i] - _stateUtilties[i]);
            _stateUtilties[i] = helper[i];
            maxDiff = Math.max(diff, maxDiff);
        }
        return maxDiff;
    }

    public double valueIterationOneRound() {
        return _innerValueIterationOneRound(createHelperForValueIteration());
    }

    public synchronized void valueIteration(double threshold) {
        clearStateResults();
        double[] helper = createHelperForValueIteration();
        double diff;
        int i = 0;
        do {
            long start = System.nanoTime();
            diff = _innerValueIterationOneRound(helper);
            long end = System.nanoTime();
            System.out.printf("Iter: %d, diff: %f, time: %f ns%n", i++, diff, (end - start) / 1000000000.0);
        } while (diff > threshold);
    }

    public synchronized void printUtilityAndActions(PrintStream ps) {
        ps.println("S\tC\tA\tU");
        for (int i = 0, pos = 0; i < _serverLocations.length; i++) {
            for (int j = 0; j < _clientLocations.length; j++, pos++) {
                ps.printf("%d\t%d\t%d\t%f%n", i, j, _stateActions[i * _clientLocations.length + j], _stateUtilties[i * _clientLocations.length + j]);
            }
        }
    }

}
