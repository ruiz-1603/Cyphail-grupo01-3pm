package org.cyphail.ast;

import java.util.List;
import java.util.Optional;

/**
 * ASTs de P2 armados a mano (sin el parser), para tests del printer, del validador y del CodeGen.
 * Cada metodo dice el query Cyphail que representa.
 */
public final class P2Asts {

    private P2Asts() {}

    // ------------------------------------------------------------ helpers

    public static PatternNode node(String variable, String... labels) {
        return new PatternNode(variable, List.of(labels), List.of());
    }

    public static PatternNode node(String variable, List<String> labels, PropertyEntry... properties) {
        return new PatternNode(variable, labels, List.of(properties));
    }

    public static PatternNode anonymousNode(String... labels) {
        return new PatternNode(Optional.empty(), List.of(labels), List.of());
    }

    public static RelationshipPattern rel(String variable, Direction direction, String... types) {
        return new RelationshipPattern(Optional.of(variable), List.of(types), List.of(), direction);
    }

    public static RelationshipPattern anonymousRel(Direction direction, String... types) {
        return new RelationshipPattern(Optional.empty(), List.of(types), List.of(), direction);
    }

    public static PathPattern path(PatternNode left, RelationshipPattern rel, PatternNode right) {
        return new PathPattern(left, rel, right);
    }

    public static PropertyEntry entry(String key, Expr value) {
        return new PropertyEntry(key, value);
    }

    public static PropertyAccess prop(String variable, String property) {
        return new PropertyAccess(variable, property);
    }

    public static ProjectionItem item(Expr expr) {
        return new ProjectionItem(expr, Optional.empty());
    }

    public static ProjectionItem item(Expr expr, String alias) {
        return new ProjectionItem(expr, Optional.of(alias));
    }

    public static Optional<ReturnClause> returning(ProjectionItem... items) {
        return Optional.of(new ReturnClause(new Projection(List.of(items), List.of())));
    }

    public static Optional<MatchClause> match(PatternElement... patterns) {
        return Optional.of(new MatchClause(List.of(patterns)));
    }

    public static Optional<WhereClause> where(Expr condition) {
        return Optional.of(new WhereClause(condition));
    }

    // ------------------------------------------------------------ ejemplos del SPEC P2

    /**
     * MATCH (p:Persona)-[:TRABAJA_EN]->(e:Empresa),
     *       (c:Persona)-[:TRABAJA_EN]->(e)
     * WHERE p.nombre <> c.nombre
     * RETURN p.nombre, c.nombre, e.nombre
     */
    public static Query trabajaEn() {
        return new Query(
                match(path(node("p", "Persona"), anonymousRel(Direction.OUT, "TRABAJA_EN"), node("e", "Empresa")),
                        path(node("c", "Persona"), anonymousRel(Direction.OUT, "TRABAJA_EN"), node("e"))),
                where(new Binary(BinaryOp.NEQ, prop("p", "nombre"), prop("c", "nombre"))),
                List.of(),
                returning(item(prop("p", "nombre")), item(prop("c", "nombre")), item(prop("e", "nombre"))));
    }

    /**
     * CREATE (a:Persona {nombre: 'Marta'})-[r:TRABAJA_EN {rol: 'Dev'}]->(e:Empresa {nombre: 'UCR'})
     * RETURN a, r, e
     */
    public static Query createSinMatch() {
        var a = node("a", List.of("Persona"), entry("nombre", new StringLit("Marta")));
        var r = new RelationshipPattern(Optional.of("r"), List.of("TRABAJA_EN"),
                List.of(entry("rol", new StringLit("Dev"))), Direction.OUT);
        var e = node("e", List.of("Empresa"), entry("nombre", new StringLit("UCR")));
        return new Query(
                Optional.empty(),
                Optional.empty(),
                List.of(new CreateClause(List.of(path(a, r, e)))),
                returning(item(new Var("a")), item(new Var("r")), item(new Var("e"))));
    }

    /**
     * MATCH (a:Person:Female)-[r:KNOWS]->(b:Person:Male)
     * WHERE r.since < 2000
     * DELETE r
     */
    public static Query deleteRelacion() {
        return new Query(
                match(path(node("a", "Person", "Female"), rel("r", Direction.OUT, "KNOWS"), node("b", "Person", "Male"))),
                where(new Binary(BinaryOp.LT, prop("r", "since"), new NumberLit(2000))),
                List.of(new DeleteClause(List.of("r"))),
                Optional.empty());
    }

    /**
     * MATCH (p:Product {code: "PROD-99"})
     * SET p.precio = 19.99
     * RETURN p
     */
    public static Query setPrecio() {
        return new Query(
                match(node("p", List.of("Product"), entry("code", new StringLit("PROD-99")))),
                Optional.empty(),
                List.of(new SetClause(List.of(new SetProperty(prop("p", "precio"), new DecimalLit(19.99))))),
                returning(item(new Var("p"))));
    }

    /**
     * MATCH (p:Product {code: "PROD-99"})
     * REMOVE p.discount
     * RETURN p.id
     */
    public static Query removeDescuento() {
        return new Query(
                match(node("p", List.of("Product"), entry("code", new StringLit("PROD-99")))),
                Optional.empty(),
                List.of(new RemoveClause(List.of(new RemoveProperty(prop("p", "discount"))))),
                returning(item(prop("p", "id"))));
    }

    /**
     * MATCH (u:Student {id: "4-666-000"})-[r:attends]->(c:Course {nrc: "50691"})
     * SET r.grade = 9.5
     * RETURN u.id, r.grade
     */
    public static Query setNota() {
        var u = node("u", List.of("Student"), entry("id", new StringLit("4-666-000")));
        var c = node("c", List.of("Course"), entry("nrc", new StringLit("50691")));
        return new Query(
                match(path(u, rel("r", Direction.OUT, "attends"), c)),
                Optional.empty(),
                List.of(new SetClause(List.of(new SetProperty(prop("r", "grade"), new DecimalLit(9.5))))),
                returning(item(prop("u", "id")), item(prop("r", "grade"))));
    }

    /**
     * MATCH (a:Autor {nombre: 'Bill Gates'}), (b:Libro {titulo: 'Fake Windows Millenium'})
     * CREATE (a)-[:ESCRIBIO]->(b)
     */
    public static Query matchCreate() {
        return new Query(
                match(node("a", List.of("Autor"), entry("nombre", new StringLit("Bill Gates"))),
                        node("b", List.of("Libro"), entry("titulo", new StringLit("Fake Windows Millenium")))),
                Optional.empty(),
                List.of(new CreateClause(List.of(path(node("a"), anonymousRel(Direction.OUT, "ESCRIBIO"), node("b"))))),
                Optional.empty());
    }
}