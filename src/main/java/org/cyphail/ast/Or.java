package org.cyphail.ast;

/** a OR b. Una cadena a OR b OR c se arma asociando a la izquierda: Or(Or(a, b), c). */
public record Or(Expr left, Expr right) implements Expr {
}