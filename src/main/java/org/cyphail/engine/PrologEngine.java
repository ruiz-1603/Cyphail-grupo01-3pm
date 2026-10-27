package org.cyphail.engine;

import org.cyphail.ast.Query;
import org.cyphail.codegen.CodeGen;
import org.cyphail.codegen.CodeGenException;
import org.cyphail.codegen.GeneratedQuery;
import org.cyphail.codegen.PrologSyntax;
import org.cyphail.mqi.MqiClient;
import org.cyphail.mqi.PrologResult;
import org.cyphail.parser.CyphailParser;
import org.cyphail.parser.core.Fail;
import org.cyphail.parser.core.InputString;
import org.cyphail.parser.core.Ok;
import org.cyphail.util.TableFormatter;
import org.cyphail.validator.VariableValidator;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
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
 */

/**
 * Broker and Engine implementation backed by SWI-Prolog over MQI.
 * Starts the SWI-Prolog MQI process, opens the socket connection, sends transpiled
 * goals from {@link CodeGen}, parses the JSON response, and formats tabular output.
 */
public class PrologEngine implements Engine {

    private static final String DEFAULT_ENGINE_SCRIPT = "prolog/engine.pl";
    private static final String LOCALHOST = "127.0.0.1";

    private final String engineScriptPath;
    private Process prologProcess;
    private Socket socket;
    private MqiClient mqiClient;
    private String currentGraph;

    public PrologEngine() {
        this(DEFAULT_ENGINE_SCRIPT);
    }

    public PrologEngine(String engineScriptPath) {
        this.engineScriptPath = engineScriptPath;
    }

    /** Starts the SWI-Prolog MQI server process and authenticates the client socket if not already started. */
    public synchronized void ensureStarted() throws IOException {
        if (mqiClient != null) {
            return;
        }
        var pb = new ProcessBuilder(
                "swipl",
                "-l", engineScriptPath,
                "mqi",
                "--write_connection_values=true"
        );
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        pb.directory(new File("."));

        this.prologProcess = pb.start();
        var prologStdout = new BufferedReader(
                new InputStreamReader(prologProcess.getInputStream(), StandardCharsets.UTF_8));

        String portLine = prologStdout.readLine();
        String passwordLine = prologStdout.readLine();
        if (portLine == null || passwordLine == null) {
            throw new IOException("Failed to read MQI port and password from SWI-Prolog process.");
        }

        int port = Integer.parseInt(portLine.trim());
        String password = passwordLine.trim();

        this.socket = new Socket(LOCALHOST, port);
        this.mqiClient = new MqiClient(
                new BufferedInputStream(socket.getInputStream()),
                socket.getOutputStream()
        );
        this.mqiClient.authenticate(password);
    }

    /** Executes a raw Prolog goal over MQI and returns the parsed {@link PrologResult}. */
    public synchronized PrologResult runGoal(String goal) throws IOException {
        ensureStarted();
        return mqiClient.run(goal);
    }

    @Override
    public List<String> listAvailableGraphs() {
        try {
            return switch (runGoal("cy_db(G)")) {
                case PrologResult.Success(var rows) -> rows.stream()
                        .map(row -> row.get("G"))
                        .filter(g -> g != null)
                        .map(Object::toString)
                        .distinct()
                        .sorted()
                        .toList();
                case PrologResult.Failure(), PrologResult.PrologError(_) -> List.of();
            };
        } catch (IOException e) {
            throw new IllegalStateException("Could not query available graphs via MQI: " + e.getMessage(), e);
        }
    }

    @Override
    public void setCurrentGraph(String graphName) {
        if (graphName == null || graphName.isBlank()) {
            throw new IllegalArgumentException("Graph name cannot be empty.");
        }
        String normalized = graphName.trim().toLowerCase();
        try {
            PrologResult result = runGoal("set_current_graph(" + PrologSyntax.atom(normalized) + ")");
            switch (result) {
                case PrologResult.Success _ -> this.currentGraph = normalized;
                case PrologResult.Failure _ ->
                        throw new IllegalArgumentException("Graph '" + normalized + "' not found.");
                case PrologResult.PrologError(Object term) ->
                        throw new IllegalStateException("Prolog error selecting graph: " + term);
            }
        } catch (IOException e) {
            throw new IllegalStateException("MQI communication error: " + e.getMessage(), e);
        }
    }

    @Override
    public String getCurrentGraph() {
        return currentGraph;
    }

    @Override
    public String execute(String query) {
        if (currentGraph == null) {
            return "ERROR: No graph selected. Use .use <graph_name> first.";
        }

        String cleanQuery = stripTrailingSemicolon(query);

        return switch (CyphailParser.parse(cleanQuery)) {
            case Fail<InputString, Query, String>(String reason) -> "ERROR: " + reason;
            case Ok<InputString, Query, String>(Query ast, InputString _) -> {
                var validationError = VariableValidator.validate(ast);
                if (validationError.isPresent()) {
                    yield "ERROR: " + validationError.get();
                }
                yield executeValidatedAst(ast);
            }
        };
    }

    private String executeValidatedAst(Query ast) {
        try {
            long startTime = System.currentTimeMillis();
            GeneratedQuery generated = CodeGen.generate(ast, currentGraph);
            PrologResult result = runGoal(generated.goal());
            long elapsed = System.currentTimeMillis() - startTime;

            return formatPrologResult(result, generated.columns(), elapsed);
        } catch (CodeGenException e) {
            return "ERROR: " + e.getMessage();
        } catch (IOException e) {
            return "ERROR: MQI communication failure: " + e.getMessage();
        }
    }

    /** Formats a PrologResult containing the {@code Rows} binding into a tabular string. */
    public static String formatPrologResult(PrologResult result, List<String> columns, long elapsedMs) {
        return switch (result) {
            case PrologResult.Success(var rows) -> {
                String rowsText = extractRowsText(rows);
                String[][] tableData = toTableMatrix(rowsText, columns);
                int solutionCount = Math.max(0, tableData.length - 1);
                String table = TableFormatter.formatTable(tableData);
                yield table + "\n(" + solutionCount + " row(s))\nOK. Query resolved after " + elapsedMs + " ms.\n";
            }
            case PrologResult.Failure() -> "Failure (no solutions)\n";
            case PrologResult.PrologError(Object term) -> "ERROR: Prolog exception: " + term;
        };
    }

    private static String extractRowsText(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return "";
        }
        Object raw = rows.getFirst().get(GeneratedQuery.ROWS);
        return raw != null ? raw.toString() : "";
    }

    /** Converts the newline/space-delimited {@code Rows} string from {@code cy_engine.pl} into a 2D table matrix. */
    public static String[][] toTableMatrix(String rowsText, List<String> columns) {
        String[] header = columns.toArray(String[]::new);
        if (rowsText == null || rowsText.isBlank()) {
            return new String[][]{header};
        }

        String[] lines = rowsText.split("\\R");
        Stream<String[]> dataRows = Arrays.stream(lines)
                .skip(1) // skip Prolog header line; use exact column names from CodeGen
                .filter(line -> !line.isBlank())
                .map(line -> splitRowCells(line, header.length));

        return Stream.concat(Stream.<String[]>of(header), dataRows)
                .toArray(String[][]::new);
    }

    private static String[] splitRowCells(String line, int columnCount) {
        if (columnCount <= 1) {
            return new String[]{line.trim()};
        }
        String[] parts = line.trim().split("\\s+", columnCount);
        return IntStream.range(0, columnCount)
                .mapToObj(i -> i < parts.length ? parts[i] : "")
                .toArray(String[]::new);
    }

    private static String stripTrailingSemicolon(String input) {
        if (input == null) {
            return "";
        }
        String trimmed = input.stripTrailing();
        return trimmed.endsWith(";")
                ? trimmed.substring(0, trimmed.length() - 1).stripTrailing()
                : trimmed;
    }

    @Override
    public synchronized void close() {
        if (mqiClient != null) {
            try {
                mqiClient.close();
            } catch (IOException ignored) {
            } finally {
                mqiClient = null;
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            } finally {
                socket = null;
            }
        }
        if (prologProcess != null) {
            prologProcess.destroy();
            prologProcess = null;
        }
    }
}
