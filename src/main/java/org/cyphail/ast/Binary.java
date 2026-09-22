package org.cyphail.ast;

record Binary(BinaryOp op, Expr left, Expr right) implements Expr {

}