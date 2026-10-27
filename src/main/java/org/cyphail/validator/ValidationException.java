package org.cyphail.validator;

/** Error de semantica estatica. El mensaje es el que se le muestra al usuario. */
public final class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}