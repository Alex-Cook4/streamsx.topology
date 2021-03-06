/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2015  
 */
package com.ibm.streamsx.topology.builder;

import java.util.HashSet;
import java.util.Set;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

/**
 * JSON representation.
 * 
 * Operator:
 * 
 * params: Object of Parameters, keyed by parameter name. outputs: Array of
 * output ports (see BStream) inputs: Array in input ports type: connections:
 * array of connection names as strings
 * 
 * Parameter value: Value of parameter.
 */

public class BOperator extends BJSONObject {

    private final GraphBuilder bt;

    private Set<String> regions;

    public BOperator(GraphBuilder bt) {
        this.bt = bt;
    }

    public GraphBuilder builder() {
        return bt;
    }

    public boolean addRegion(String name) {
        if (regions == null)
            regions = new HashSet<>();

        return regions.add(name);
    }

    public boolean copyRegions(BOperator otherOp) {
        if (otherOp.regions == null)
            return false;

        if (regions == null)
            regions = new HashSet<>();

        return regions.addAll(otherOp.regions);
    }
    
    /**
     * Add a configuration value to this operator.
     * The value must be a value acceptable to JSON
     * and is put into the "config" object of this operator.
     * @param key
     * @param value
     */
    public void addConfig(String key, Object value) {
        
        JSONObject config = (JSONObject) json().get("config");
        if (config == null)
            json().put("config", config = new JSONObject());
        
        config.put(key, value);     
    }

    @Override
    public JSONObject complete() {
        JSONObject json = super.complete();
        if (regions != null) {
            JSONArray ra = new JSONArray();
            ra.addAll(regions);
            json.put("regions", ra);
        }
        return json;
    }
}
