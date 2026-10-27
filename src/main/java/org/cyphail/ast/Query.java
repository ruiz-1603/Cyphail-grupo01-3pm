package org.cyphail.ast;

import java.util.List;
import java.util.Optional;

/**
 * Raiz del AST. Segun la gramatica: readingClause* updatingClause* returnClause?
 *
 * @param match        MATCH; vacio en un CREATE sin MATCH previo
 * @param where        WHERE del MATCH; solo puede estar si hay MATCH
 * @param updates      CREATE, SET, REMOVE y DELETE en el orden escrito
 * @param returnClause RETURN; vacio en p.ej. MATCH ... DELETE r
 */
public record Query(Optional<MatchClause> match, Optional<WhereClause> where,
                    List<UpdateClause> updates, Optional<ReturnClause> returnClause) {

    public Query {
        updates = List.copyOf(updates);
    }

    /** Query con MATCH y RETURN. Es el constructor que ya usaba el parser de P1. */
    public Query(MatchClause match, Optional<WhereClause> where,
                 List<UpdateClause> updates, ReturnClause returnClause) {
        this(Optional.of(match), where, updates, Optional.of(returnClause));
    }
}