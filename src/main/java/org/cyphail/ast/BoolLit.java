package org.cyphail.ast;

/** Literal booleano TRUE / FALSE (gramatica: booleanLiteral), p.ej. {activo: true}. */
public record BoolLit(boolean value) implements Expr {
}
