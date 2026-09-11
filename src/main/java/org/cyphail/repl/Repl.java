package org.cyphail.repl;

import org.cyphail.engine.Engine;
import org.cyphail.util.IO;

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

// single responsability
// invoker of the command pattern, only reads console lines, doesn't know about intern logic
public class Repl {
    private final Map<String, ReplCommand> commands;
    private final QueryHandler queryHandler;

    public Repl(Engine engine) {
        this.commands = CommandRegistry.forEngine(engine);
        this.queryHandler = new QueryHandler(engine);
    }

    public void start() {
        IO.println(ReplMessages.welcome());
        replLoop();
    }

    private void replLoop() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        while (running) {
            IO.print("cyphail> ");
            String line = scanner.nextLine().trim();
            if (line.isBlank()) continue;
            if (line.startsWith(".")) {
                running = !handleCommand(line);
            } else {
                queryHandler.run(line);
            }
        }
    }


    boolean handleCommand(String line) {
        String[] parts = line.split("\\s+", 2);
        String name = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1] : null;

        ReplCommand command = commands.getOrDefault(
                name, unused -> CommandOutcome.message("Unknown command: " + name));
        CommandOutcome outcome = command.execute(arg);

        IO.println(outcome.message());
        return outcome.exit();
    }
}