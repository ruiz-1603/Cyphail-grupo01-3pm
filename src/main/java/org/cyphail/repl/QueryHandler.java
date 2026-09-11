package org.cyphail.repl;
import org.cyphail.engine.Engine;
import org.cyphail.util.IO;
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

isolation from dot commands
this class is used only for querys
*/

final class QueryHandler {
    private final Engine engine;

    QueryHandler(Engine engine) {
        this.engine = engine;
    }

    void run(String query) {
        if (engine.getCurrentGraph() == null) {
            IO.println("ERROR: No graph selected. Use .use <graph_name> first.");
            return;
        }
        IO.println();
        IO.println(engine.execute(query));
    }
}