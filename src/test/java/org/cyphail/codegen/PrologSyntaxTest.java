package org.cyphail.codegen;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

class PrologSyntaxTest {

    @Test
    void variableConPrefijoYMayuscula() {
        assertEquals("__A", PrologSyntax.variable("a"));
        assertEquals("__Persona", PrologSyntax.variable("persona"));
        assertEquals("___x", PrologSyntax.variable("_x"));
    }

    @Test
    void atomoSimpleSinComillas() {
        assertEquals("amigo", PrologSyntax.atom("amigo"));
        assertEquals("user_amigos_node", PrologSyntax.atom("user_amigos_node"));
    }

    @Test
    void atomoConComillasCuandoHaceFalta() {
        assertEquals("'Person'", PrologSyntax.atom("Person"));
        assertEquals("'a.name'", PrologSyntax.atom("a.name"));
        assertEquals("'4-666-000'", PrologSyntax.atom("4-666-000"));
        assertEquals("''", PrologSyntax.atom(""));
        assertEquals("'it\\'s'", PrologSyntax.atom("it's"));
        assertEquals("'c:\\\\dir'", PrologSyntax.atom("c:\\dir"));
    }

    @Test
    void terminosCompuestos() {
        assertEquals("cy_eq(__A_name, 'Ana')", PrologSyntax.call("cy_eq", "__A_name", "'Ana'"));
        assertEquals("[a, b]", PrologSyntax.list(List.of("a", "b")));
        assertEquals("[]", PrologSyntax.list(List.of()));
        assertEquals("user_amigos_prop", PrologSyntax.graphPredicate("amigos", "prop"));
    }
}
