package org.cyphail.repl;

import org.cyphail.engine.Engine;
import org.cyphail.util.IO;

import static org.cyphail.util.Statements.stripTrailingSemicolon;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
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

// Invoker of the command pattern; reads console lines (including multiline queries; the ';' is optional)
public class Repl {
    private static final String PROMPT = "cyphail> ";
    private static final String CONTINUATION_PROMPT = "   ...> ";

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
            replLoop(scanner, Optional.empty());
        } finally {
            engine.close();
        }
    }

    /** A statement read from the console; pendingLine is the dot-command line that ended it (still to run). */
    record ReadStatement(String text, Optional<String> pendingLine) { }

    /** Result of processing one line: whether to exit and the dot-command line left pending, if any. */
    private record Step(boolean exit, Optional<String> pendingLine) { }

    /** Reads a statement (see {@link #readStatement}) and returns only its text, without the trailing ';'. */
    public static String readQuery(Scanner scanner, String accumulated) {
        return readStatement(scanner, accumulated).text();
    }

    /**
     * Reads a single-line or multi-line statement recursively. The ';' is optional (Guia de Revision P2):
     * the statement ends with a trailing ';', a blank line, EOF, or a line that starts a dot-command
     * (that line is returned as pending so it still runs). Strips the trailing ';' before returning.
     */
    static ReadStatement readStatement(Scanner scanner, String accumulated) {
        String current = accumulated == null ? "" : accumulated;
        if (current.stripTrailing().endsWith(";")) {
            return new ReadStatement(stripTrailingSemicolon(current), Optional.empty());
        }
        if (!current.isBlank()) {
            IO.print(CONTINUATION_PROMPT);
        }
        if (!scanner.hasNextLine()) {
            return new ReadStatement(stripTrailingSemicolon(current), Optional.empty());
        }
        String nextLine = scanner.nextLine();
        if (!current.isBlank() && nextLine.isBlank()) {
            return new ReadStatement(stripTrailingSemicolon(current), Optional.empty());
        }
        if (!current.isBlank() && nextLine.trim().startsWith(".")) {
            return new ReadStatement(stripTrailingSemicolon(current), Optional.of(nextLine.trim()));
        }
        String combined = current.isBlank()
                ? nextLine.trim()
                : current + "\n" + nextLine;
        return readStatement(scanner, combined);
    }

    // El prompt se imprime antes de esperar la linea (hasNextLine bloquea hasta que el usuario escribe)
    private void replLoop(Scanner scanner, Optional<String> pendingLine) {
        if (pendingLine.isEmpty()) {
            IO.print(PROMPT);
        }
        Optional<String> line = pendingLine.or(() -> scanner.hasNextLine()
                ? Optional.of(scanner.nextLine())
                : Optional.empty());
        line.map(String::trim)
                .map(text -> processLine(scanner, text))
                .filter(step -> !step.exit())
                .ifPresent(step -> replLoop(scanner, step.pendingLine()));
    }

    private Step processLine(Scanner scanner, String line) {
        if (line.isBlank()) {
            return new Step(false, Optional.empty());
        }
        if (line.equalsIgnoreCase(".tree") || line.toLowerCase().startsWith(".tree ")) {
            String initial = line.length() > 5 ? line.substring(5).trim() : "";
            ReadStatement query = readStatement(scanner, initial);
            return new Step(handleCommand(".tree " + query.text()), query.pendingLine());
        }
        if (line.startsWith(".")) {
            return new Step(handleCommand(stripTrailingSemicolon(line)), Optional.empty());
        }
        ReadStatement query = readStatement(scanner, line);
        queryHandler.run(query.text());
        return new Step(false, query.pendingLine());
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
