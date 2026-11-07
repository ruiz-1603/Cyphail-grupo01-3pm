package org.cyphail.ast;

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

import java.util.List;

/** REMOVE p:Oferta  ->  RemoveLabels("p", ["Oferta"]) */
public record RemoveLabels(String variable, List<String> labels) implements RemoveItem {

    public RemoveLabels {
        labels = List.copyOf(labels);
    }
}