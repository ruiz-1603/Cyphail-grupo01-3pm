package org.cyphail.ast;

/** NOT a */
public record Not(Expr operand) implements Expr {
}