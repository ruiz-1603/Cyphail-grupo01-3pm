package org.cyphail.ast;

import java.util.List;
import java.util.Optional;

public record Query(MatchClause match, Optional<WhereClause> where,
                    List<UpdateClause> updates, ReturnClause returnClause) {}