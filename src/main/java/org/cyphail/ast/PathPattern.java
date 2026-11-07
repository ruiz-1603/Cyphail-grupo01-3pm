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
 * Un salto: nodo, relacion, nodo. La gramatica de P2 permite un solo salto por elemento.
 *
 *   (p:Persona)-[:TRABAJA_EN]->(e:Empresa)
 *   -> PathPattern(left = p, relationship = TRABAJA_EN con OUT, right = e)
 *
 * La direccion de la relacion se lee de left hacia right.
 */
public record PathPattern(PatternNode left, RelationshipPattern relationship, PatternNode right)
        implements PatternElement {
}