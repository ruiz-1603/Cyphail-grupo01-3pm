package org.cyphail.ast;

import java.util.List;
import java.util.Optional;

/**
 * Relacion de un patron: -[variable:TIPO1|TIPO2 {clave: valor}]-> (o <-[...]- o -[...]-).
 *
 * @param variable   nombre de la variable; vacio si es anonima, p.ej. -[:TRABAJA_EN]->
 * @param types      tipos permitidos (separados por | en el query); puede ser vacia, p.ej. -[r]->
 * @param properties propiedades inline de la relacion
 * @param direction  OUT, IN o NONE
 */
public record RelationshipPattern(Optional<String> variable, List<String> types,
                                  List<PropertyEntry> properties, Direction direction) {

    public RelationshipPattern {
        types = List.copyOf(types);
        properties = List.copyOf(properties);
    }
}