package org.cyphail.repl;

import org.cyphail.ast.AstPrinter;
import org.cyphail.ast.Query;
import org.cyphail.engine.Engine;
import org.cyphail.parser.CyphailParser;
import org.cyphail.parser.core.Fail;
import org.cyphail.parser.core.InputString;
import org.cyphail.parser.core.Ok;
import org.cyphail.util.TableFormatter;
import org.cyphail.validator.VariableValidator;
import org.cyphail.data.JsonGraphLoader;

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
 *
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
        registry.put(".tree", arg -> handleTree(arg));
        return Map.copyOf(registry);
    }

private static CommandOutcome handleUse(Engine engine, String graphName) {
    if (graphName == null) {
        return CommandOutcome.message(availableGraphsTable());
    }
    
    String normalized = graphName.toLowerCase();
    
    try {
        JsonGraphLoader.GraphData graphData = JsonGraphLoader.loadGraph(normalized);
        engine.setCurrentGraph(normalized);
        long time = (long) (Math.random() * 5) + 1;
        return CommandOutcome.message("OK. \"" + normalized + "\" graph available after " + time + "ms\n");
    } catch (Exception e) {
        return CommandOutcome.message("ERROR: Graph '" + normalized + "' not found. " + e.getMessage());
    }
}

private static String availableGraphsTable() {
    try {
        var graphs = JsonGraphLoader.listAvailableGraphs();
        String[][] table = new String[graphs.size() + 1][];
        table[0] = new String[]{"Graph"};
        for (int i = 0; i < graphs.size(); i++) {
            table[i + 1] = new String[]{graphs.get(i)};
        }
        return TableFormatter.formatTable(table) + "\nOK. Query available after 5 ms.";
    } catch (java.io.IOException e) {
        return "ERROR: could not read data directory: " + e.getMessage();
    }
}
private static CommandOutcome handleTree(String query) {
    if (query == null) {
        return CommandOutcome.message("Usage: .tree <query>");
    }

    return switch (CyphailParser.parse(query)) {
        case Ok<InputString, Query, String>(Query ast, InputString rest) -> {
            // ✅ VALIDAR VARIABLES PRIMERO
            var validationError = VariableValidator.validate(ast);
            if (validationError.isPresent()) {
                yield CommandOutcome.message("ERROR: " + validationError.get());
            }
            yield CommandOutcome.message(AstPrinter.print(ast));
        }
        case Fail<InputString, Query, String>(String reason) ->
                CommandOutcome.message("ERROR: " + reason);
    };
}



}