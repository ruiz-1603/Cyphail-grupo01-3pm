package org.cyphail.ast;

public enum BinaryOp {
    LT("<"),
    GT(">"),
    NEQ("<>");
    private final String symbol;

    BinaryOp(String symbol) { this.symbol = symbol; }

    public String symbol() { return symbol; }
}