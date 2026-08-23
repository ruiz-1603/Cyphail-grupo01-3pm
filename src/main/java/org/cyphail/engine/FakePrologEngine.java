package org.cyphail.engine;

import org.cyphail.data.FakeGraphData;
import org.cyphail.util.TableFormatter;

public class FakePrologEngine implements Engine {
    private String currentGraph = null;

    public void setCurrentGraph(String graphName) {
        if (FakeGraphData.graphExists(graphName)) {
            this.currentGraph = graphName;
        }
    }

    public String getCurrentGraph() {
        return currentGraph;
    }

    @Override
    public String execute(String command) {
        if (currentGraph == null) {
            return "ERROR: No graph selected. Use .use <graph_name> first.";
        }

        FakeGraphData.GraphInfo graphInfo = FakeGraphData.getGraph(currentGraph);
        String upperCommand = command.toUpperCase().trim();

        // Detectar tipo de query
        if (upperCommand.startsWith("MATCH") && upperCommand.contains("RETURN")) {
            long startTime = System.currentTimeMillis();

            if (upperCommand.contains("AMIGO_DE") || upperCommand.contains("RIVAL") || 
                upperCommand.contains("REQUIRES")) {
                // Query con relaciones
                String table = TableFormatter.formatTable(graphInfo.edges);
                long elapsed = System.currentTimeMillis() - startTime;
                return table + "\nOK. Query resolved after " + elapsed + " ms.\n";
            } else {
                // Query de nodos
                String table = TableFormatter.formatTable(graphInfo.nodes);
                long elapsed = System.currentTimeMillis() - startTime;
                return table + "\nOK. Query resolved after " + elapsed + " ms.\n";
            }
        }

        return "ERROR: Invalid command format.\n";
    }
}