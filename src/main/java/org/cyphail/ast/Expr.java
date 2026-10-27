package org.cyphail.ast;

sealed public interface Expr
        permits Binary, And, Or, Not, PropertyAccess, Var, NumberLit, DecimalLit, StringLit {

}