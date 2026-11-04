package org.cyphail.repl;

import org.cyphail.engine.PrologEngine;
import org.junit.jupiter.api.Test;

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

/*
 * Pruebas del comando .tree (guia de revision P1: ".tree en cada Ck").
 * El arbol se muestra aunque el query tenga errores de semantica estatica (C10, C11).
 * .tree no ejecuta nada, por eso el engine nunca levanta SWI-Prolog.
 */
class TreeCommandTest {

    private static String tree(String query) {
        try (var engine = new PrologEngine()) {
            return CommandRegistry.forEngine(engine).get(".tree").execute(query).message();
        }
    }

    @Test
    void queryValidoMuestraSoloElArbol() {
        var output = tree("MATCH (m:Movie) WHERE m.year > 1990 RETURN m.title AS title");

        assertTrue(output.startsWith("Query{"));
        assertFalse(output.contains("ERROR"));
    }

    @Test
    void caso10MuestraArbolYErrorSemantico() {
        var output = tree("""
                MATCH (p:Person), (o:Order {personId: p.id, status: "cancelled"})
                WHERE q.age > 60
                RETURN q AS name""");

        assertTrue(output.startsWith("Query{"));
        assertTrue(output.contains("Expr: (> (. q age) 60)"));
        assertTrue(output.endsWith("ERROR: Variable 'q' is not defined in clause WHERE"));
    }

    @Test
    void caso11MuestraArbolYErrorSemantico() {
        var output = tree("""
                MATCH (o:Order {personId: p.id, status: "cancelled"}), (p:Person)
                WHERE p.age > 60
                CREATE (a:Archive {id: o.id, name: "retired", year: 2026})
                DELETE o
                RETURN p.name AS name""");

        assertTrue(output.startsWith("Query{"));
        assertTrue(output.contains("ERROR: "));
    }

    @Test
    void errorDeSintaxisNoTieneArbol() {
        var output = tree("MATCH (m:Movie RETURN m");

        assertTrue(output.startsWith("ERROR: "));
        assertFalse(output.contains("Query{"));
    }
}
