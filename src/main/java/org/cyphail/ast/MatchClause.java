package org.cyphail.ast;

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