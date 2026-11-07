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

/** SET p.precio = 19.99  ->  SetProperty(PropertyAccess("p", "precio"), DecimalLit(19.99)) */
public record SetProperty(PropertyAccess target, Expr value) implements SetItem {
}