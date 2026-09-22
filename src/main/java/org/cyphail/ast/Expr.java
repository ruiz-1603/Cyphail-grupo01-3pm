package org.cyphail.ast;

sealed public interface Expr permits Binary, PropertyAccess, Var, NumberLit, StringLit {

}