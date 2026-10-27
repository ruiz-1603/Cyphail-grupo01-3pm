package org.cyphail.repl;

import org.cyphail.engine.Engine;
import org.cyphail.util.IO;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;

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

// Invoker of the command pattern; reads console lines (including multiline queries ending with ';')
public class Repl {
    private final Engine engine;
    private final Map<String, ReplCommand> commands;
    private final QueryHandler queryHandler;

    public Repl(Engine engine) {
        this.engine = engine;
        this.commands = CommandRegistry.forEngine(engine);
        this.queryHandler = new QueryHandler(engine);
    }

    public void start() {
        start(System.in);
    }

    public void start(InputStream input) {
        IO.println(ReplMessages.welcome());
        try (Scanner scanner = new Scanner(input, StandardCharsets.UTF_8)) {
            replLoop(scanner);
        } finally {
            engine.close();
        }
    }

    /**
     * Reads a single-line or multi-line statement recursively until a trailing ';' is found
     * (or until a blank line / EOF is reached). Strips the trailing ';' before returning.
     */
    public static String readQuery(Scanner scanner, String accumulated) {
        String current = accumulated == null ? "" : accumulated;
        if (current.stripTrailing().endsWith(";")) {
            return stripTrailingSemicolon(current);
        }
        if (!scanner.hasNextLine()) {
            return stripTrailingSemicolon(current);
        }
        String nextLine = scanner.nextLine();
        if (nextLine.isBlank() && !current.isBlank()) {
            return stripTrailingSemicolon(current);
        }
        String combined = current.isBlank()
                ? nextLine.trim()
                : current + "\n" + nextLine;
        return readQuery(scanner, combined);
    }

    public static String stripTrailingSemicolon(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.stripTrailing();
        return trimmed.endsWith(";")
                ? trimmed.substring(0, trimmed.length() - 1).stripTrailing()
                : trimmed;
    }

    private void replLoop(Scanner scanner) {
        if (!scanner.hasNextLine()) {
            return;
        }
        IO.print("cyphail> ");
        String line = scanner.nextLine().trim();

        boolean shouldExit = processLine(scanner, line);
        if (!shouldExit) {
            replLoop(scanner);
        }
    }

    private boolean processLine(Scanner scanner, String line) {
        if (line.isBlank()) {
            return false;
        }
        if (line.equalsIgnoreCase(".tree") || line.toLowerCase().startsWith(".tree ")) {
            String initial = line.length() > 5 ? line.substring(5).trim() : "";
            String query = readQuery(scanner, initial);
            return handleCommand(".tree " + query);
        }
        if (line.startsWith(".")) {
            return handleCommand(stripTrailingSemicolon(line));
        }
        String query = readQuery(scanner, line);
        queryHandler.run(query);
        return false;
    }

    public boolean handleCommand(String line) {
        String[] parts = line.trim().split("\\s+", 2);
        String name = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1].trim() : null;

        ReplCommand command = commands.getOrDefault(
                name, unused -> CommandOutcome.message("Unknown command: " + name));
        CommandOutcome outcome = command.execute(arg);

        IO.println(outcome.message());
        return outcome.exit();
    }
}
