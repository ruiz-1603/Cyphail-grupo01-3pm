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

/** a AND b. Una cadena a AND b AND c se arma asociando a la izquierda: And(And(a, b), c). */
public record And(Expr left, Expr right) implements Expr {
}