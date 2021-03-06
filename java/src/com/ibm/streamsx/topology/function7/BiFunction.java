/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2015  
 */
package com.ibm.streamsx.topology.function7;

import java.io.Serializable;

/**
 * A function that is passed two arguments and returns a value.
 * 
 * @param <T1>
 *            Type of the first argument
 * @param <T2>
 *            Type of the first second argument
 * @param <R>
 *            Type of the return value.
 */
public interface BiFunction<T1, T2, R> extends Serializable {
    R apply(T1 v1, T2 v2);
}
