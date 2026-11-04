package org.cyphail.util;

import org.cyphail.ast.*;
import org.junit.jupiter.api.Test;

import static org.cyphail.ast.ExprText.text;
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

/** Ayudantes que antes estaban repetidos: Statements (";" final) y ExprText (texto Cyphail de una expresion). */
class StatementsAndExprTextTest {

    @Test
    void stripTrailingSemicolonRemovesOnlyTheFinalSemicolon() {
        assertEquals("MATCH (a) RETURN a", Statements.stripTrailingSemicolon("MATCH (a) RETURN a ;  "));
        assertEquals("MATCH (a) RETURN a", Statements.stripTrailingSemicolon("MATCH (a) RETURN a"));
        assertEquals("", Statements.stripTrailingSemicolon(null));
    }

    @Test
    void exprTextIsTheCyphailTextOfTheExpression() {
        assertEquals("a.name", text(new PropertyAccess("a", "name")));
        assertEquals("a", text(new Var("a")));
        assertEquals("a.age > 18", text(new Binary(BinaryOp.GT, new PropertyAccess("a", "age"), new NumberLit(18))));
        assertEquals("NOT a.x = true OR b.y <= 2.5",
                text(new Or(new Not(new Binary(BinaryOp.EQ, new PropertyAccess("a", "x"), new BoolLit(true))),
                        new Binary(BinaryOp.LTE, new PropertyAccess("b", "y"), new DecimalLit(2.5)))));
    }
}
