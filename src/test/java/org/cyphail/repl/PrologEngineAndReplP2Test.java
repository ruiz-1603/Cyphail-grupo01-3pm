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
    void semicolonIsOptionalStatementEndsWithBlankLineOrDotCommand() {
        // Sin ";": la linea en blanco cierra el estatuto
        Scanner blank = new Scanner("RETURN p.nombre\n\nMATCH (x) RETURN x\n");
        assertEquals("MATCH (p:Persona)\nRETURN p.nombre", Repl.readQuery(blank, "MATCH (p:Persona)"));

        // Sin ";": un dot-command cierra el estatuto y queda pendiente para ejecutarse
        Scanner dot = new Scanner(".use empresa\n");
        Repl.ReadStatement statement = Repl.readStatement(dot, "MATCH (p:Persona) RETURN p.nombre");
        assertEquals("MATCH (p:Persona) RETURN p.nombre", statement.text());
        assertEquals(java.util.Optional.of(".use empresa"), statement.pendingLine());
    }

    @Test
    void parseScriptStatementsWithoutSemicolons() {
        String script = """
                .use empresa
                MATCH (p:Persona)
                RETURN p.nombre

                .tree MATCH (p:Persona)
                RETURN p
                .use p1
                MATCH (m:Movie) RETURN m.title;
                """;
        assertEquals(List.of(
                ".use empresa",
                "MATCH (p:Persona)\nRETURN p.nombre",
                ".tree MATCH (p:Persona)\nRETURN p",
                ".use p1",
                "MATCH (m:Movie) RETURN m.title"),
                CommandRegistry.parseScriptStatements(script));
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
    void tableMatrixKeepsValuesWithSpacesInOneColumn() {
        String[][] table = PrologEngine.toTableMatrix(
                "m.title\tyear\nThe Matrix\t1999\nWar and Peace\t", List.of("m.title", "year"));

        assertArrayEquals(new String[]{"m.title", "year"}, table[0]);
        assertArrayEquals(new String[]{"The Matrix", "1999"}, table[1]);
        assertArrayEquals(new String[]{"War and Peace", ""}, table[2]);
    }

    @Test
    void p1CasesRunOnTheP1Graph() {
        try (PrologEngine engine = new PrologEngine()) {
            assertTrue(engine.listAvailableGraphs().contains("p1"));
            engine.setCurrentGraph("p1");

            // CASO 1: titulos con espacios en una sola columna
            String caso1 = engine.execute("MATCH (m:Movie) RETURN m.title, m.year AS year");
            assertTrue(caso1.contains("The Matrix"));
            assertTrue(caso1.contains("(4 row(s))"));

            // CASO 2
            String caso2 = engine.execute("MATCH (b:Book) WHERE b.pages < 300 RETURN b.title, b.pages AS totalPages");
            assertTrue(caso2.contains("The Great Gatsby"));
            assertTrue(caso2.contains("(1 row(s))"));

            // CASO 3: etiquetas multiples + propiedad
            String caso3 = engine.execute(
                    "MATCH (a:Person:Employee {id: 1}) WHERE a.age > 30 RETURN a.name AS name, a.age AS age");
            assertTrue(caso3.contains("Priscilla"));
            assertTrue(caso3.contains("(1 row(s))"));

            // CASO 8: propiedad que referencia otra variable (personId: p.id)
            String caso8 = engine.execute(
                    "MATCH (p:Person {id: 1}), (o:Order {personId: p.id}) RETURN p.name AS name, o.total AS total");
            assertTrue(caso8.contains("Priscilla"));
            assertTrue(caso8.contains("50000"));

            // CASO 7: CREATE queda fuera de la ejecucion en P2
            String caso7 = engine.execute("MATCH (p:Person) WHERE p.age > 18 "
                    + "CREATE (c:Certificate {issuedTo: \"adult\", year: 2026}) RETURN p.name AS name, p.age AS age");
            assertEquals("ERROR: CREATE is not supported in Sprint P2 (only MATCH, WHERE and RETURN)", caso7);
        }
    }

    @Test
    void prologEngineReportsErrorForUnknownGraphOrMissingFile() {
        try (PrologEngine engine = new PrologEngine()) {
            Map<String, ReplCommand> commands = CommandRegistry.forEngine(engine);

            CommandOutcome badUse = commands.get(".use").execute("nonexistent_graph");
            assertEquals("ERROR: Graph 'nonexistent_graph' not found.", badUse.message());

            CommandOutcome badLoad = commands.get(".load").execute("\"data/does_not_exist.cyphail\"");
            assertTrue(badLoad.message().startsWith("ERROR:"));
        }
    }
}
