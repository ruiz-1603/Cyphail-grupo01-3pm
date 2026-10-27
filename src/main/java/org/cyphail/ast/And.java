package org.cyphail.ast;

/** a AND b. Una cadena a AND b AND c se arma asociando a la izquierda: And(And(a, b), c). */
public record And(Expr left, Expr right) implements Expr {
}