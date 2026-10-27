package org.cyphail.ast;

import java.util.List;

/**
 * CREATE patron1, patron2, ...
 * Se acepta List<PatternNode> (P1) o List<PatternElement> (P2), por eso el comodin.
 */
public record CreateClause(List<? extends PatternElement> patterns) implements UpdateClause {

    public CreateClause {
        patterns = List.copyOf(patterns);
    }
}