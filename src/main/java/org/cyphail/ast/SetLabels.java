package org.cyphail.ast;

import java.util.List;

/** SET p:Oferta:Nuevo  ->  SetLabels("p", ["Oferta", "Nuevo"]) */
public record SetLabels(String variable, List<String> labels) implements SetItem {

    public SetLabels {
        labels = List.copyOf(labels);
    }
}