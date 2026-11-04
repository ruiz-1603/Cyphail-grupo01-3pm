package org.cyphail.util;

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
 * Estatutos del REPL: un estatuto puede terminar con ";" (SPEC P2 II.1).
 * Unico lugar donde se quita ese ";" (lo usan Repl, CommandRegistry y PrologEngine).
 */
public final class Statements {

    private Statements() {}

    /** Quita el ";" final (y los espacios que lo rodean). Si no hay ";" solo quita espacios al final. */
    public static String stripTrailingSemicolon(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.stripTrailing();
        return trimmed.endsWith(";")
                ? trimmed.substring(0, trimmed.length() - 1).stripTrailing()
                : trimmed;
    }
}
