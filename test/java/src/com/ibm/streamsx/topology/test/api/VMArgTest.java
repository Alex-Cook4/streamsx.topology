/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2015  
 */
package com.ibm.streamsx.topology.test.api;

import static org.junit.Assume.assumeTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assume;
import org.junit.Test;

import com.ibm.streamsx.topology.TStream;
import com.ibm.streamsx.topology.Topology;
import com.ibm.streamsx.topology.context.ContextProperties;
import com.ibm.streamsx.topology.context.StreamsContext;
import com.ibm.streamsx.topology.context.StreamsContext.Type;
import com.ibm.streamsx.topology.context.StreamsContextFactory;
import com.ibm.streamsx.topology.function7.Supplier;
import com.ibm.streamsx.topology.test.TestTopology;

public class VMArgTest extends TestTopology {

    @Test
    public void testSettingSystemProperty() throws Exception {
        
        assumeTrue(getTesterType() != StreamsContext.Type.EMBEDDED_TESTER);
        
        final Topology topology = new Topology("testSettingSystemProperty");
        
        final String propertyName = "tester.property.921421";
        final String propertyValue = "abcdef832124";
        final String vmArg = "-D" + propertyName + "=" + propertyValue;
        
        TStream<String> source = topology.limitedSource(new ReadProperty(propertyName), 1, String.class);

        final Map<String,Object> config = getConfig();
        List<String> vmArgs = (List<String>) config.get(ContextProperties.VMARGS);
        vmArgs.add(vmArg);
        
        // StreamsContextFactory.getStreamsContext(Type.TOOLKIT).submit(topology, config);
        
        // config.put(ContextProperties.KEEP_ARTIFACTS, Boolean.TRUE);
        completeAndValidate(config, source, 10, propertyValue);
    }
    
    public static class ReadProperty implements Supplier<String> {

        private final String property;
        ReadProperty(String property) {
           this.property = property;
        }
        @Override
        public String get() {
            return System.getProperty(property);
        }       
    }
}
