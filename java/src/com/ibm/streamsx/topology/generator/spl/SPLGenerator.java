/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2015  
 */
package com.ibm.streamsx.topology.generator.spl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.json.java.OrderedJSONObject;
import com.ibm.streamsx.topology.builder.GraphBuilder;

public class SPLGenerator {
	//Needed for composite name generation
	private int numParallelComposites = 0;
	
	//The final list of composites (Main composite and parallel regions), which
	//compose the graph.
	ArrayList<JSONObject> composites = new ArrayList<JSONObject>();


    public String generateSPL(GraphBuilder graph) throws IOException {
        return generateSPL(graph.complete());
    }

    public String generateSPL(JSONObject graph) throws IOException {
    	JSONObject comp = new OrderedJSONObject();
    	comp.put("name", graph.get("name"));
    	comp.put("public", true);    	
    	
    	ArrayList<JSONObject> starts = findStarts(graph);
    	separateIntoComposites(starts, comp, graph);
        StringBuilder sb = new StringBuilder();
        generateGraph(graph, sb);
        return sb.toString();
    }
    
    private ArrayList<JSONObject> findStarts(JSONObject graph) {
    	ArrayList<JSONObject> starts = new ArrayList<JSONObject>();
    	JSONArray ops = (JSONArray) graph.get("operators");
		for(int k = 0; k < ops.size(); k++){
			JSONObject op = (JSONObject)(ops.get(k));
			JSONArray inputs = (JSONArray) op.get("inputs");
			if(inputs == null || inputs.size() == 0){
				if(((JSONObject) ops.get(k)).get("name") != null && 
						!((String) ((JSONObject) ops.get(k)).get("name")).startsWith("$")){
					starts.add((JSONObject) ops.get(k));	
				}
			}
		}
		return starts;
	}

	void generateGraph(JSONObject graph, StringBuilder sb) throws IOException {
    	
        String namespace = (String) graph.get("namespace");
        if (namespace != null && !namespace.isEmpty()) {
            sb.append("namespace ");
            sb.append(namespace);
            sb.append(";\n");
        }
        
        JSONObject graphConfig = getGraphConfig(graph);

        for(int i = 0; i < composites.size(); i++){
        	StringBuilder compBuilder = new StringBuilder();   
        	generateComposite(graphConfig, composites.get(i), compBuilder);
        	sb.append(compBuilder.toString()); 
        }
    }	
    
    void generateComposite(JSONObject graphConfig, JSONObject graph, StringBuilder compBuilder) throws IOException {
    	Boolean isPublic = (Boolean) graph.get("public");
    	String name = (String) graph.get("name");
    	name = getSPLCompatibleName(name);
    	if (isPublic != null && isPublic)
            compBuilder.append("public "); 
    	
    	compBuilder.append("composite ");

    	compBuilder.append(name);
    	if(name.startsWith("__parallel_")){
    		String iput = (String) graph.get("inputName");
    		String oput = (String) graph.get("outputName");
    		
    		iput = splBasename(iput);
    		oput = splBasename(oput);
    		
    		compBuilder.append("(input " + iput + "; output "
    				+ oput + ")");
    	}
    	compBuilder.append("\n{\n");

    	compBuilder.append("graph\n");
        operators(graphConfig, graph, compBuilder);

        compBuilder.append("}\n");  	
    }

    void operators(JSONObject graphConfig, JSONObject graph, StringBuilder sb) throws IOException {
        JSONArray ops = (JSONArray) graph.get("operators");
        
        if (ops == null || ops.isEmpty())
            return;

        for (int i = 0; i < ops.size(); i++) {
            JSONObject op = (JSONObject) ops.get(i);

            String splOp = OperatorGenerator.generate(graphConfig, op);
            sb.append(splOp);
            sb.append("\n");
            
        }
    }

    /**
     * Recursively breaks the graph into different composites, separating the
     * Main composite from the parallel ones. Should work with nested 
     * parallelism, but hasn't yet been tested.
     * @param starts A list of operators that indicate the start of the region.
     * These are either source operators, or operators in a composite that read
     * from the composite's input port.
     * @param comp A JSON object representing a composite with a name field,
     * but not an operator field.
     * @param graph The top-level JSON graph that contains all the operators.
     * Necessary to pass it to the getChildren function.
     */
    JSONObject separateIntoComposites(ArrayList<JSONObject> starts, JSONObject comp, JSONObject graph){
    	// Contains all ops which have been reached by graph traversal,
    	// regardless of whether they are 'special' operators, such as the ones
    	// whose kind begins with '$', or whether they're included in the final
    	// physical graph.
    	HashSet<JSONObject> allTraversedOps = new HashSet<JSONObject>();
    	
    	// Only contains operators that are in the final physical graph.
    	ArrayList<JSONObject> visited = new ArrayList<JSONObject>();
    	
    	// Operators which might not have been visited yet.
    	ArrayList<JSONObject> unvisited = new ArrayList<JSONObject>();
    	JSONObject unparallelOp = null;
    	
    	unvisited.addAll(starts);
    	
    	// While there are still nodes to visit
    	while(unvisited.size() > 0){
    		// Get the first unvisited node
    		JSONObject visitOp = unvisited.get(0);
    		// Check whether we've seen it before. Remember, allTraversedOps
    		// contains *every* operator we've traversed in the JSON graph,
    		// while visited is a list of only the physical operators that will
    		// be included in the graph.
    		if(allTraversedOps.contains(visitOp)){
    			unvisited.remove(0);
    			continue;
    		}
    		// We've now traversed this operator.
    		allTraversedOps.add(visitOp);
    		
    		// If the operator is not a special operator, add it to the 
    		// visited list.
    		if(!isParallelStart(visitOp) && !isParallelEnd(visitOp)){
    			ArrayList<JSONObject> children = getChildren(visitOp, graph);
        		unvisited.addAll(children);
        		visited.add(visitOp);		
    		} 
    		
    		// If the operator is the start of a parallel region, make a new JSON 
    		// operator to insert into the main graph, make a new JSON graph to 
    		// represent the parallel composite, find the parallel region's start
    		// operators, and recursively call this function to populate the new composite.
    		else if(isParallelStart(visitOp)){
    			// The new composite, represented in JSON
    			JSONObject subComp = new OrderedJSONObject();
    			// The operator to include in the graph that refers to the 
    			// parallel composite.
    			JSONObject compOperator = new OrderedJSONObject();
    			subComp.put("name", "__parallel_Composite_" + Integer.toString(numParallelComposites));
    			subComp.put("public", false);
    			
    			compOperator.put("kind", "__parallel_Composite_" + Integer.toString(numParallelComposites));
    			compOperator.put("name", "paraComp_" + Integer.toString(numParallelComposites));
    			compOperator.put("inputs", visitOp.get("inputs"));
    			
    			Boolean partitioned = (Boolean) ((JSONObject)((JSONArray)visitOp.get("outputs"))
    					.get(0)).get("partitioned");
    			if(partitioned != null && partitioned){
    				JSONArray inputs = (JSONArray) visitOp.get("inputs");
    				String parallelInputPortName = null;
    				
    				// Get the first port that has the __spl_hash attribute
    				for(int i = 0; i < inputs.size(); i++){
    					JSONObject input = (JSONObject) inputs.get(i);
    					String type = (String) input.get("type");
    					if(type.contains("__spl_hash")){
    						parallelInputPortName = (String) input.get("name");
    					}
    				}
    				compOperator.put("partitioned", true);
    				compOperator.put("parallelInputPortName", parallelInputPortName);
    			}
    			
    			// Necessary to later indicate whether the composite the operator
    			//refers to is parallelized.
    			compOperator.put("parallelOperator", true);
    			
    			JSONArray outputs = (JSONArray) visitOp.get("outputs");
    			JSONObject output = (JSONObject) outputs.get(0);
    			compOperator.put("width", output.get("width"));
    			numParallelComposites++;
    			
    			// Get the start operators in the parallel region -- the ones
    			// immediately downstream from the $Parallel operator
    			ArrayList<JSONObject> parallelStarts = getChildren(visitOp, graph);
    			
    			// Once you have the start operators, recursively call the function
    			// to populate the parallel composite.
    			JSONObject parallelEnd = separateIntoComposites(parallelStarts, subComp, graph);
    			
    			// The name of the input port of the composite should be the 
    			// name of the input port of the first child operator. For now,
    			// we're assuming that all start operators in a parallel region
    			// all read from the same upstream port.
    			subComp.put("inputName", ((JSONArray) ((JSONObject) ((JSONArray) parallelStarts.get(0).
    					get("inputs")).get(0)).get("connections")).get(0));
    			
    			// If the parallel region ended by invoking unparallel,
    			// the output port of the invocation of the composite operator
    			// should be the same as the output port of the $unparallel
    			// operator
    			if(parallelEnd != null){
    				ArrayList<JSONObject> children = getChildren(parallelEnd, graph);
    				unvisited.addAll(children);
    				compOperator.put("outputs", parallelEnd.get("outputs"));
        			subComp.put("outputName", ((JSONArray) ((JSONObject) ((JSONArray) parallelEnd.
        					get("inputs")).get(0)).get("connections")).get(0));
    			}
    			
    			// Add comp operator to the list of physical operators
    			visited.add(compOperator);
    		}
    	
    		// Is end of parallel region
    		else {
    			unparallelOp = visitOp;
    		}
    		
    		// remove the operator we've traversed from the list of unvisited
    		// operators.
    		unvisited.remove(0);
    	}
    	
		JSONArray compOps = new JSONArray(visited.size());
		compOps.addAll(visited);
		
		comp.put("operators", compOps);
		composites.add(comp);
		
		// If one of the operators in the composite was the $unparallel operator
		// then return that $unparallel operator, otherwise return null.
		return unparallelOp;
    }
    
    private boolean isParallelEnd(JSONObject visitOp) {
		return visitOp.get("kind").equals("$Unparallel$");
	}

	private boolean isParallelStart(JSONObject visitOp) {
		// TODO Auto-generated method stub
		return visitOp.get("kind").equals("$Parallel$");
	}

	// Get the children of the operator. Should probably be re-worked.
	private ArrayList<JSONObject> getChildren(JSONObject visitOp, JSONObject graph) {
		ArrayList<JSONObject> uniqueChildren = new ArrayList<JSONObject>();
		HashSet<JSONObject> children = new HashSet<JSONObject>();
		JSONArray outputs = (JSONArray) visitOp.get("outputs");
		if(outputs == null || outputs.size() == 0){
            return uniqueChildren;
        }

		for(int i = 0; i < outputs.size(); i++){
			JSONArray connections = (JSONArray) ((JSONObject) outputs.get(i)).get("connections");
			for(int j = 0; j < connections.size(); j++){
				String inputPort = (String) connections.get(j);
				//TODO: build index instead of iterating through graph each time
				JSONArray ops = (JSONArray) graph.get("operators");
				for(int k = 0; k < ops.size(); k++){
					JSONArray inputs = (JSONArray) ((JSONObject) ops.get(k)).get("inputs");
					if(inputs != null && inputs.size() != 0){
						for(int l = 0; l < inputs.size(); l++){
							String name = (String) ((JSONObject) inputs.get(l)).get("name");
							if(name.equals(inputPort)){
								children.add((JSONObject) ops.get(k));
							}
						}
					}
				}
			}
		}
		uniqueChildren.addAll(children);
		return uniqueChildren;
	}
	
	private List<JSONObject> getParents(JSONObject visitOp, JSONObject graph){
		List<JSONObject> uniqueParents = new ArrayList<>();
		Set<JSONObject> parents = new HashSet<>();
		JSONArray inputs = (JSONArray) visitOp.get("inputs");
		if(inputs == null || inputs.size() == 0){
            return uniqueParents;
        }
		for(int i = 0; i < inputs.size(); i++){
			JSONArray connections = (JSONArray) ((JSONObject) inputs.get(i)).get("connections");
			for(int j = 0; j < connections.size(); j++){
				String outputPort = (String) connections.get(j);
				//TODO: build index instead of iterating through graph each time
				JSONArray ops = (JSONArray) graph.get("operators");
				for(int k = 0; k < ops.size(); k++){
					JSONArray outputs = (JSONArray) ((JSONObject) ops.get(k)).get("outputs");
					if(outputs != null && outputs.size() != 0){
						for(int l = 0; l < outputs.size(); l++){
							String name = (String) ((JSONObject) outputs.get(l)).get("name");
							if(name.equals(outputPort)){
								parents.add((JSONObject) ops.get(k));
							}
						}
					}
				}
			}
		}
		
		return uniqueParents;
	}

	/**
     * Takes a name String that might have characters which are incompatible in
     * an SPL stream name and maps them to the '_' character.
     * 
     * @param name
     * @return A string which can be a valid SPL stream name.
     */
    static String getSPLCompatibleName(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", "_");
    }

    static String basename(String name) {
        int i = name.lastIndexOf('.');
        if (i == -1)
            return name;

        return name.substring(i + 1);

    }

    static String splBasename(String name) {
        return getSPLCompatibleName(basename(name));
    }
    
    static void stringLiteral(StringBuilder sb, String value) {
        sb.append('"');
        
        // Replace any backslash with an escaped version
        // to stop SPL treating the value as an escape leadin
        value = value.replace("\\", "\\\\");
        
        // Replace new-lines with its SPL escaped version, \n
        // which is \\n as a Java string literal
        value = value.replace("\n", "\\n");
        
        value = value.replace("\"", "\\\"");
        
        sb.append(value);
        sb.append('"');
    }
    
    /**
     * Append the value with the correct SPL suffix. 
     * Integer & Double do not require a suffix.
     */
    static void numberLiteral(StringBuilder sb, Number value) {
        sb.append(value);
 
        if (value instanceof Byte)
            sb.append('b');
        else if (value instanceof Short)
            sb.append('h');
        else if (value instanceof Long)
            sb.append('l');
        else if (value instanceof Float)
            sb.append("w"); // word, meaning 32 bits
        
    }
    
    static JSONObject getGraphConfig(JSONObject graph) {
        JSONObject config = (JSONObject) graph.get("config");
        if (config == null)
            config = new JSONObject();
        return config;
        
    }
}
