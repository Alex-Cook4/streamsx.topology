/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2015  
 */
package com.ibm.streamsx.topology.internal.functional.ops;

import static com.ibm.streamsx.topology.internal.functional.FunctionalHelper.getInputMapping;
import static com.ibm.streamsx.topology.internal.functional.FunctionalHelper.getLogicObject;

import com.ibm.streams.operator.OperatorContext;
import com.ibm.streams.operator.OutputTuple;
import com.ibm.streams.operator.StreamingInput;
import com.ibm.streams.operator.StreamingOutput;
import com.ibm.streams.operator.Tuple;
import com.ibm.streams.operator.model.Icons;
import com.ibm.streams.operator.model.InputPortSet;
import com.ibm.streams.operator.model.OutputPortSet;
import com.ibm.streams.operator.model.PrimitiveOperator;
import com.ibm.streamsx.topology.function7.Predicate;
import com.ibm.streamsx.topology.internal.spljava.SPLMapping;

@PrimitiveOperator
@InputPortSet(cardinality = 1)
@OutputPortSet(cardinality = 1)
@Icons(location16 = "opt/icons/filter_16.gif", location32 = "opt/icons/filter_32.gif")
public class FunctionFilter<T> extends FunctionFunctor {

    private Predicate<T> filter;
    private SPLMapping<T> mapping;
    private StreamingOutput<OutputTuple> passed;

    @Override
    public void initialize(OperatorContext context) throws Exception {
        super.initialize(context);

        filter = getLogicObject(getFunctionalLogic());
        passed = getOutput(0);
        mapping = getInputMapping(this, 0);
    }

    @Override
    public void process(StreamingInput<Tuple> stream, Tuple tuple)
            throws Exception {
        T value = mapping.convertFrom(tuple);

        boolean submitTuple;
        synchronized (filter) {
            submitTuple = filter.test(value);
        }
        if (submitTuple)
            passed.submit(tuple);
    }
}
