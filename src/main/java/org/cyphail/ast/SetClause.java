package org.cyphail.ast;

import java.util.List;

/** SET item1, item2, ...  p.ej. SET p.precio = 19.99, p:Oferta */
public record SetClause(List<SetItem> items) implements UpdateClause {

    public SetClause {
        items = List.copyOf(items);
    }
}