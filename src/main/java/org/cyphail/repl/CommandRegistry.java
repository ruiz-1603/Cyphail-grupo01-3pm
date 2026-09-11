package org.cyphail.repl;

import org.cyphail.data.FakeGraphData;
import org.cyphail.engine.Engine;
import org.cyphail.util.TableFormatter;

import java.util.LinkedHashMap;
import java.util.Map;

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

final client from command pattern, defines the types of commands
 and connect them to its receiver using lambdas
 *
 */
public final class CommandRegistry {

    private CommandRegistry() {}

    public static Map<String, ReplCommand> forEngine(Engine engine) { // here to add new commands (message: print)
        Map<String, ReplCommand> registry = new LinkedHashMap<>();
        registry.put(".exit", arg -> CommandOutcome.exit("Thanks for using Cyphail!"));
        registry.put(".help", arg -> CommandOutcome.message(ReplMessages.help()));
        registry.put(".about", arg -> CommandOutcome.message(ReplMessages.about()));
        registry.put(".use", arg -> handleUse(engine, arg));
        return Map.copyOf(registry);
    }

    private static CommandOutcome handleUse(Engine engine, String graphName) {
        if (graphName == null) return CommandOutcome.message(availableGraphsTable());
        String normalized = graphName.toLowerCase();
        if (!FakeGraphData.graphExists(normalized)) {
            return CommandOutcome.message("ERROR: Graph '" + normalized + "' not found.");
        }
        engine.setCurrentGraph(normalized);
        long time = (long) (Math.random() * 5) + 1;
        return CommandOutcome.message("OK. \"" + normalized + "\" graph available after " + time + "ms\n");
    }

   //unique data source, correcting last observations
    private static String availableGraphsTable() {
        String[][] rows = FakeGraphData.getGraphs().entrySet().stream()
                .map(e -> new String[]{e.getKey(), e.getValue().description})
                .toArray(String[][]::new);

        String[][] table = new String[rows.length + 1][];
        table[0] = new String[]{"Graph", "Description"};
        System.arraycopy(rows, 0, table, 1, rows.length);

        return TableFormatter.formatTable(table) + "\nOK. Query available after 5 ms.";
    }
}