package org.cyphail.validator;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tabla de simbolos del analisis semantico (inmutable). La produce VariableValidator.analyze.
 *
 *   variables: a -> NODE, r -> RELATIONSHIP   (en el orden en que se declararon)
 *   columns:   nombres de las columnas del RETURN, en orden (alias si hay, si no el texto: a.name, a)
 *
 * Correspondencia con Prolog (la arma el CodeGen, ver codegen.SymbolTable y PrologSyntax.variable):
 *   variable a (nodo o relacion) -> __A, que es el ID del nodo o relacion
 *   a.name                       -> __A_name
 *   RETURN a                     -> devuelve solo el ID (__A), restriccion P2
 * Por eso ninguna variable Cyphail puede empezar con "__".
 *
 * Cada "with" devuelve una tabla nueva; la original no cambia.
 */
public record VariableTable(Map<String, VarKind> variables, List<String> columns) {

    public VariableTable {
        variables = Collections.unmodifiableMap(new LinkedHashMap<>(variables));
        columns = List.copyOf(columns);
    }

    public static VariableTable empty() {
        return new VariableTable(Map.of(), List.of());
    }

    public Optional<VarKind> kindOf(String variable) {
        return Optional.ofNullable(variables.get(variable));
    }

    public boolean isDeclared(String variable) {
        return variables.containsKey(variable);
    }

    /** Variables de un tipo, en el orden en que se declararon. */
    public List<String> namesOf(VarKind kind) {
        return variables.entrySet().stream()
                .filter(e -> e.getValue() == kind)
                .map(Map.Entry::getKey)
                .toList();
    }

    /** Declara la variable. Si ya existe con el mismo tipo devuelve la misma tabla. */
    public VariableTable withVariable(String variable, VarKind kind) {
        if (kind == variables.get(variable)) return this;
        var copy = new LinkedHashMap<>(variables);
        copy.put(variable, kind);
        return new VariableTable(copy, columns);
    }

    public VariableTable withColumns(List<String> newColumns) {
        return new VariableTable(variables, newColumns);
    }
}