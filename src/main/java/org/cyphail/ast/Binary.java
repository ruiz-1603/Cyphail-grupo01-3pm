package org.cyphail.ast;

public record Binary(BinaryOp op, Expr left, Expr right) implements Expr {

}