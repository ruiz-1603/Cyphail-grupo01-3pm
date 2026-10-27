package org.cyphail.ast;

import java.util.List;

/**
 * [DETACH] DELETE a, r, ...
 *
 * @param detach    true si se escribio DETACH DELETE
 * @param variables variables a borrar (nodos o relaciones)
 */
public record DeleteClause(boolean detach, List<String> variables) implements UpdateClause {

    public DeleteClause {
        variables = List.copyOf(variables);
    }

    /** DELETE sin DETACH. Es el constructor que ya usaba el parser de P1. */
    public DeleteClause(List<String> variables) {
        this(false, variables);
    }
}