package org.cyphail.ast;

/**
 * Un elemento de un patron (lo que va separado por comas en MATCH o CREATE).
 * Segun la gramatica: patternElement : nodePattern (relationshipPattern nodePattern)?
 *
 *   (a:Person)                    -> PatternNode
 *   (a:Person)-[r:KNOWS]->(b)     -> PathPattern
 */
public sealed interface PatternElement permits PatternNode, PathPattern {
}