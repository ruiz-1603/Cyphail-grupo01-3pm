package org.cyphail.validator;

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

/** Que representa una variable del query. */
public enum VarKind {
    /** Variable de nodo: (a) */
    NODE,
    /** Variable de relacion: -[r]-> */
    RELATIONSHIP
}