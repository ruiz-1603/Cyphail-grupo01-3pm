package org.cyphail.cli;

import org.cyphail.util.IO;
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

@Command(name = "cyphail",
        description = "Cyphail graph query prototype",
        mixinStandardHelpOptions = true,
        subcommands = {ReplCommand.class})

public class CyphailCommand implements Runnable {
    @Override
    public void run() {
        IO.println("Use --help to see available commands.");
    }
}
