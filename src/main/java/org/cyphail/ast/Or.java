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

/** a OR b. Una cadena a OR b OR c se arma asociando a la izquierda: Or(Or(a, b), c). */
public record Or(Expr left, Expr right) implements Expr {
}