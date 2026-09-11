package org.cyphail.repl;
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

    inmutable result when execute a ReplCommand
   replaces the variable mutable on Repl loop
 returns a value that the invoker needs to decides what to do
*/

public record CommandOutcome(String message, boolean exit) {
    public static CommandOutcome message(String message) {
        return new CommandOutcome(message, false);
    }

    public static CommandOutcome exit(String message) {
        return new CommandOutcome(message, true);
    }
}