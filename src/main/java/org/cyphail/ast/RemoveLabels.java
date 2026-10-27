package org.cyphail.ast;

import java.util.List;

/** REMOVE p:Oferta  ->  RemoveLabels("p", ["Oferta"]) */
public record RemoveLabels(String variable, List<String> labels) implements RemoveItem {

    public RemoveLabels {
        labels = List.copyOf(labels);
    }
}