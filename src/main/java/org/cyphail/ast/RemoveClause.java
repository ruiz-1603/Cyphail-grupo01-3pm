package org.cyphail.ast;

import java.util.List;

/** REMOVE item1, item2, ...  p.ej. REMOVE p.discount, p:Oferta */
public record RemoveClause(List<RemoveItem> items) implements UpdateClause {

    public RemoveClause {
        items = List.copyOf(items);
    }
}