package org.cyphail.ast;

public sealed interface UpdateClause permits CreateClause, DeleteClause {
}