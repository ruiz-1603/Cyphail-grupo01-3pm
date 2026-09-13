package org.cyphail.parser.core;

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
 *
FACTORY PATTERN

I = rest input type (the next parser needs to know where to continue)
T = token type(change depending the pipeline)
R = error reason type

*/

@FunctionalInterface
public interface Parser<I, T, R> {

    Result<I, T, R> parse(I input);
}