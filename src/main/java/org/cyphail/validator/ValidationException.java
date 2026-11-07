package org.cyphail.validator;

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

/** Error de semantica estatica. El mensaje es el que se le muestra al usuario. */
public final class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}