package org.cyphail.engine;

import java.util.List;

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

public interface Engine extends AutoCloseable {
    String execute(String command);
    void setCurrentGraph(String graphName);
    String getCurrentGraph();

    default List<String> listAvailableGraphs() {
        return List.of();
    }

    @Override
    default void close() {}
}
