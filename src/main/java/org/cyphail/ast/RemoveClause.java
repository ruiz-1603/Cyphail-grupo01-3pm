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

/** REMOVE item1, item2, ...  p.ej. REMOVE p.discount, p:Oferta */
public record RemoveClause(List<RemoveItem> items) implements UpdateClause {

    public RemoveClause {
        items = List.copyOf(items);
    }
}