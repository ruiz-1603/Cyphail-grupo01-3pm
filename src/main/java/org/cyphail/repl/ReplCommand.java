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

command pattern, every .command is a lambda that
return its implementation, creates it and bind to the receiver

for future new commands add a single lambda in CommandRegistry class
*/
@FunctionalInterface
public interface ReplCommand {
    CommandOutcome execute(String argument);
}
