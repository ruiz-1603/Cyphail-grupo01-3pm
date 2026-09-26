package org.cyphail.engine;

import org.cyphail.data.JsonGraphLoader;
import org.cyphail.util.TableFormatter;

import java.io.IOException;


/**
 * Proyecto Cyphail - Grupo 01-3pm
 * Autores:
 * - Priscilla Murillo Romero
 * - Aaron Ruiz Medina
 * - Samael Sanchez Mora
 * - Daniel Villarroel Abaduca
 * - Nicolás Zárate Hernández
 * 
 * Motor fake que ejecuta queries contra datos JSON cargados
 */
public class FakePrologEngine implements Engine {
    private String currentGraph;

    @Override
    public void setCurrentGraph(String graphName) {
        this.currentGraph = graphName;
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
            // Se relee el JSON de disco en cada ejecución para que los
            // cambios hechos durante la defensa (sin recompilar) se vean.
            JsonGraphLoader.GraphData data = JsonGraphLoader.loadGraph(currentGraph);
            long startTime = System.currentTimeMillis();

            // El motor real (Prolog) llega en P2; por ahora el fake engine
            // solo demuestra que los datos vienen de disco.
            String table = TableFormatter.formatTable(data.nodes());

            long elapsed = System.currentTimeMillis() - startTime;
            return table + "\nOK. Query resolved after " + elapsed + " ms.\n";
        } catch (IOException e) {
            return "ERROR: Could not read data for graph '" + currentGraph + "': " + e.getMessage();
        }
    }
}