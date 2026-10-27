package org.cyphail.ast;

/** SET p.precio = 19.99  ->  SetProperty(PropertyAccess("p", "precio"), DecimalLit(19.99)) */
public record SetProperty(PropertyAccess target, Expr value) implements SetItem {
}