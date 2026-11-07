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

import java.util.List;

/**
 * MATCH patron1, patron2, ...
 * Se acepta List<PatternNode> (P1) o List<PatternElement> (P2), por eso el comodin.
 */
public record MatchClause(List<? extends PatternElement> patterns) {

	public MatchClause {
		patterns = List.copyOf(patterns);
	}
}