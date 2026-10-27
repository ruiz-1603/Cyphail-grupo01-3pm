package org.cyphail.ast;

import java.util.List;
import java.util.Optional;

/**
 * Nodo de un patron: (variable:Label1:Label2 {clave: valor}).
 *
 * @param variable   nombre de la variable; vacio si el nodo es anonimo, p.ej. (:Persona)
 * @param labels     etiquetas en el orden escrito; puede ser vacia, p.ej. (e)
 * @param properties propiedades inline en el orden escrito
 */
public record PatternNode(Optional<String> variable, List<String> labels, List<PropertyEntry> properties)
        implements PatternElement {

    public PatternNode {
        labels = List.copyOf(labels);
        properties = List.copyOf(properties);
    }

    /** Nodo con variable. Es el constructor que ya usaba el parser de P1. */
    public PatternNode(String variable, List<String> labels, List<PropertyEntry> properties) {
        this(Optional.of(variable), labels, properties);
    }
}