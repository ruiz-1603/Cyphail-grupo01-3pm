package org.cyphail.codegen;

import org.cyphail.ast.*;
import org.cyphail.parser.CyphailParser;
import org.cyphail.parser.core.Fail;
import org.cyphail.parser.core.Ok;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

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

/**
 * Los ASTs se arman a mano (no dependen del parser), salvo el ultimo test que
 * prueba el flujo parser -> CodeGen.
 */
class CodeGenTest {

    // Helpers para armar ASTs cortos

    private static PatternNode node(String var, List<String> labels, PropertyEntry... props) {
        return new PatternNode(var, labels, List.of(props));
    }

    private static ProjectionItem item(Expr expr) {
        return new ProjectionItem(expr, Optional.empty());
    }

    private static ProjectionItem item(Expr expr, String alias) {
        return new ProjectionItem(expr, Optional.of(alias));
    }

    private static Query query(List<PatternNode> match, Optional<WhereClause> where,
                               List<UpdateClause> updates, ProjectionItem... items) {
        return new Query(new MatchClause(match), where, updates,
                new ReturnClause(new Projection(List.of(items), List.of())));
    }

    private static PropertyAccess prop(String var, String key) {
        return new PropertyAccess(var, key);
    }


    @Test
    void matchSimpleConAlias() {
        // MATCH (m:Movie) RETURN m.title AS title, m.year
        var q = query(List.of(node("m", List.of("Movie"))), Optional.empty(), List.of(),
                item(prop("m", "title"), "title"), item(prop("m", "year")));

        var gen = CodeGen.generate(q, "movies");

        assertEquals("cy_row(__M_title, __M_year)", gen.head());
        assertEquals(List.of(
                "user_movies_node(__M)",
                "user_movies_label(__M, 'Movie')",
                "user_movies_prop(__M, title, __M_title)",
                "user_movies_prop(__M, year, __M_year)"), gen.body());
        assertEquals(List.of("title", "m.year"), gen.columns());
    }

    @Test
    void whereConPropiedadesDeDosVariables() {
        // MATCH (a:Person), (b:Person) WHERE a.age > b.age RETURN a.name, b.name
        var q = query(List.of(node("a", List.of("Person")), node("b", List.of("Person"))),
                Optional.of(new WhereClause(new Binary(BinaryOp.GT, prop("a", "age"), prop("b", "age")))),
                List.of(),
                item(prop("a", "name")), item(prop("b", "name")));

        var gen = CodeGen.generate(q, "amigos");

        assertEquals(List.of(
                "user_amigos_node(__A)",
                "user_amigos_label(__A, 'Person')",
                "user_amigos_node(__B)",
                "user_amigos_label(__B, 'Person')",
                "user_amigos_prop(__A, age, __A_age)",
                "user_amigos_prop(__B, age, __B_age)",
                "cy_gt(__A_age, __B_age)",
                "user_amigos_prop(__A, name, __A_name)",
                "user_amigos_prop(__B, name, __B_name)"), gen.body());
        assertEquals(List.of("a.name", "b.name"), gen.columns());
    }

    @Test
    void cadaOperadorUsaSuComparadorDelEngine() {
        var expected = List.of("cy_lt", "cy_gt", "cy_neq", "cy_eq", "cy_le", "cy_ge");
        var ops = List.of(BinaryOp.LT, BinaryOp.GT, BinaryOp.NEQ, BinaryOp.EQ, BinaryOp.LTE, BinaryOp.GTE);

        for (int i = 0; i < ops.size(); i++) {
            var q = query(List.of(node("a", List.of("Person"))),
                    Optional.of(new WhereClause(new Binary(ops.get(i), prop("a", "age"), new NumberLit(18)))),
                    List.of(), item(new Var("a")));
            var gen = CodeGen.generate(q, "amigos");
            assertTrue(gen.body().contains(expected.get(i) + "(__A_age, 18)"), ops.get(i).symbol());
        }
    }

    @Test
    void propiedadUsadaVariasVecesSeObtieneUnaSolaVez() {
        // MATCH (a:Person {name: "Ana"}) WHERE a.name <> "Luis" RETURN a.name
        var q = query(List.of(node("a", List.of("Person"), new PropertyEntry("name", new StringLit("Ana")))),
                Optional.of(new WhereClause(new Binary(BinaryOp.NEQ, prop("a", "name"), new StringLit("Luis")))),
                List.of(), item(prop("a", "name")));

        var gen = CodeGen.generate(q, "amigos");

        assertEquals(List.of(
                "user_amigos_node(__A)",
                "user_amigos_label(__A, 'Person')",
                "user_amigos_prop(__A, name, __A_name)",
                "cy_eq(__A_name, 'Ana')",
                "cy_neq(__A_name, 'Luis')"), gen.body());
        assertEquals("cy_row(__A_name)", gen.head());
    }

    @Test
    void variableDeJoinNoRepiteElNodo() {
        // MATCH (a:Person), (a:Female) RETURN a
        var q = query(List.of(node("a", List.of("Person")), node("a", List.of("Female"))),
                Optional.empty(), List.of(), item(new Var("a")));

        var gen = CodeGen.generate(q, "amigos");

        assertEquals(List.of(
                "user_amigos_node(__A)",
                "user_amigos_label(__A, 'Person')",
                "user_amigos_label(__A, 'Female')"), gen.body());
    }

    @Test
    void returnDeVariableDevuelveSoloElId() {
        // Restriccion P2: RETURN a -> id del nodo
        var q = query(List.of(node("a", List.of("Person"))), Optional.empty(), List.of(),
                item(new Var("a"), "persona"));

        var gen = CodeGen.generate(q, "amigos");

        assertEquals("cy_row(__A)", gen.head());
        assertEquals(List.of("persona"), gen.columns());
    }

    @Test
    void createNoSeTranspilaEnP2() {
        // MATCH (a:Person) CREATE (b:Person) RETURN b
        var q = query(List.of(node("a", List.of("Person"))), Optional.empty(),
                List.of(new CreateClause(List.of(node("b", List.of("Person"))))),
                item(new Var("b")));

        var error = assertThrows(CodeGenException.class, () -> CodeGen.generate(q, "amigos"));
        assertEquals("CREATE is not supported in Sprint P2 (only MATCH, WHERE and RETURN)", error.getMessage());
    }

    @Test
    void deleteNoSeTranspilaEnP2() {
        // MATCH (r:Robot) DELETE r RETURN r
        var q = query(List.of(node("r", List.of("Robot"))), Optional.empty(),
                List.of(new DeleteClause(List.of("r"))), item(new Var("r")));

        var error = assertThrows(CodeGenException.class, () -> CodeGen.generate(q, "amigos"));
        assertEquals("DELETE is not supported in Sprint P2 (only MATCH, WHERE and RETURN)", error.getMessage());
    }

    @Test
    void goalCompletoParaMqi() {
        // MATCH (m:Movie) WHERE m.year > 2000 RETURN m.title
        var q = query(List.of(node("m", List.of("Movie"))),
                Optional.of(new WhereClause(new Binary(BinaryOp.GT, prop("m", "year"), new NumberLit(2000)))),
                List.of(), item(prop("m", "title")));

        var gen = CodeGen.generate(q, "movies");

        assertEquals("cy_create_query(__QueryId, (cy_row(__M_title) :- "
                        + "user_movies_node(__M), user_movies_label(__M, 'Movie'), "
                        + "user_movies_prop(__M, year, __M_year), cy_gt(__M_year, 2000), "
                        + "user_movies_prop(__M, title, __M_title))), "
                        + "cy_exec_query(__QueryId, ['m.title'], Rows), "
                        + "cy_erase_query(__QueryId)",
                gen.goal());
    }

    @Test
    void clausulaSinGoalsUsaTrue() {
        var gen = new GeneratedQuery("cy_row", List.of(), List.of(), SymbolTable.empty());
        assertEquals("(cy_row :- true)", gen.clause());
    }

    @Test
    void tablaDeSimbolosAsociaVariablesYCalculos() {
        var q = query(List.of(node("a", List.of("Person"))), Optional.empty(), List.of(),
                item(prop("a", "name")));

        var symbols = CodeGen.generate(q, "amigos").symbols();

        assertEquals(Optional.of("__A"), symbols.variable("a"));
        assertEquals(Optional.of("__A_name"), symbols.property("a", "name"));
    }

    @Test
    void variablesQueColisionanRecibenSufijo() {
        // 'a' y 'A' darian ambas __A; 'queryId' daria __QueryId (reservada para el goal externo)
        var q = query(List.of(node("a", List.of("X")), node("A", List.of("X")), node("queryId", List.of("X"))),
                Optional.empty(), List.of(),
                item(new Var("a")), item(new Var("A")), item(new Var("queryId")));

        var gen = CodeGen.generate(q, "g");

        assertEquals("cy_row(__A, __A_2, __QueryId_2)", gen.head());
    }

    @Test
    void textoConComillaSeEscapa() {
        var q = query(List.of(node("p", List.of("Person"), new PropertyEntry("name", new StringLit("O'Neil")))),
                Optional.empty(), List.of(), item(new Var("p")));

        var gen = CodeGen.generate(q, "amigos");

        assertTrue(gen.body().contains("cy_eq(__P_name, 'O\\'Neil')"));
    }

    @Test
    void variableNoDefinidaFalla() {
        var q = query(List.of(node("a", List.of("Person"))), Optional.empty(), List.of(),
                item(prop("x", "name")));

        var error = assertThrows(CodeGenException.class, () -> CodeGen.generate(q, "amigos"));
        assertEquals("Variable 'x' is not defined", error.getMessage());
    }

    @Test
    void flujoParserACodeGen() {
        var text = "MATCH (m:Movie) WHERE m.year > 2000 RETURN m.title AS title";

        GeneratedQuery gen = switch (CyphailParser.parse(text)) {
            case Ok(Query query, var rest) -> CodeGen.generate(query, "movies");
            case Fail(String reason) -> fail(reason);
        };

        assertEquals(List.of("title"), gen.columns());
        assertEquals("cy_gt(__M_year, 2000)", gen.body().get(3));
    }
}
