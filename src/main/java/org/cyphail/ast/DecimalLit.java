package org.cyphail.ast;

/** Numero con punto decimal, p.ej. 19.99. Los enteros siguen siendo NumberLit. */
public record DecimalLit(double value) implements Expr {
}