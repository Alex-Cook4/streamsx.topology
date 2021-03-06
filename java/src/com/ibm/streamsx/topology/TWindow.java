/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2015  
 */
package com.ibm.streamsx.topology;

import java.util.concurrent.TimeUnit;

import com.ibm.streamsx.topology.function7.BiFunction;
import com.ibm.streamsx.topology.function7.Function;
import com.ibm.streamsx.topology.tuple.Keyable;

/**
 * Declares a window of tuples for a {@link TStream}. Logically a {@code Window}
 * represents an continuously updated ordered list of tuples according to the
 * criteria that created it. For example {@link TStream#last(int) s.last(10)}
 * declares a window that at any time contains the last ten tuples seen on
 * stream {@code s}, while
 * {@link TStream#last(long, java.util.concurrent.TimeUnit) s.last(5,
 * TimeUnit.SECONDS)} is a window that always contains all tuples present in the
 * {@code s} in the last five seconds.. <BR>
 * When {@code T} implements {@link Keyable} then the window is partitioned,
 * using the value of {@link Keyable#getKey()}, this means each partition
 * independently maintains the declared window contents for its key.
 * 
 * @param <T>
 *            Tuple type, any instance of {@code T} at runtime must be
 *            serializable.
 * 
 * @see TStream#last()
 * @see TStream#last(int)
 * @see TStream#last(long, java.util.concurrent.TimeUnit)
 */
public interface TWindow<T> extends TopologyElement {

    /**
     * Declares a stream that containing tuples that represent an aggregation of
     * this window. Each time the contents of the window is updated by a new
     * tuple being added to it, or a tuple being evicted from the window
     * {@code aggregator.call(tuples)} is called, where {@code tuples} is an
     * {@code Iterable} that containing all the tuples in the current window.
     * The {@code Iterable} is stable during the method call, and returns the
     * tuples in order of insertion into the window, from oldest to newest. <BR>
     * Thus the returned stream will contain a sequence of tuples that where the
     * most recent tuple represents the most up to date aggregation of this
     * window or window partition.
     * 
     * @param aggregator
     *            Logic to aggregation the complete window contents.
     * @param tupleClass
     *            Class of the tuples in the returned stream.
     * @return A stream that contains the latest aggregations of this window.
     */
    <A> TStream<A> aggregate(Function<Iterable<T>, A> aggregator,
            Class<A> tupleClass);
    
    /**
     * Declares a stream that containing tuples that represent an aggregation of
     * this window. Approximately every {@code period} (with unit {@code unit})
     * {@code aggregator.call(tuples)} is called, where {@code tuples} is an
     * {@code Iterable} that containing all the tuples in the current window.
     * The {@code Iterable} is stable during the method call, and returns the
     * tuples in order of insertion into the window, from oldest to newest. <BR>
     * Thus the returned stream will contain a new tuple every {@code period} seconds
     * (according to {@code unit}) aggregation of this
     * window or window partition.
 
     * @param aggregator
     *            Logic to aggregation the complete window contents.
     * @param period Approximately how often to perform the aggregation.
     * @param unit Time unit for {@code period}.
     * @param tupleClass
     *            Class of the tuples in the returned stream.
     * @return A stream that contains the latest aggregations of this window.
     */
    <A> TStream<A> aggregate(Function<Iterable<T>, A> aggregator,
            long period,
            TimeUnit unit,
            Class<A> tupleClass);

    /**
     * Join this window to a stream. This method is identical to calling
     * {@link TStream#join(TWindow, BiFunction, Class) stream.join(this, joiner,
     * tupleClass)}.
     * 
     * @param stream
     *            Stream to join with.
     * @param joiner
     *            logic processing the join.
     * @param tupleClass
     *            Tuple type for the returned stream.
     * @return Stream containing the results of the join.
     * 
     * @see TStream#join(TWindow, BiFunction, Class)
     */
    <J, U> TStream<J> join(TStream<U> stream,
            BiFunction<U, Iterable<T>, J> joiner, Class<J> tupleClass);

    /**
     * The class for tuples on this stream.
     * 
     * @return The class for tuples on this stream.
     */
    Class<T> getTupleClass();
}
