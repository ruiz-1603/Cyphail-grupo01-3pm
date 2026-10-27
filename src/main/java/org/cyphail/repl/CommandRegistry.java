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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
 * Final client from Command Pattern; defines the commands and connects them to their receiver using lambdas.
 */
public final class CommandRegistry {

    private CommandRegistry() {}

    public static Map<String, ReplCommand> forEngine(Engine engine) {
        Map<String, ReplCommand> registry = new LinkedHashMap<>();
        registry.put(".exit", arg -> CommandOutcome.exit("Thanks for using Cyphail!"));
        registry.put(".quit", arg -> CommandOutcome.exit("Thanks for using Cyphail!"));
        registry.put(".help", arg -> CommandOutcome.message(ReplMessages.help()));
        registry.put(".about", arg -> CommandOutcome.message(ReplMessages.about()));
        registry.put(".use", arg -> handleUse(engine, arg));
        registry.put(".tree", arg -> handleTree(arg));
        registry.put(".load", arg -> handleLoad(engine, arg));
        return Map.copyOf(registry);
    }

    private static CommandOutcome handleUse(Engine engine, String graphName) {
        if (graphName == null || graphName.isBlank()) {
            return CommandOutcome.message(availableGraphsTable(engine));
        }

        String normalized = Repl.stripTrailingSemicolon(graphName).trim().toLowerCase();

        try {
            long start = System.currentTimeMillis();
            engine.setCurrentGraph(normalized);
            long time = Math.max(1L, System.currentTimeMillis() - start);
            return CommandOutcome.message("OK. \"" + normalized + "\" graph available after " + time + "ms\n");
        } catch (Exception e) {
            return CommandOutcome.message("ERROR: Graph '" + normalized + "' not found. " + e.getMessage());
        }
    }

    private static String availableGraphsTable(Engine engine) {
        try {
            List<String> graphs = engine.listAvailableGraphs();
            String[][] table = Stream.concat(
                    Stream.<String[]>of(new String[]{"Graph"}),
                    graphs.stream().map(g -> new String[]{g})
            ).toArray(String[][]::new);
            return TableFormatter.formatTable(table) + "\nOK. Query available after 5 ms.";
        } catch (Exception e) {
            return "ERROR: could not list available graphs: " + e.getMessage();
        }
    }

    private static CommandOutcome handleTree(String query) {
        if (query == null || query.isBlank()) {
            return CommandOutcome.message("Usage: .tree <query>");
        }

        String cleanQuery = Repl.stripTrailingSemicolon(query);

        return switch (CyphailParser.parse(cleanQuery)) {
            case Ok<InputString, Query, String>(Query ast, InputString _) ->
                    VariableValidator.validate(ast)
                            .map(err -> CommandOutcome.message("ERROR: " + err))
                            .orElseGet(() -> CommandOutcome.message(AstPrinter.print(ast)));
            case Fail<InputString, Query, String>(String reason) ->
                    CommandOutcome.message("ERROR: " + reason);
        };
    }

    private static CommandOutcome handleLoad(Engine engine, String rawArg) {
        String cleanPath = unquotePath(rawArg);
        if (cleanPath.isEmpty()) {
            return CommandOutcome.message("Usage: .load \"<path/to/file.cyphail>\"");
        }

        try {
            Path path = Path.of(cleanPath);
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                return CommandOutcome.message("ERROR: File not found: " + cleanPath);
            }

            String content = Files.readString(path, StandardCharsets.UTF_8);
            List<String> statements = parseScriptStatements(content);
            if (statements.isEmpty()) {
                return CommandOutcome.message("ERROR: No statements found in file: " + cleanPath);
            }

            String output = statements.stream()
                    .map(stmt -> executeScriptStatement(engine, stmt))
                    .collect(Collectors.joining("\n"));
            return CommandOutcome.message(output);
        } catch (InvalidPathException | IOException e) {
            return CommandOutcome.message("ERROR: Could not read file '" + cleanPath + "': " + e.getMessage());
        }
    }

    private static String executeScriptStatement(Engine engine, String statement) {
        if (statement.toLowerCase().startsWith(".use ")) {
            return handleUse(engine, statement.substring(5).trim()).message();
        }
        if (statement.toLowerCase().startsWith(".tree ")) {
            return handleTree(statement.substring(6).trim()).message();
        }
        if (engine.getCurrentGraph() == null) {
            return "ERROR: No graph selected. Use .use <graph_name> first.";
        }
        return engine.execute(statement);
    }

    /** Extracts clean statements separated by ';' from a .cyphail script, ignoring comment lines. */
    public static List<String> parseScriptStatements(String scriptContent) {
        if (scriptContent == null || scriptContent.isBlank()) {
            return List.of();
        }
        String withoutComments = Arrays.stream(scriptContent.split("\\R"))
                .filter(line -> {
                    String trimmed = line.trim();
                    return !trimmed.startsWith("//") && !trimmed.startsWith("--") && !trimmed.startsWith("#");
                })
                .collect(Collectors.joining("\n"));

        return Arrays.stream(withoutComments.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public static String unquotePath(String rawArg) {
        if (rawArg == null) {
            return "";
        }
        String trimmed = Repl.stripTrailingSemicolon(rawArg).trim();
        if (trimmed.length() >= 2
                && ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'")))) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }
}
