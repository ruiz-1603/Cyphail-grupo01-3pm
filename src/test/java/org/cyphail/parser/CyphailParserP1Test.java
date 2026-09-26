package org.cyphail.parser;

import org.cyphail.ast.*;
import org.cyphail.parser.core.Result;
import org.cyphail.parser.core.Ok;
import org.cyphail.parser.core.Fail;
import org.cyphail.parser.core.InputString;
import org.cyphail.validator.VariableValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Proyecto Cyphail - Grupo 01-3pm
 * Autores:
 * - Priscilla Murillo Romero
 * - Aaron Ruiz Medina
 * - Samael Sanchez Mora
 * - Daniel Villarroel Abaduca
 * - Nicolás Zárate Hernández
 * 
 * Tests para los 11 casos de prueba del Sprint P1.
 */
class CyphailParserP1Test {

    // ========== CASO 1 ==========
    @Test
    void c1_PatternAndProjectionWithAlias() {
        String query = """
                MATCH (m:Movie)
                RETURN m.title,
                       m.year AS year
                """;

        var result = CyphailParser.parse(query);
        assertFalse(result instanceof Fail, "C1: Should parse successfully");
        
        Query ast = ((Ok<InputString, Query, String>) result).token();
        assertEquals(1, ast.match().patterns().size());
        assertEquals("m", ast.match().patterns().get(0).variable());
        assertFalse(ast.where().isPresent());
        
        var validation = VariableValidator.validate(ast);
        assertTrue(validation.isEmpty(), "C1: Should have no variable errors");
    }

    // ========== CASO 2 ==========
    @Test
    void c2_PatternWithWhereComparison() {
        String query = """
                MATCH (b:Book)
                WHERE b.pages < 300
                RETURN b.title,
                       b.pages AS totalPages
                """;

        var result = CyphailParser.parse(query);
        assertFalse(result instanceof Fail, "C2: Should parse successfully");
        
        Query ast = ((Ok<InputString, Query, String>) result).token();
        assertTrue(ast.where().isPresent(), "C2: Should have WHERE clause");
        
        var validation = VariableValidator.validate(ast);
        assertTrue(validation.isEmpty(), "C2: Should have no variable errors");
    }

    // ========== CASO 3 ==========
    @Test
    void c3_MultipleLabels() {
        String query = """
                MATCH (a:Person:Employee {id: 1})
                WHERE a.age > 30
                RETURN a.name AS name,
                       a.age AS age
                """;

        var result = CyphailParser.parse(query);
        assertFalse(result instanceof Fail, "C3: Should parse successfully");
        
        Query ast = ((Ok<InputString, Query, String>) result).token();
        var pattern = ast.match().patterns().get(0);
        assertEquals(2, pattern.labels().size(), "C3: Should have two labels");
        assertTrue(pattern.labels().contains("Person"));
        assertTrue(pattern.labels().contains("Employee"));
        
        var validation = VariableValidator.validate(ast);
        assertTrue(validation.isEmpty(), "C3: Should have no variable errors");
    }

    // ========== CASO 4 ==========
    @Test
    void c4_TwoDisconnectedPatterns() {
        String query = """
                MATCH (m:Movie), (p:Person)
                WHERE m.year > 2000
                RETURN m.title AS title,
                       p.name AS actor
                """;

        var result = CyphailParser.parse(query);
        assertFalse(result instanceof Fail, "C4: Should parse successfully");
        
        Query ast = ((Ok<InputString, Query, String>) result).token();
        assertEquals(2, ast.match().patterns().size(), "C4: Should have two patterns");
        
        var validation = VariableValidator.validate(ast);
        assertTrue(validation.isEmpty(), "C4: Should have no variable errors");
    }

    // ========== CASO 5 ==========
    @Test
    void c5_TwoPatternsDifferentAttributes() {
        String query = """
                MATCH (m:Movie), (p:Person)
                WHERE m.year <> p.age
                RETURN m.title AS title,
                       p.name AS name
                """;

        var result = CyphailParser.parse(query);
        assertFalse(result instanceof Fail, "C5: Should parse successfully");
        
        Query ast = ((Ok<InputString, Query, String>) result).token();
        assertTrue(ast.where().isPresent(), "C5: Should have WHERE");
        
        var validation = VariableValidator.validate(ast);
        assertTrue(validation.isEmpty(), "C5: Should have no variable errors");
    }

    // ========== CASO 6 ==========
    @Test
    void c6_PatternsWithProperties() {
        String query = """
                MATCH (m:Movie {year: 1999}), (p:Person {age: 40})
                WHERE m.year <> p.age
                RETURN m.title AS title,
                       p.name AS name
                """;

        var result = CyphailParser.parse(query);
        assertFalse(result instanceof Fail, "C6: Should parse successfully");
        
        Query ast = ((Ok<InputString, Query, String>) result).token();
        var m = ast.match().patterns().get(0);
        var p = ast.match().patterns().get(1);
        assertFalse(m.properties().isEmpty(), "C6: m should have properties");
        assertFalse(p.properties().isEmpty(), "C6: p should have properties");
        
        var validation = VariableValidator.validate(ast);
        assertTrue(validation.isEmpty(), "C6: Should have no variable errors");
    }

    // ========== CASO 7 ==========
    @Test
    void c7_MatchAndCreate() {
        String query = """
                MATCH (p:Person)
                WHERE p.age > 18
                CREATE (c:Certificate {issuedTo: "adult", year: 2026})
                RETURN p.name AS name,
                       p.age AS age
                """;

        var result = CyphailParser.parse(query);
        assertFalse(result instanceof Fail, "C7: Should parse successfully");
        
        Query ast = ((Ok<InputString, Query, String>) result).token();
        assertFalse(ast.updates().isEmpty(), "C7: Should have updates");
        assertTrue(ast.updates().get(0) instanceof CreateClause);
        
        var validation = VariableValidator.validate(ast);
        assertTrue(validation.isEmpty(), "C7: Should have no variable errors");
    }

    // ========== CASO 8 ==========
    @Test
    void c8_PropertyExpressionSimple() {
        String query = """
                MATCH (p:Person {id: 1}), (o:Order {personId: p.id})
                RETURN p.name AS name,
                       o.total AS total
                """;

        var result = CyphailParser.parse(query);
        assertFalse(result instanceof Fail, "C8: Should parse successfully");
        
        Query ast = ((Ok<InputString, Query, String>) result).token();
        var oPattern = ast.match().patterns().get(1);
        var personIdProp = oPattern.properties().stream()
            .filter(prop -> "personId".equals(prop.key()))
            .findFirst();
        assertTrue(personIdProp.isPresent());
        assertTrue(personIdProp.get().value() instanceof PropertyAccess);
        
        var validation = VariableValidator.validate(ast);
        assertTrue(validation.isEmpty(), "C8: Should have no variable errors");
    }

    // ========== CASO 9 ==========
    @Test
    void c9_MatchCreateDelete() {
        String query = """
                MATCH (p:Person), (o:Order {personId: p.id, status: "cancelled"})
                WHERE p.age > 60
                CREATE (a:Archive {id: o.id, name: "retired", year: 2026})
                DELETE o
                RETURN p.name AS name
                """;

        var result = CyphailParser.parse(query);
        assertFalse(result instanceof Fail, "C9: Should parse successfully");
        
        Query ast = ((Ok<InputString, Query, String>) result).token();
        assertEquals(2, ast.updates().size(), "C9: Should have CREATE and DELETE");
        assertTrue(ast.updates().get(0) instanceof CreateClause);
        assertTrue(ast.updates().get(1) instanceof DeleteClause);
        
        var validation = VariableValidator.validate(ast);
        assertTrue(validation.isEmpty(), "C9: Should have no variable errors");
    }

    // ========== CASO 10 ==========
    @Test
    void c10_UndefinedVariableInWhere() {
        String query = """
                MATCH (p:Person), (o:Order {personId: p.id, status: "cancelled"})
                WHERE q.age > 60
                RETURN q AS name
                """;

        var result = CyphailParser.parse(query);
        assertFalse(result instanceof Fail, "C10: Should parse successfully");

        Query ast = ((Ok<InputString, Query, String>) result).token();
        var validation = VariableValidator.validate(ast);
        assertTrue(validation.isPresent(), "C10: Should report undefined variable 'q'");
        assertTrue(validation.get().contains("'q'"));
    }

    @Test
    void c11_VariableUsedBeforeDeclared() {
        String query = """
                MATCH (o:Order {personId: p.id, status: "cancelled"}), (p:Person)
                WHERE p.age > 60
                CREATE (a:Archive {id: o.id, name: "retired", year: 2026})
                DELETE o
                RETURN p.name AS name
                """;

        var result = CyphailParser.parse(query);
        assertFalse(result instanceof Fail, "C11: Should parse successfully");

        Query ast = ((Ok<InputString, Query, String>) result).token();
        var validation = VariableValidator.validate(ast);
        assertTrue(validation.isPresent(), "C11: 'p' is used before its own declaration in MATCH");
        assertTrue(validation.get().contains("'p'"));
    }
}