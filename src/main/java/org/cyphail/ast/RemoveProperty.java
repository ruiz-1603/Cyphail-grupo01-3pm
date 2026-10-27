package org.cyphail.ast;

/** REMOVE p.discount  ->  RemoveProperty(PropertyAccess("p", "discount")) */
public record RemoveProperty(PropertyAccess target) implements RemoveItem {
}