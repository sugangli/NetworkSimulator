/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.simulator.algorithms;

import java.util.Objects;
import java.util.function.Function;

/**
 *
 * @author ubuntu
 * @param <R>
 * @param <T1>
 * @param <T2>
 * @param <T3>
 */
@FunctionalInterface
public interface TriFunction<T1, T2, T3, R> {

    R apply(T1 t1, T2 t2, T3 t3);

    default <V> TriFunction<T1, T2, T3, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T1 t1, T2 t2, T3 t3) -> after.apply(apply(t1, t2, t3));

    }
}
