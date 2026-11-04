package org.cyphail.parser;

import org.cyphail.ast.*;
import org.cyphail.parser.core.Fail;
import org.cyphail.parser.core.InputString;
import org.cyphail.parser.core.Result;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.cyphail.ast.P2Asts.*;
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
 * Pruebas del parser para Sprint P2:
 * 1) los ejemplos del SPEC P2 deben producir exactamente los ASTs de P2Asts (hechos a mano);
 * 2) los casos de "EIF400-II-2026-Cypahil-P2-10-Casos_Prueba_Unitarios.md" (CASO 1..10, 8b, 8c);
 * 3) lo que esta fuera del sprint (patrones encadenados, tipos alternativos) no se acepta.
 */
class CyphailParserP2Test {

    private static void assertParses(Query expected, String text) {
        assertEquals(Result.ok(expected, new InputString(text, text.length())), CyphailParser.parse(text));
    }

    private static NumberLit num(long value) {
        return new NumberLit(value);
    }

    private static StringLit str(String value) {
        return new StringLit(value);
    }

    private static Binary cmp(BinaryOp op, Expr left, Expr right) {
        return new Binary(op, left, right);
    }

    private static RelationshipPattern rel(String variable, Direction direction, String type, PropertyEntry... props) {
        return new RelationshipPattern(Optional.of(variable), List.of(type), List.of(props), direction);
    }

    // ------------------------------------------------------------ ejemplos del SPEC P2

    @Test
    void specRelacionesConVariableDeJoin() {
        assertParses(trabajaEn(), """
                MATCH (p:Persona)-[:TRABAJA_EN]->(e:Empresa),
                      (c:Persona)-[:TRABAJA_EN]->(e)
                WHERE p.nombre <> c.nombre
                RETURN p.nombre, c.nombre, e.nombre""");
    }

    @Test
    void specCreateSinMatch() {
        assertParses(createSinMatch(), """
                CREATE (a:Persona {nombre: 'Marta'})
                    -[r:TRABAJA_EN {rol: 'Dev'}]->(e:Empresa
                                                {nombre: 'UCR'})
                RETURN a, r, e""");
    }

    @Test
    void specMatchCreate() {
        assertParses(matchCreate(), """
                MATCH (a:Autor {nombre: 'Bill Gates'}),
                      (b:Libro {titulo: 'Fake Windows Millenium'})
                CREATE (a)-[:ESCRIBIO]->(b)""");
    }

    @Test
    void specDeleteRelacion() {
        assertParses(deleteRelacion(), """
                MATCH (a:Person:Female)-[r:KNOWS]->(b:Person:Male)
                WHERE r.since < 2000
                DELETE r""");
    }

    @Test
    void specSetConDecimal() {
        assertParses(setPrecio(), """
                MATCH (p:Product {code: "PROD-99"})
                SET p.precio = 19.99
                RETURN p""");
    }

    @Test
    void specRemove() {
        assertParses(removeDescuento(), """
                MATCH (p:Product {code: "PROD-99"})
                REMOVE p.discount
                RETURN p.id""");
    }

    @Test
    void specSetSobreRelacion() {
        assertParses(setNota(), """
                MATCH
                (u:Student {id: "4-666-000"})
                -[r:attends]->
                (c:Course {nrc: "50691"})
                SET r.grade = 9.5
                RETURN u.id, r.grade""");
    }

    // ------------------------------------------------------------ casos de referencia P2

    @Test
    void caso1RelacionSinTipo() {
        var r = new RelationshipPattern(Optional.of("r"), List.of(), List.of(), Direction.OUT);
        assertParses(new Query(match(path(node("p", "Person"), r, node("q", "Person"))),
                        Optional.empty(), List.of(), returning(item(new Var("r")))),
                """
                // Ejercita: sintaxis basica -[var]->
                MATCH (p:Person)-[r]->(q:Person)
                RETURN r""");
    }

    @Test
    void caso2RelacionSinVariable() {
        assertParses(new Query(
                        match(path(node("a", "Persona"), anonymousRel(Direction.OUT, "CONOCE"), node("b", "Persona"))),
                        Optional.empty(), List.of(), returning(item(prop("a", "nombre")), item(prop("b", "nombre")))),
                """
                MATCH (a:Persona)-[:CONOCE]->(b:Persona)
                RETURN a.nombre, b.nombre""");
    }

    @Test
    void caso3DireccionInversa() {
        assertParses(new Query(
                        match(path(node("e", "Empresa"), rel("r", Direction.IN, "TRABAJA_EN"), node("p", "Persona"))),
                        Optional.empty(), List.of(), returning(item(prop("p", "nombre")), item(prop("e", "nombre")))),
                """
                MATCH (e:Empresa)<-[r:TRABAJA_EN]-(p:Persona)
                RETURN p.nombre, e.nombre""");
    }

    @Test
    void caso3SinDireccion() {
        assertParses(new Query(
                        match(path(node("a", "Persona"), rel("r", Direction.NONE, "CONOCE"), node("b", "Persona"))),
                        Optional.empty(), List.of(), returning(item(prop("a", "nombre")), item(prop("b", "nombre")))),
                """
                MATCH (a:Persona)-[r:CONOCE]-(b:Persona)
                RETURN a.nombre, b.nombre""");
    }

    @Test
    void caso4PropiedadesDeRelacion() {
        assertParses(new Query(
                        match(path(node("a", "Persona"), rel("r", Direction.OUT, "CONOCE", entry("desde", num(2015))),
                                node("b", "Persona"))),
                        Optional.empty(), List.of(),
                        returning(item(prop("a", "nombre")), item(prop("b", "nombre")), item(prop("r", "desde")))),
                """
                MATCH (a:Persona)-[r:CONOCE {desde: 2015}]->(b:Persona)
                RETURN a.nombre, b.nombre, r.desde""");
    }

    @Test
    void caso5WhereConAnd() {
        assertParses(new Query(
                        match(path(node("a", "Persona"), rel("r", Direction.OUT, "CONOCE"), node("b", "Persona"))),
                        where(new And(cmp(BinaryOp.GT, prop("r", "desde"), num(2010)),
                                cmp(BinaryOp.GTE, prop("b", "edad"), num(30)))),
                        List.of(),
                        returning(item(prop("a", "nombre")), item(prop("b", "nombre")), item(prop("r", "desde")))),
                """
                MATCH (a:Persona)-[r:CONOCE]->(b:Persona)
                WHERE r.desde > 2010 AND b.edad >= 30
                RETURN a.nombre, b.nombre, r.desde""");
    }

    @Test
    void caso6OrNotYComillasSimples() {
        assertParses(new Query(
                        match(path(node("p", "Persona"), rel("t", Direction.OUT, "TRABAJA_EN", entry("cargo", str("Dev"))),
                                node("e", "Empresa"))),
                        where(new Or(cmp(BinaryOp.GTE, prop("t", "desde"), num(2018)),
                                new Not(cmp(BinaryOp.EQ, prop("e", "pais"), str("CR"))))),
                        List.of(),
                        returning(item(prop("p", "nombre")), item(prop("e", "nombre")),
                                item(prop("t", "cargo")), item(prop("t", "desde")))),
                """
                MATCH (p:Persona)-[t:TRABAJA_EN {cargo: 'Dev'}]->(e:Empresa)
                WHERE t.desde >= 2018 OR NOT e.pais = 'CR'
                RETURN p.nombre, e.nombre, t.cargo, t.desde""");
    }

    @Test
    void caso7MatchCreateSinReturn() {
        var conoce = new RelationshipPattern(Optional.empty(), List.of("CONOCE"),
                List.of(entry("desde", num(2020)), entry("nivel", str("amigo"))), Direction.OUT);
        assertParses(new Query(
                        match(node("a", List.of("Persona"), entry("nombre", str("Ana"))),
                                node("b", List.of("Persona"), entry("nombre", str("Luis")))),
                        Optional.empty(),
                        List.of(new CreateClause(List.of(path(node("a"), conoce, node("b"))))),
                        Optional.empty()),
                """
                MATCH (a:Persona {nombre: 'Ana'}), (b:Persona {nombre: 'Luis'})
                CREATE (a)-[:CONOCE {desde: 2020, nivel: 'amigo'}]->(b)""");
    }

    @Test
    void caso8CreateConBooleano() {
        var a = node("a", List.of("Persona"), entry("nombre", str("Marta")));
        var r = rel("r", Direction.OUT, "TRABAJA_EN", entry("cargo", str("Dev")), entry("activo", new BoolLit(true)));
        var e = node("e", List.of("Empresa"), entry("nombre", str("Acme")));
        assertParses(new Query(Optional.empty(), Optional.empty(),
                        List.of(new CreateClause(List.of(path(a, r, e)))),
                        returning(item(new Var("a")), item(new Var("r")), item(new Var("e")))),
                """
                CREATE (a:Persona {nombre: 'Marta'})-[r:TRABAJA_EN {cargo: 'Dev', activo: true}]->(e:Empresa {nombre: 'Acme'})
                RETURN a, r, e""");
    }

    @Test
    void caso9DeleteDeRelacion() {
        assertParses(new Query(
                        match(path(node("a", "Persona"), rel("r", Direction.OUT, "CONOCE"), node("b", "Persona"))),
                        where(cmp(BinaryOp.LT, prop("r", "desde"), num(2000))),
                        List.of(new DeleteClause(List.of("r"))),
                        Optional.empty()),
                """
                MATCH (a:Persona)-[r:CONOCE]->(b:Persona)
                WHERE r.desde < 2000
                DELETE r""");
    }

    @Test
    void caso10Integrador() {
        var p = node("p", List.of("Persona"), entry("activo", new BoolLit(true)));
        var t = rel("t", Direction.OUT, "TRABAJA_EN", entry("cargo", str("Dev")));
        var condition = new And(
                new Or(cmp(BinaryOp.GTE, prop("t", "desde"), num(2018)),
                        cmp(BinaryOp.GT, prop("s", "porcentaje"), new DecimalLit(25.5))),
                new Not(cmp(BinaryOp.EQ, prop("k", "desde"), num(2020))));
        assertParses(new Query(
                        match(path(p, t, node("e", "Empresa")),
                                path(node("q", "Persona"), rel("s", Direction.OUT, "SOCIO_DE"), node("e")),
                                path(node("p"), rel("k", Direction.NONE, "CONOCE"), node("q"))),
                        where(condition),
                        List.of(),
                        returning(item(prop("p", "nombre")), item(prop("q", "nombre")), item(prop("e", "nombre")),
                                item(prop("t", "cargo")), item(prop("s", "porcentaje")), item(prop("k", "desde")))),
                """
                MATCH (p:Persona {activo: true})-[t:TRABAJA_EN {cargo: 'Dev'}]->(e:Empresa),
                      (q:Persona)-[s:SOCIO_DE]->(e),
                      (p)-[k:CONOCE]-(q)
                WHERE (t.desde >= 2018 OR s.porcentaje > 25.5) AND NOT k.desde = 2020
                RETURN p.nombre, q.nombre, e.nombre, t.cargo, s.porcentaje, k.desde""");
    }

    @Test
    void caso8cPatronesDesconectadosSinEtiquetas() {
        var r = new RelationshipPattern(Optional.of("r"), List.of(), List.of(), Direction.OUT);
        var s = new RelationshipPattern(Optional.of("s"), List.of(), List.of(), Direction.OUT);
        assertParses(new Query(match(path(node("a"), r, node("b")), path(node("c"), s, node("d"))),
                        Optional.empty(), List.of(), returning(item(new Var("r")), item(new Var("s")))),
                """
                MATCH (a)-[r]->(b), (c)-[s]->(d)
                RETURN r, s""");
    }

    // ------------------------------------------------------------ precedencia y otros detalles

    @Test
    void andTieneMasPrecedenciaQueOr() {
        // a OR b AND c  =  a OR (b AND c)
        var a = cmp(BinaryOp.EQ, prop("x", "a"), num(1));
        var b = cmp(BinaryOp.EQ, prop("x", "b"), num(2));
        var c = cmp(BinaryOp.EQ, prop("x", "c"), num(3));
        assertParses(new Query(match(node("x", "N")), where(new Or(a, new And(b, c))), List.of(),
                        returning(item(new Var("x")))),
                "MATCH (x:N) WHERE x.a = 1 OR x.b = 2 AND x.c = 3 RETURN x");
    }

    @Test
    void nodoAnonimoConEtiqueta() {
        assertParses(new Query(
                        match(path(node("p", "Person"), anonymousRel(Direction.OUT, "MANAGES"), anonymousNode("Person"))),
                        Optional.empty(), List.of(), returning(item(prop("p", "name")))),
                "MATCH (p:Person)-[:MANAGES]->(:Person) RETURN p.name");
    }

    @Test
    void setYRemoveDeEtiquetas() {
        assertParses(new Query(match(node("p", "Person")), Optional.empty(),
                        List.of(new SetClause(List.of(new SetLabels("p", List.of("Vip")),
                                        new SetProperty(prop("p", "activo"), new BoolLit(false)))),
                                new RemoveClause(List.of(new RemoveLabels("p", List.of("Old", "Temp"))))),
                        returning(item(new Var("p")))),
                "MATCH (p:Person) SET p:Vip, p.activo = FALSE REMOVE p:Old:Temp RETURN p");
    }

    // ------------------------------------------------------------ fuera del sprint y errores

    @Test
    void patronesEncadenadosNoSeAceptan() {
        assertInstanceOf(Fail.class, CyphailParser.parse("MATCH (a)-[r]->(b)-[s]->(c) RETURN a"));
    }

    @Test
    void tiposAlternativosNoSeAceptan() {
        assertInstanceOf(Fail.class, CyphailParser.parse("MATCH (a)-[r:A|B]->(b) RETURN a"));
    }

    @Test
    void whereMalFormadoReportaSuError() {
        // Como WHERE esta presente, su error se reporta (no se ignora como si WHERE fuera opcional)
        var result = CyphailParser.parse("MATCH (a:Person) WHERE a.age >");
        assertInstanceOf(Fail.class, result);
        assertTrue(((Fail<InputString, Query, String>) result).reason().contains("at position 30"));
    }

    @Test
    void queryVacioNoEsValido() {
        assertEquals(Result.fail("Expected 'MATCH', 'CREATE' or 'RETURN' at position 0"), CyphailParser.parse("   "));
    }
}
