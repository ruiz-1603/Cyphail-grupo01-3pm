package org.cyphail.codegen;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/*
 * Proyecto Cyphail
 * Grupo 01-3pm
 *
 * Autores:
 * - Priscilla Murillo Romero
 * - Aaron Ruiz Medina
 * - Samael Sanchez Mora
 * - Daniel Villarroel Abaduca
 * - Nicolás Zárate Hernández
 */

/**
 * Tabla de simbolos del CodeGen (inmutable): asocia lo que aparece en el query
 * Cyphail con las variables del codigo Prolog generado.
 *
 *   variables:  a       -> __A       (id del nodo o relacion)
 *   properties: a.name  -> __A_name  (calculo: valor de la propiedad)
 *   used:       todos los nombres Prolog ya ocupados (incluye los reservados)
 *
 * Cada "with" devuelve una tabla nueva; la original no cambia.
 */
public record SymbolTable(Map<String, String> variables,
                          Map<String, String> properties,
                          Set<String> used) {

    /** Nombres que usa el goal externo (ver GeneratedQuery.goal()); ninguna variable del query puede tomarlos. */
    public static final Set<String> RESERVED = Set.of(GeneratedQuery.QUERY_ID, GeneratedQuery.ROWS);

    public SymbolTable {
        // Copias defensivas que conservan el orden de insercion
        variables = Collections.unmodifiableMap(new LinkedHashMap<>(variables));
        properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
        used = Collections.unmodifiableSet(new LinkedHashSet<>(used));
    }

    public static SymbolTable empty() {
        return new SymbolTable(Map.of(), Map.of(), RESERVED);
    }

    public Optional<String> variable(String name) {
        return Optional.ofNullable(variables.get(name));
    }

    public Optional<String> property(String variable, String property) {
        return Optional.ofNullable(properties.get(propertyKey(variable, property)));
    }

    /** Registra la variable Cyphail si no existe. Si ya existe devuelve la misma tabla. */
    public SymbolTable withVariable(String name) {
        if (variables.containsKey(name)) return this;
        var prologName = fresh(PrologSyntax.variable(name));
        return new SymbolTable(put(variables, name, prologName), properties, add(used, prologName));
    }

    /** Registra el calculo variable.property; la variable ya debe estar en la tabla. */
    public SymbolTable withProperty(String variable, String property) {
        var key = propertyKey(variable, property);
        if (properties.containsKey(key)) return this;
        var owner = variable(variable).orElseThrow(() -> CodeGenException.undefinedVariable(variable));
        var prologName = fresh(owner + "_" + property);
        return new SymbolTable(variables, put(properties, key, prologName), add(used, prologName));
    }

    // Si 'base' ya esta ocupado (p.ej. 'a' y 'A' dan ambos __A) se agrega _2, _3, ...
    private String fresh(String base) {
        if (!used.contains(base)) return base;
        int n = 2;
        while (used.contains(base + "_" + n)) n++;
        return base + "_" + n;
    }

    private static String propertyKey(String variable, String property) {
        return variable + "." + property;
    }

    private static Map<String, String> put(Map<String, String> map, String key, String value) {
        var copy = new LinkedHashMap<>(map);
        copy.put(key, value);
        return copy;
    }

    private static Set<String> add(Set<String> set, String value) {
        var copy = new LinkedHashSet<>(set);
        copy.add(value);
        return copy;
    }
}
