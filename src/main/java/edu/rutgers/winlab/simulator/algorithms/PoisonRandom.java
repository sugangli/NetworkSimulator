/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.algorithms;

import java.util.Random;

/**
 *
 * @author ubuntu
 */
public class PoisonRandom {

    private static final Random RAND = new Random(0);
    public static final int STEP = 200;

    private final double _lambda;

    public PoisonRandom(double lambda) {
        _lambda = lambda;
    }

    public int getNext() {
        double p = 1, lambdaLeft = _lambda;
        int k = 0;
        do {
            k++;
            double u = RAND.nextDouble();
            p *= u;
            if (p < Math.E && lambdaLeft > 0) {
                if (lambdaLeft > STEP) {
                    p *= Math.exp(STEP);
                    lambdaLeft -= STEP;
                } else {
                    p *= Math.exp(lambdaLeft);
                    lambdaLeft = -1;
                }
            }
//            System.out.printf("p=%f%n", p);
        } while (p > 1);
//        System.out.printf("k=%d%n", k);
        return k - 1;
    }
}
