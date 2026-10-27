package org.cyphail.engine;

import org.cyphail.data.JsonGraphLoader;
import org.cyphail.util.TableFormatter;

import java.io.IOException;
import java.util.List;

/**
 * Proyecto Cyphail - Grupo 01-3pm
 * Autores:
 * - Priscilla Murillo Romero
 * - Aaron Ruiz Medina
 * - Samael Sanchez Mora
 * - Daniel Villarroel Abaduca
 * - Nicolás Zárate Hernández
 *
 * Motor fake (Sprint P1) que ejecuta queries contra datos JSON cargados.
 */
public class FakePrologEngine implements Engine {
    private String currentGraph;

    @Override
    public List<String> listAvailableGraphs() {
        try {
            return JsonGraphLoader.listAvailableGraphs();
        } catch (IOException e) {
            throw new IllegalStateException("Could not read data directory: " + e.getMessage(), e);
        }
    }

    @Override
    public void setCurrentGraph(String graphName) {
        try {
            JsonGraphLoader.loadGraph(graphName);
            this.currentGraph = graphName;
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override
    public String getCurrentGraph() {
        return currentGraph;
    }

    @Override
    public String execute(String query) {
        if (currentGraph == null) {
            return "ERROR: No graph selected. Use .use <graph_name> first.";
        }

        try {
            JsonGraphLoader.GraphData data = JsonGraphLoader.loadGraph(currentGraph);
            long startTime = System.currentTimeMillis();
            String table = TableFormatter.formatTable(data.nodes());
            long elapsed = System.currentTimeMillis() - startTime;
            return table + "\nOK. Query resolved after " + elapsed + " ms.\n";
        } catch (IOException e) {
            return "ERROR: Could not read data for graph '" + currentGraph + "': " + e.getMessage();
        }
    }
}
