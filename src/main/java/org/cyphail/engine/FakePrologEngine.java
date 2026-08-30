package org.cyphail.engine;

import org.cyphail.data.FakeGraphData;
import org.cyphail.util.TableFormatter;

/*
 * Proyecto Cyphail
 * Grupo 01-3pm
 *
 * Autores:
 * - Priscilla Murillo Romero
 * - Aaron Ruiz Medina
 * - Samael Sanchez Mora
 * - Daniel Villarroel Abaduca
 * - Nicolás Zárate Hernández
 */

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

        // Validate query structure
        if (upperCommand.startsWith("MATCH") && upperCommand.contains("RETURN")) {
            long startTime = System.currentTimeMillis();

            //  Determine query type based on relationship keywords
            if (upperCommand.contains("AMIGO_DE") || upperCommand.contains("RIVAL") || 
                upperCommand.contains("REQUIRES")) {
                // Relationship query: return edges (aristas del grafo)
                String table = TableFormatter.formatTable(graphInfo.edges);
                long elapsed = System.currentTimeMillis() - startTime;
                return table + "\nOK. Query resolved after " + elapsed + " ms.\n";
            } else {
                // Node query: return vertices (vértices del grafo)
                String table = TableFormatter.formatTable(graphInfo.nodes);
                long elapsed = System.currentTimeMillis() - startTime;
                return table + "\nOK. Query resolved after " + elapsed + " ms.\n";
            }
        }

        return "ERROR: Invalid command format.\n";
    }
}