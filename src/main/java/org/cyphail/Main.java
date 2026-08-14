package org.cyphail;

import org.cyphail.cli.CyphailCommand;
import picocli.CommandLine;

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

public class Main {
    static void main(String[] args) {
        //  IO.println("Cyphail");
        int exitCode = new CommandLine(new CyphailCommand()).execute(args);
        System.exit(exitCode);
    }
}