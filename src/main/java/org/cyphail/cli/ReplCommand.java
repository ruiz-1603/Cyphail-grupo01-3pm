package org.cyphail.cli;

import org.cyphail.engine.FakePrologEngine;
import org.cyphail.repl.Repl;
import picocli.CommandLine.Command;

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

@Command(name = "repl",
        description = "Starts the Cyphail interactive REPL")

public class ReplCommand implements Runnable {
    private static final FakePrologEngine ENGINE = new FakePrologEngine();

    @Override
    public void run() {
        new Repl(ENGINE).start();
    }
}
