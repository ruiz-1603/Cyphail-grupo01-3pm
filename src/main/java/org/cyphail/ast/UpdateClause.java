package org.cyphail.ast;

sealed interface UpdateClause permits CreateClause, DeleteClause {

}