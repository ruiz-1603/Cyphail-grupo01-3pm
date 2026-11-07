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

/**
 * Un elemento de un patron (lo que va separado por comas en MATCH o CREATE).
 * Segun la gramatica: patternElement : nodePattern (relationshipPattern nodePattern)?
 *
 *   (a:Person)                    -> PatternNode
 *   (a:Person)-[r:KNOWS]->(b)     -> PathPattern
 */
public sealed interface PatternElement permits PatternNode, PathPattern {
}