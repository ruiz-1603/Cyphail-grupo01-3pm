package org.cyphail.validator;

import org.cyphail.ast.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.cyphail.ast.P2Asts.*;
import static org.junit.jupiter.api.Assertions.*;

/** ASTs armados a mano: no dependen del parser. */
class VariableValidatorTest {

    private static String errorOf(Query query) {
        return VariableValidator.validate(query).orElseThrow(() -> new AssertionError("Expected a validation error"));
    }

    private static void assertValid(Query query) {
        assertEquals(Optional.empty(), VariableValidator.validate(query));
    }

    // ------------------------------------------------------------ positivos: ejemplos del SPEC P2

    @Test
    void ejemplosDelSpecSonValidos() {
        assertValid(trabajaEn());
        assertValid(createSinMatch());
        assertValid(deleteRelacion());
        assertValid(setPrecio());
        assertValid(removeDescuento());
        assertValid(setNota());
        assertValid(matchCreate());
    }

    @Test
    void tablaDeSimbolosTieneTiposYColumnas() {
        var table = VariableValidator.analyze(setNota());

        assertEquals(Map.of("u", VarKind.NODE, "r", VarKind.RELATIONSHIP, "c", VarKind.NODE), table.variables());
        assertEquals(List.of("u", "c"), table.namesOf(VarKind.NODE));
        assertEquals(List.of("r"), table.namesOf(VarKind.RELATIONSHIP));
        assertEquals(List.of("u.id", "r.grade"), table.columns());
    }

    @Test
    void columnasUsanElAliasSiExiste() {
        var query = new Query(match(node("m", "Movie")), Optional.empty(), List.of(),
                returning(item(prop("m", "title"), "title"), item(prop("m", "year")), item(new Var("m"))));

        assertEquals(List.of("title", "m.year", "m"), VariableValidator.analyze(query).columns());
    }

    @Test
    void variableDeNodoSePuedeRepetir() {
        // (p)-[:TRABAJA_EN]->(e), (c)-[:TRABAJA_EN]->(e): e es variable de join
        var table = VariableValidator.analyze(trabajaEn());
        assertEquals(VarKind.NODE, table.kindOf("e").orElseThrow());
    }

    @Test
    void createDeclaraVariablesParaElReturn() {
        var table = VariableValidator.analyze(createSinMatch());
        assertEquals(List.of("a", "r", "e"), table.columns());
        assertEquals(VarKind.RELATIONSHIP, table.kindOf("r").orElseThrow());
    }

    @Test
    void nodosYRelacionesAnonimosNoDeclaranNada() {
        var query = new Query(
                match(path(anonymousNode("Persona"), anonymousRel(Direction.NONE), node("e"))),
                Optional.empty(), List.of(), returning(item(new Var("e"))));

        assertEquals(Map.of("e", VarKind.NODE), VariableValidator.analyze(query).variables());
    }

    @Test
    void whereConAndOrNot() {
        var query = new Query(
                match(path(node("a"), rel("r", Direction.OUT, "KNOWS"), node("b"))),
                where(new And(new Not(new Binary(BinaryOp.LT, prop("a", "age"), new NumberLit(18))),
                        new Or(new Binary(BinaryOp.GT, prop("r", "since"), new NumberLit(2000)),
                                new Binary(BinaryOp.EQ, prop("b", "vip"), new NumberLit(1))))),
                List.of(), returning(item(prop("a", "name"))));

        assertValid(query);
    }

    // ------------------------------------------------------------ negativos: reglas nuevas de P2

    @Test
    void variableDeRelacionNoSePuedeRepetir() {
        // MATCH (a)-[r]->(b), (c)-[r]->(b) RETURN a
        var query = new Query(
                match(path(node("a"), rel("r", Direction.OUT), node("b")),
                        path(node("c"), rel("r", Direction.OUT), node("b"))),
                Optional.empty(), List.of(), returning(item(new Var("a"))));

        assertEquals("Relationship variable 'r' cannot be repeated in the same query", errorOf(query));
    }

    @Test
    void variableDeRelacionDelMatchNoSePuedeRepetirEnCreate() {
        // MATCH (a)-[r:KNOWS]->(b) CREATE (a)-[r:KNOWS]->(b)
        var query = new Query(
                match(path(node("a"), rel("r", Direction.OUT, "KNOWS"), node("b"))),
                Optional.empty(),
                List.of(new CreateClause(List.of(path(node("a"), rel("r", Direction.OUT, "KNOWS"), node("b"))))),
                Optional.empty());

        assertEquals("Relationship variable 'r' cannot be repeated in the same query", errorOf(query));
    }

    @Test
    void nodoNoPuedeUsarseComoRelacion() {
        // MATCH (a:N), (b)-[a]->(c) RETURN b
        var query = new Query(
                match(node("a", "N"), path(node("b"), rel("a", Direction.OUT), node("c"))),
                Optional.empty(), List.of(), returning(item(new Var("b"))));

        assertEquals("Variable 'a' is a node and cannot be used as a relationship", errorOf(query));
    }

    @Test
    void relacionNoPuedeUsarseComoNodo() {
        // MATCH (b)-[a]->(c), (a) RETURN b
        var query = new Query(
                match(path(node("b"), rel("a", Direction.OUT), node("c")), node("a")),
                Optional.empty(), List.of(), returning(item(new Var("b"))));

        assertEquals("Variable 'a' is a relationship and cannot be used as a node", errorOf(query));
    }

    @Test
    void variableDeNodoNoPuedeEmpezarConDobleGuion() {
        // MATCH (__a:Person) RETURN __a.name
        var query = new Query(match(node("__a", "Person")), Optional.empty(), List.of(),
                returning(item(prop("__a", "name"))));

        assertEquals("Variable '__a' cannot start with '__' (reserved for generated Prolog variables)", errorOf(query));
    }

    @Test
    void variableDeRelacionNoPuedeEmpezarConDobleGuion() {
        var query = new Query(match(path(node("a"), rel("__r", Direction.OUT), node("b"))),
                Optional.empty(), List.of(), returning(item(new Var("a"))));

        assertTrue(errorOf(query).startsWith("Variable '__r' cannot start with '__'"));
    }

    @Test
    void aliasNoPuedeEmpezarConDobleGuion() {
        var query = new Query(match(node("a")), Optional.empty(), List.of(),
                returning(item(prop("a", "name"), "__name")));

        assertTrue(errorOf(query).startsWith("Variable '__name' cannot start with '__'"));
    }

    @Test
    void unSoloGuionBajoSiSePermite() {
        var query = new Query(match(node("_a"), node("a_b")), Optional.empty(), List.of(),
                returning(item(new Var("_a"))));

        assertValid(query);
    }

    // ------------------------------------------------------------ negativos: variables no definidas

    @Test
    void setConVariableNoDefinida() {
        var query = new Query(match(node("p", "Product")), Optional.empty(),
                List.of(new SetClause(List.of(new SetProperty(prop("q", "precio"), new DecimalLit(1.5))))),
                Optional.empty());

        assertEquals("Variable 'q' is not defined in clause SET", errorOf(query));
    }

    @Test
    void setConValorQueUsaVariableNoDefinida() {
        var query = new Query(match(node("p", "Product")), Optional.empty(),
                List.of(new SetClause(List.of(new SetProperty(prop("p", "precio"), prop("x", "precio"))))),
                Optional.empty());

        assertEquals("Variable 'x' is not defined in clause SET", errorOf(query));
    }

    @Test
    void removeConVariableNoDefinida() {
        var query = new Query(match(node("p", "Product")), Optional.empty(),
                List.of(new RemoveClause(List.of(new RemoveProperty(prop("q", "discount"))))),
                Optional.empty());

        assertEquals("Variable 'q' is not defined in clause REMOVE", errorOf(query));
    }

    @Test
    void deleteConVariableNoDefinida() {
        var query = new Query(match(node("a")), Optional.empty(),
                List.of(new DeleteClause(true, List.of("r"))), Optional.empty());

        assertEquals("Variable 'r' is not defined in clause DELETE", errorOf(query));
    }

    @Test
    void whereSinMatch() {
        var query = new Query(Optional.empty(), where(new Binary(BinaryOp.EQ, new NumberLit(1), new NumberLit(1))),
                List.of(), Optional.empty());

        assertEquals("WHERE requires a MATCH clause", errorOf(query));
    }

    @Test
    void propiedadDeRelacionUsaVariableDeclaradaDespues() {
        // MATCH (a)-[r {since: b.year}]->(b): b todavia no existe cuando se leen las propiedades de r
        var r = new RelationshipPattern(Optional.of("r"), List.of(),
                List.of(entry("since", prop("b", "year"))), Direction.OUT);
        var query = new Query(match(path(node("a"), r, node("b"))), Optional.empty(), List.of(),
                returning(item(new Var("a"))));

        assertEquals("Variable 'b' is not defined in clause MATCH", errorOf(query));
    }

    // ------------------------------------------------------------ negativos: otras reglas

    @Test
    void createNecesitaDireccion() {
        var query = new Query(Optional.empty(), Optional.empty(),
                List.of(new CreateClause(List.of(path(node("a"), rel("r", Direction.NONE, "KNOWS"), node("b"))))),
                Optional.empty());

        assertEquals("CREATE needs a direction (-> or <-) for 'r'", errorOf(query));
    }

    @Test
    void createNecesitaUnSoloTipo() {
        var query = new Query(Optional.empty(), Optional.empty(),
                List.of(new CreateClause(List.of(path(node("a"), anonymousRel(Direction.OUT), node("b"))))),
                Optional.empty());

        assertEquals("CREATE needs exactly one type for 'relationship'", errorOf(query));
    }

    @Test
    void etiquetasSoloEnNodos() {
        var query = new Query(match(path(node("a"), rel("r", Direction.OUT, "KNOWS"), node("b"))),
                Optional.empty(),
                List.of(new SetClause(List.of(new SetLabels("r", List.of("Old"))))),
                Optional.empty());

        assertEquals("Labels can only be changed on nodes, but 'r' is a relationship (clause SET)", errorOf(query));
    }

    @Test
    void columnaRepetidaEnReturn() {
        var query = new Query(match(node("a"), node("b")), Optional.empty(), List.of(),
                returning(item(prop("a", "name"), "name"), item(prop("b", "name"), "name")));

        assertEquals("Column 'name' is defined more than once in RETURN", errorOf(query));
    }
}