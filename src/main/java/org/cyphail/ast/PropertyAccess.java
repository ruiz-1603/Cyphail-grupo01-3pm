package org.cyphail.ast;

public record PropertyAccess(String variable, String property) implements Expr {

}