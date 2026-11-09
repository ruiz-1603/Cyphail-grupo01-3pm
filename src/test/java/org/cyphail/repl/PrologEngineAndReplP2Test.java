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

            // CASO 7: un Certificate por cada adulto (4 filas)
            String caso7 = engine.execute("MATCH (p:Person) WHERE p.age > 18 "
                    + "CREATE (c:Certificate {issuedTo: \"adult\", year: 2026}) RETURN p.name AS name, p.age AS age");
            assertTrue(caso7.contains("(4 row(s))"));
            assertTrue(caso7.contains("4 node(s) created, 8 property(ies) set, 4 label(s) added"));
            assertTrue(engine.execute("MATCH (c:Certificate) RETURN c.year").contains("(4 row(s))"));

            // CASO 9: archiva la orden cancelada de Samael y la borra
            String caso9 = engine.execute("MATCH (p:Person), (o:Order {personId: p.id, status: \"cancelled\"}) "
                    + "WHERE p.age > 60 CREATE (a:Archive {id: o.id, name: \"retired\", year: 2026}) DELETE o "
                    + "RETURN p.name AS name");
            assertTrue(caso9.contains("Samael"));
            assertTrue(caso9.contains("1 node(s) deleted"));
            assertTrue(engine.execute("MATCH (o:Order) RETURN o.id").contains("(2 row(s))"));

            // .use vuelve a cargar el grafo desde su archivo
            engine.setCurrentGraph("p1");
            assertTrue(engine.execute("MATCH (c:Certificate) RETURN c").contains("(0 row(s))"));
            assertTrue(engine.execute("MATCH (o:Order) RETURN o.id").contains("(3 row(s))"));
        }
    }

    @Test
    void p2UpdateCasesRunOnEmpresaAndRevision() {
        try (PrologEngine engine = new PrologEngine()) {
            engine.setCurrentGraph("empresa");

            // CASO 7: MATCH de dos nodos + CREATE de la relacion (sin RETURN: solo el resumen)
            String caso7 = engine.execute("MATCH (a:Persona {nombre: 'Marta'}), (b:Persona {nombre: 'Pedro'}) "
                    + "CREATE (a)-[:CONOCE {desde: 2020, nivel: 'amigo'}]->(b)");
            assertEquals("1 relationship(s) created, 2 property(ies) set", caso7.lines().findFirst().orElseThrow());
            assertTrue(engine.execute("MATCH (a:Persona {nombre: 'Marta'})-[r:CONOCE]->(b:Persona {nombre: 'Pedro'}) "
                    + "RETURN r.nivel").contains("amigo"));

            // CASO 8: CREATE sin MATCH
            String caso8 = engine.execute("CREATE (a:Persona {nombre: 'Marta'})-[r:TRABAJA_EN {cargo: 'Dev', activo: true}]"
                    + "->(e:Empresa {nombre: 'Acme'}) RETURN a, r, e");
            assertTrue(caso8.contains("(1 row(s))"));
            assertTrue(caso8.contains("2 node(s) created, 1 relationship(s) created, 4 property(ies) set, 2 label(s) added"));

            // CASO 9: DELETE de la relacion de 1998
            String caso9 = engine.execute("MATCH (a:Persona)-[r:CONOCE]->(b:Persona) WHERE r.desde < 2000 DELETE r");
            assertTrue(caso9.startsWith("1 relationship(s) deleted"));
            assertTrue(engine.execute("MATCH (a)-[r:CONOCE]->(b) WHERE r.desde < 2000 RETURN r").contains("(0 row(s))"));

            // Un nodo con relaciones no se borra (como en Cypher) y queda en el grafo
            String deleteNode = engine.execute("MATCH (p:Persona {nombre: 'Jose'}) DELETE p");
            assertTrue(deleteNode.startsWith("ERROR:"));
            assertTrue(deleteNode.contains("still has relationships"));
            assertTrue(engine.execute("MATCH (p:Persona {nombre: 'Jose'}) RETURN p").contains("(1 row(s))"));

            // Un nodo sin relaciones si se borra (la Empresa creada en el CASO 8 solo tiene un arco: se borra primero)
            assertTrue(engine.execute("MATCH (a)-[t:TRABAJA_EN {activo: true}]->(e) DELETE t, e").contains("1 node(s) deleted"));

            // CASO 11 (Guia P2): SET multiple
            engine.setCurrentGraph("revision");
            String caso11 = engine.execute("MATCH (u:Student {id: \"4-666-000\"})-[r:attends]->(c:Course {nrc: \"50691\"}) "
                    + "SET r.grade = 9.5, u.activo = true, c.cupo = 30 RETURN u.id, r.grade, u.activo, c.cupo");
            assertTrue(caso11.contains("4-666-000"));
            assertTrue(caso11.contains("9.5"));
            assertTrue(caso11.contains("3 property(ies) set"));

            // CASO 12 (Guia P2): REMOVE multiple
            String caso12 = engine.execute("MATCH (p:Product {code: \"PROD-99\"}) REMOVE p.discount, p.promo, p:Oferta RETURN p.id");
            assertTrue(caso12.contains("99"));
            assertTrue(caso12.contains("2 property(ies) removed, 1 label(s) removed"));
            assertTrue(engine.execute("MATCH (p:Product:Oferta) RETURN p.id").contains("(0 row(s))"));
        }
    }

    // Ejemplos de actualizacion del SPEC P2 (II.4) sobre el grafo revision
    @Test
    void specP2UpdateExamplesRunOnRevision() {
        try (PrologEngine engine = new PrologEngine()) {
            engine.setCurrentGraph("revision");

            String create = engine.execute("MATCH (a:Autor {nombre: 'Bill Gates'}), "
                    + "(b:Libro {titulo: 'Fake Windows Millenium'}) CREATE (a)-[:ESCRIBIO]->(b);");
            assertTrue(create.startsWith("1 relationship(s) created"));
            assertTrue(engine.execute("MATCH (a:Autor)-[:ESCRIBIO]->(b:Libro) RETURN a.nombre, b.titulo")
                    .contains("Fake Windows Millenium"));

            String delete = engine.execute("MATCH (a:Person:Female)-[r:KNOWS]->(b:Person:Male) "
                    + "WHERE r.since < 2000 DELETE r");
            assertTrue(delete.startsWith("1 relationship(s) deleted"));
            assertTrue(engine.execute("MATCH (a)-[r:KNOWS]->(b) RETURN r").contains("(2 row(s))"));

            String set = engine.execute("MATCH (p:Product {code: \"PROD-99\"}) SET p.precio = 19.99 RETURN p.precio");
            assertTrue(set.contains("19.99"));
            assertTrue(set.contains("1 property(ies) set"));
        }
    }

    @Test
    void scriptsOfAllCasesLoadWithoutErrors() {
        try (PrologEngine engine = new PrologEngine()) {
            Map<String, ReplCommand> commands = CommandRegistry.forEngine(engine);
            for (String script : List.of("data/casos_p1.cyphail", "data/casos_p2.cyphail", "data/spec_p2.cyphail")) {
                String output = commands.get(".load").execute("\"" + script + "\"").message();
                // Los unicos ERROR esperados son los de variables no definidas de P1 (C10, C11, C12)
                long errors = output.lines().filter(line -> line.startsWith("ERROR:")).count();
                long expected = script.contains("p1") ? 3 : 0;
                assertEquals(expected, errors, script + "\n" + output);
            }
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
