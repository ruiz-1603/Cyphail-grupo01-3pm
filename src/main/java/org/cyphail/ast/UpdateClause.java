package org.cyphail.ast;

/** Clausulas que modifican el grafo, en el orden en que se escriben. */
public sealed interface UpdateClause permits CreateClause, SetClause, RemoveClause, DeleteClause {
}