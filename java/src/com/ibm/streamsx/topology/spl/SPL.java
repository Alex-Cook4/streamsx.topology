/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2015  
 */
package com.ibm.streamsx.topology.spl;

import static com.ibm.streamsx.topology.internal.core.InternalProperties.TK_DIRS;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.streams.operator.StreamSchema;
import com.ibm.streamsx.topology.TStream;
import com.ibm.streamsx.topology.TopologyElement;
import com.ibm.streamsx.topology.builder.BInputPort;
import com.ibm.streamsx.topology.builder.BOperatorInvocation;
import com.ibm.streamsx.topology.builder.BOutputPort;
import com.ibm.streamsx.topology.internal.core.SourceInfo;

/**
 * Integration between Java topologies and SPL operator invocations. If the SPL
 * operator to be invoked is an SPL Java primitive operator then the methods of
 * {@link JavaPrimitive} should be used.
 */
public class SPL {

    /**
     * Create an SPLStream from the invocation of an SPL operator
     * that consumes a stream.
     * 
     * @param kind
     *            SPL kind of the operator to be invoked.
     * @param input
     *            Stream that will be connected to the only input port of the
     *            operator
     * @param outputSchema
     *            SPL schema of the operator's only output port.
     * @param params
     *            Parameters for the SPL operator, ignored if it is null.
     * @return SPLStream the represents the output of the operator.
     */
    public static SPLStream invokeOperator(String kind, TStream<?> input,
            StreamSchema outputSchema, Map<String, ? extends Object> params) {

        BOperatorInvocation op = input.builder().addSPLOperator(
                opNameFromKind(kind), kind, params);
        SourceInfo.setSourceInfo(op, SPL.class);
        BInputPort inputPort = input.connectTo(op, false, null);
        BOutputPort stream = op.addOutput(outputSchema);
        return new SPLStreamImpl(input, stream);
    }
    public static SPLStream invokeOperator(String name, String kind, TStream<?> input,
            StreamSchema outputSchema, Map<String, ? extends Object> params) {

        BOperatorInvocation op = input.builder().addSPLOperator(name, kind, params);
        SourceInfo.setSourceInfo(op, SPL.class);
        BInputPort inputPort = input.connectTo(op, false, null);
        BOutputPort stream = op.addOutput(outputSchema);
        return new SPLStreamImpl(input, stream);
    }


    /**
     * Invocation an SPL operator that consumes a Stream.
     * 
     * @param kind
     *            SPL kind of the operator to be invoked.
     * @param input
     *            Stream that will be connected to the only input port of the
     *            operator
     * @param params
     *            Parameters for the SPL operator, ignored if it is null.
     */
    public static void invokeSink(String kind, TStream<?> input,
            Map<String, ? extends Object> params) {
        


        BOperatorInvocation op = input.builder().addSPLOperator(
                opNameFromKind(kind), kind, params);
        SourceInfo.setSourceInfo(op, SPL.class);
        input.connectTo(op, false, null);
    }
    
    private static String opNameFromKind(String kind) {
        String opName;
        if (kind.contains("::"))
            opName = kind.substring(kind.lastIndexOf(':') + 1);
        else
            opName = kind;
        return opName;
    }

    /**
     * Invocation an SPL source operator to produce a Stream.
     * 
     * @param te
     *            Reference to Topology the operator will be in.
     * @param kind
     *            SPL kind of the operator to be invoked.
     * @param params
     *            Parameters for the SPL operator.
     * @param schema
     *            Schema of the output port.
     * @return SPLStream the represents the output of the operator.
     */
    public static SPLStream invokeSource(TopologyElement te, String kind,
            Map<String, Object> params, StreamSchema schema) {

        BOperatorInvocation splSource = te.builder().addSPLOperator(
                opNameFromKind(kind), kind, params);
        SourceInfo.setSourceInfo(splSource, SPL.class);
        BOutputPort stream = splSource.addOutput(schema);
       
        return new SPLStreamImpl(te, stream);
    }


    
    public static void addToolkit(TopologyElement te, File toolkitRoot) throws IOException {
        @SuppressWarnings("unchecked")
        Set<String> tks = (Set<String>) te.topology().getConfig().get(TK_DIRS);
        
        if (tks == null) {
            tks = new HashSet<>();
            te.topology().getConfig().put(TK_DIRS, tks);
        }
        tks.add(toolkitRoot.getCanonicalPath());
    }
}
