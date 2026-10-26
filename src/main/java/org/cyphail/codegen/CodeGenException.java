package org.cyphail.codegen;

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
 * Error al transpilar. Normalmente el validador semantico lo detecta antes;
 * el CodeGen lo lanza si recibe un AST que no puede traducir.
 */
public class CodeGenException extends RuntimeException {

    public CodeGenException(String message) {
        super(message);
    }

    static CodeGenException undefinedVariable(String name) {
        return new CodeGenException(String.format("Variable '%s' is not defined", name));
    }
}
