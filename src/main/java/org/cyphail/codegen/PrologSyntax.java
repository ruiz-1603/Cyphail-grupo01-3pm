package org.cyphail.codegen;

import java.util.List;
import java.util.regex.Pattern;

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
 * Funciones puras para escribir texto Prolog (target del transpilador).
 * No guardan estado: reciben strings y devuelven strings.
 */
public final class PrologSyntax {

    private PrologSyntax() {}

    // Un atomo que empieza en minuscula y solo tiene letras, digitos o '_' no necesita comillas
    private static final Pattern PLAIN_ATOM = Pattern.compile("[a-z][a-zA-Z0-9_]*");

    /**
     * Nombre Prolog de una variable Cyphail: prefijo "__" y primera letra en
     * mayuscula (a -> __A, persona -> __Persona). El SPEC P2 prohibe que una
     * variable Cyphail empiece con "__", asi no choca con variables del usuario.
     */
    public static String variable(String cyphailName) {
        return "__" + Character.toUpperCase(cyphailName.charAt(0)) + cyphailName.substring(1);
    }

    /** Atomo Prolog: tal cual si es simple ('amigo'), entre comillas simples si no ('Ana', 'a.name'). */
    public static String atom(String text) {
        if (PLAIN_ATOM.matcher(text).matches()) return text;
        var escaped = text.replace("\\", "\\\\").replace("'", "\\'");
        return "'" + escaped + "'";
    }

    /** Termino compuesto: functor(arg1, arg2, ...). */
    public static String call(String functor, String... args) {
        return atom(functor) + "(" + String.join(", ", args) + ")";
    }

    /** Lista Prolog: [x, y, z]. */
    public static String list(List<String> elements) {
        return "[" + String.join(", ", elements) + "]";
    }

    /** Predicado de datos del grafo G: user_G_suffix (ver prolog/cy_engine.pl). */
    public static String graphPredicate(String graph, String suffix) {
        return "user_" + graph + "_" + suffix;
    }
}
