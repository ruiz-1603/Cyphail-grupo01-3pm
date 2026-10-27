package org.cyphail.repl;

import org.cyphail.engine.PrologEngine;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

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

class PrologEngineAndReplP2Test {

    @Test
    void readQueryHandlesSingleAndMultilineEndingWithSemicolon() {
        String multilineInput = "WHERE m.year > 1990\nRETURN m.title AS title,\n       m.year AS year;\n";
        Scanner scanner = new Scanner(multilineInput);
        String query = Repl.readQuery(scanner, "MATCH (m:Movie)");

        assertEquals("""
                MATCH (m:Movie)
                WHERE m.year > 1990
                RETURN m.title AS title,
                       m.year AS year""", query);
        assertFalse(query.endsWith(";"));
    }

    @Test
    void parseScriptStatementsExtractsQueriesAndIgnoresComments() {
        String script = """
                // Comment line
                MATCH (m:Movie)
                WHERE m.year > 2000
                RETURN m.title;
                -- Another comment
                MATCH (p:Person) RETURN p.name;
                """;
        List<String> statements = CommandRegistry.parseScriptStatements(script);
        assertEquals(2, statements.size());
        assertTrue(statements.get(0).startsWith("MATCH (m:Movie)"));
        assertEquals("MATCH (p:Person) RETURN p.name", statements.get(1));
    }

    @Test
    void unquotePathHandlesQuotesAndSemicolon() {
        assertEquals("data/sample_queries.cyphail",
                CommandRegistry.unquotePath("\"data/sample_queries.cyphail\";"));
        assertEquals("data/sample_queries.cyphail",
                CommandRegistry.unquotePath("'data/sample_queries.cyphail'"));
    }

    @Test
    void prologEngineExecutesEndToEndQueriesAndLoadCommandViaMqi() {
        try (PrologEngine engine = new PrologEngine()) {
            List<String> graphs = engine.listAvailableGraphs();
            assertTrue(graphs.containsAll(List.of("amigos", "empresa", "movies")));

            engine.setCurrentGraph("movies");
            assertEquals("movies", engine.getCurrentGraph());

            String output = engine.execute("""
                    MATCH (m:Movie)
                    WHERE m.year > 2000
                    RETURN m.title AS title, m.year AS year;
                    """);
            assertTrue(output.contains("title"));
            assertTrue(output.contains("Avatar"));
            assertTrue(output.contains("2009"));
            assertFalse(output.contains("Matrix"));

            Map<String, ReplCommand> commands = CommandRegistry.forEngine(engine);
            CommandOutcome loadOutcome = commands.get(".load").execute("\"data/sample_queries.cyphail\"");
            assertFalse(loadOutcome.exit());
            assertTrue(loadOutcome.message().contains("Avatar"));
            assertTrue(loadOutcome.message().contains("Keanu"));
        }
    }

    @Test
    void prologEngineReportsErrorForUnknownGraphOrMissingFile() {
        try (PrologEngine engine = new PrologEngine()) {
            Map<String, ReplCommand> commands = CommandRegistry.forEngine(engine);

            CommandOutcome badUse = commands.get(".use").execute("nonexistent_graph");
            assertTrue(badUse.message().startsWith("ERROR:"));

            CommandOutcome badLoad = commands.get(".load").execute("\"data/does_not_exist.cyphail\"");
            assertTrue(badLoad.message().startsWith("ERROR:"));
        }
    }
}
