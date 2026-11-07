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

public enum BinaryOp {
    LT("<"),
    GT(">"),
    NEQ("<>"),
    EQ("="),
    LTE("<="),
    GTE(">=");
    private final String symbol;

    BinaryOp(String symbol) { this.symbol = symbol; }

    public String symbol() { return symbol; }
}