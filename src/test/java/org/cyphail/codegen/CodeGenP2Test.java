package org.cyphail.codegen;

import org.cyphail.ast.*;
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
 * CodeGen P2: relaciones (OUT, IN, NONE), elementos anonimos, propiedades de relacion,
 * unicidad de arcos, AND/OR/NOT y literales nuevos. Los ASTs se arman a mano (P2Asts).
 */
class CodeGenP2Test {

    private static NumberLit num(long value) {
        return new NumberLit(value);
    }

    private static Binary cmp(BinaryOp op, Expr left, Expr right) {
        return new Binary(op, left, right);
    }

    // Ejemplo "CodeGen como Transpilador" del SPEC P2 (grafo amigos)
    @Test
    void ejemploDelSpecAmigosMutuos() {
        var query = new Query(
                match(path(node("a"), anonymousRel(Direction.OUT, "amigo"), node("b")),
                        path(node("b"), anonymousRel(Direction.OUT, "amigo"), node("a"))),
                where(new And(cmp(BinaryOp.GT, prop("a", "age"), num(18)),
                        cmp(BinaryOp.EQ, prop("a", "age"), prop("b", "age")))),
                List.of(),
                returning(item(prop("a", "name")), item(prop("b", "name"))));

        var gen = CodeGen.generate(query, "amigos");

        assertEquals("cy_row(__A_name, __B_name)", gen.head());
        assertEquals(List.of(
                "user_amigos_edge(__E1, amigo, __A, __B)",
                "user_amigos_edge(__E2, amigo, __B, __A)",
                "cy_neq(__E1, __E2)",
                "user_amigos_prop(__A, age, __A_age)",
                "cy_gt(__A_age, 18)",
                "user_amigos_prop(__B, age, __B_age)",
                "cy_eq(__A_age, __B_age)",
                "user_amigos_prop(__A, name, __A_name)",
                "user_amigos_prop(__B, name, __B_name)"), gen.body());
        assertEquals(List.of("a.name", "b.name"), gen.columns());
    }

    // (e)<-[r]-(p): el arco va de p hacia e
    @Test
    void direccionInversaInvierteLosExtremos() {
        var query = new Query(
                match(path(node("e", "Empresa"), rel("r", Direction.IN, "TRABAJA_EN"), node("p", "Persona"))),
                Optional.empty(), List.of(), returning(item(prop("p", "nombre")), item(new Var("r"))));

        var gen = CodeGen.generate(query, "empresa");

        assertEquals("cy_row(__P_nombre, __R)", gen.head());
        assertEquals(List.of(
                "user_empresa_edge(__R, 'TRABAJA_EN', __P, __E)",
                "user_empresa_label(__E, 'Empresa')",
                "user_empresa_label(__P, 'Persona')",
                "user_empresa_prop(__P, nombre, __P_nombre)"), gen.body());
    }

    @Test
    void sinDireccionAceptaCualquierSentido() {
        var query = new Query(
                match(path(node("a"), rel("r", Direction.NONE, "CONOCE"), node("b"))),
                Optional.empty(), List.of(), returning(item(new Var("r"))));

        var gen = CodeGen.generate(query, "empresa");

        assertEquals(List.of(
                "(user_empresa_edge(__R, 'CONOCE', __A, __B) ; user_empresa_edge(__R, 'CONOCE', __B, __A))"),
                gen.body());
    }

    @Test
    void relacionSinTipoUsaVariableAnonima() {
        var query = new Query(
                match(path(node("a"), new RelationshipPattern(Optional.of("r"), List.of(), List.of(), Direction.OUT), node("b"))),
                Optional.empty(), List.of(), returning(item(new Var("r"))));

        assertEquals(List.of("user_amigos_edge(__R, _, __A, __B)"), CodeGen.generate(query, "amigos").body());
    }

    // Propiedades inline de la relacion y r.prop en el RETURN reutilizan el mismo calculo
    @Test
    void propiedadesDeRelacion() {
        var r = new RelationshipPattern(Optional.of("r"), List.of("CONOCE"),
                List.of(entry("desde", num(2015))), Direction.OUT);
        var query = new Query(match(path(node("a"), r, node("b"))),
                Optional.empty(), List.of(), returning(item(prop("r", "desde"))));

        var gen = CodeGen.generate(query, "empresa");

        assertEquals("cy_row(__R_desde)", gen.head());
        assertEquals(List.of(
                "user_empresa_edge(__R, 'CONOCE', __A, __B)",
                "user_empresa_prop(__R, desde, __R_desde)",
                "cy_eq(__R_desde, 2015)"), gen.body());
    }

    @Test
    void nodoYRelacionAnonimosRecibenNombresFrescos() {
        var query = new Query(
                match(path(node("a"), anonymousRel(Direction.OUT, "CONOCE"),
                        new PatternNode(Optional.empty(), List.of("Persona"), List.of(entry("nombre", new StringLit("Jose")))))),
                Optional.empty(), List.of(), returning(item(prop("a", "nombre"))));

        var gen = CodeGen.generate(query, "empresa");

        assertEquals(List.of(
                "user_empresa_edge(__E1, 'CONOCE', __A, __N1)",
                "user_empresa_label(__N1, 'Persona')",
                "user_empresa_prop(__N1, nombre, __V1)",
                "cy_eq(__V1, 'Jose')",
                "user_empresa_prop(__A, nombre, __A_nombre)"), gen.body());
    }

    // Casos P2, "unicidad de relaciones": tres arcos -> tres pares distintos
    @Test
    void relacionesDistintasDentroDelMatch() {
        var query = new Query(
                match(path(node("a"), rel("r", Direction.OUT, "X"), node("b")),
                        path(node("b"), rel("s", Direction.OUT, "X"), node("c")),
                        path(node("c"), rel("t", Direction.OUT, "X"), node("a"))),
                Optional.empty(), List.of(), returning(item(new Var("a"))));

        var body = CodeGen.generate(query, "g").body();

        assertEquals(List.of("cy_neq(__R, __S)", "cy_neq(__R, __T)", "cy_neq(__S, __T)"),
                body.subList(body.size() - 3, body.size()));
    }

    // OR: cada rama pide sus propiedades; once(...) para que una fila no salga dos veces
    @Test
    void orYNot() {
        var query = new Query(
                match(path(node("p"), rel("t", Direction.OUT, "TRABAJA_EN"), node("e"))),
                where(new Or(cmp(BinaryOp.GTE, prop("t", "desde"), num(2018)),
                        new Not(cmp(BinaryOp.EQ, prop("e", "pais"), new StringLit("CR"))))),
                List.of(), returning(item(prop("t", "desde"))));

        var gen = CodeGen.generate(query, "empresa");

        assertEquals(List.of(
                "user_empresa_edge(__T, 'TRABAJA_EN', __P, __E)",
                "once((user_empresa_prop(__T, desde, __T_desde), cy_ge(__T_desde, 2018) ; "
                        + "\\+ (user_empresa_prop(__E, pais, __E_pais), cy_eq(__E_pais, 'CR'))))",
                "user_empresa_prop(__T, desde, __T_desde)"), gen.body());
    }

    @Test
    void booleanosYDecimales() {
        var query = new Query(
                match(node("p", List.of("Persona"), entry("activo", new BoolLit(true)))),
                where(cmp(BinaryOp.GT, prop("p", "edad"), new DecimalLit(25.5))),
                List.of(), returning(item(new Var("p"))));

        assertEquals(List.of(
                "user_empresa_node(__P)",
                "user_empresa_label(__P, 'Persona')",
                "user_empresa_prop(__P, activo, __P_activo)",
                "cy_eq(__P_activo, true)",
                "user_empresa_prop(__P, edad, __P_edad)",
                "cy_gt(__P_edad, 25.5)"), CodeGen.generate(query, "empresa").body());
    }

    // ------------------------------------------------------------ actualizaciones

    // CASO 8: CREATE sin MATCH -> dos nodos nuevos y la relacion entre ellos
    @Test
    void createSinMatchCreaNodosYRelacion() {
        var gen = CodeGen.generate(createSinMatch(), "empresa");

        assertEquals("cy_row(__A, __R, __E)", gen.head());
        assertEquals(List.of(
                "cy_new_node(empresa, __A)",
                "cy_add_label(empresa, __A, 'Persona')",
                "cy_set_prop(empresa, __A, nombre, 'Marta')",
                "cy_new_node(empresa, __E)",
                "cy_add_label(empresa, __E, 'Empresa')",
                "cy_set_prop(empresa, __E, nombre, 'UCR')",
                "cy_new_edge(empresa, __R, 'TRABAJA_EN', __A, __E)",
                "cy_set_prop(empresa, __R, rol, 'Dev')"), gen.body());
        assertEquals(List.of("a", "r", "e"), gen.columns());
    }

    // CASO 7: CREATE (a)-[:ESCRIBIO]->(b) reutiliza a y b del MATCH (no crea nodos)
    @Test
    void createReutilizaVariablesDelMatch() {
        var body = CodeGen.generate(matchCreate(), "g").body();

        assertEquals("cy_new_edge(g, __E1, 'ESCRIBIO', __A, __B)", body.getLast());
        assertTrue(body.stream().noneMatch(goal -> goal.startsWith("cy_new_node")));
    }

    // (a)<-[:T]-(b) en CREATE: el arco va de b hacia a
    @Test
    void createConDireccionInversa() {
        var query = new Query(match(node("a"), node("b")), Optional.empty(),
                List.of(new CreateClause(List.of(path(node("a"), anonymousRel(Direction.IN, "T"), node("b"))))),
                Optional.empty());

        assertEquals("cy_new_edge(g, __E1, 'T', __B, __A)", CodeGen.generate(query, "g").body().getLast());
    }

    @Test
    void createExigeDireccionYUnTipo() {
        var sinDireccion = new Query(match(node("a"), node("b")), Optional.empty(),
                List.of(new CreateClause(List.of(path(node("a"), anonymousRel(Direction.NONE, "T"), node("b"))))),
                Optional.empty());
        assertEquals("A relationship in CREATE must have a direction (-> or <-)",
                assertThrows(CodeGenException.class, () -> CodeGen.generate(sinDireccion, "g")).getMessage());

        var sinTipo = new Query(match(node("a"), node("b")), Optional.empty(),
                List.of(new CreateClause(List.of(path(node("a"), anonymousRel(Direction.OUT), node("b"))))),
                Optional.empty());
        assertEquals("A relationship in CREATE must have exactly one type",
                assertThrows(CodeGenException.class, () -> CodeGen.generate(sinTipo, "g")).getMessage());
    }

    @Test
    void createNoAgregaEtiquetasAVariableExistente() {
        var query = new Query(match(node("a", "Persona")), Optional.empty(),
                List.of(new CreateClause(List.of(node("a", "Empresa")))), Optional.empty());

        var error = assertThrows(CodeGenException.class, () -> CodeGen.generate(query, "g"));
        assertEquals("Variable 'a' is already declared: CREATE cannot add labels or properties to it (use SET)",
                error.getMessage());
    }

    // CASO 9: DELETE r de una relacion despues del WHERE
    @Test
    void deleteDeRelacion() {
        var body = CodeGen.generate(deleteRelacion(), "amigos").body();

        assertEquals(List.of("user_amigos_prop(__R, since, __R_since)", "cy_lt(__R_since, 2000)", "cy_delete(amigos, __R)"),
                body.subList(body.size() - 3, body.size()));
    }

    // SET r.grade = 9.5 ... RETURN r.grade: el RETURN vuelve a leer la propiedad (valor nuevo)
    @Test
    void setYLuegoReturnLeeElValorNuevo() {
        var gen = CodeGen.generate(setNota(), "revision");

        assertEquals("cy_row(__U_id, __R_grade)", gen.head());
        assertEquals(List.of(
                "cy_set_prop(revision, __R, grade, 9.5)",
                "user_revision_prop(__R, grade, __R_grade)"),
                gen.body().subList(gen.body().size() - 2, gen.body().size()));
    }

    // Si la propiedad ya se habia pedido antes del SET, despues se pide con otro nombre
    @Test
    void setOlvidaElCalculoAnterior() {
        var query = new Query(match(node("p", "Persona")),
                where(cmp(BinaryOp.GT, prop("p", "edad"), num(30))),
                List.of(new SetClause(List.of(new SetProperty(prop("p", "edad"), num(31))))),
                returning(item(prop("p", "edad"))));

        var gen = CodeGen.generate(query, "empresa");

        assertEquals("cy_row(__P_edad_2)", gen.head());
        assertTrue(gen.body().contains("cy_set_prop(empresa, __P, edad, 31)"));
        assertEquals("user_empresa_prop(__P, edad, __P_edad_2)", gen.body().getLast());
    }

    @Test
    void setDeEtiquetas() {
        var query = new Query(match(node("p", "Product")), Optional.empty(),
                List.of(new SetClause(List.of(new SetLabels("p", List.of("Oferta", "Nuevo"))))), Optional.empty());

        var body = CodeGen.generate(query, "g").body();

        assertEquals(List.of("cy_add_label(g, __P, 'Oferta')", "cy_add_label(g, __P, 'Nuevo')"),
                body.subList(body.size() - 2, body.size()));
    }

    // CASO 12 (Guia P2): REMOVE de propiedades y de una etiqueta
    @Test
    void removeDePropiedadesYEtiqueta() {
        var query = new Query(
                match(node("p", List.of("Product"), entry("code", new StringLit("PROD-99")))),
                Optional.empty(),
                List.of(new RemoveClause(List.of(
                        new RemoveProperty(prop("p", "discount")),
                        new RemoveProperty(prop("p", "promo")),
                        new RemoveLabels("p", List.of("Oferta"))))),
                returning(item(prop("p", "id"))));

        var gen = CodeGen.generate(query, "revision");

        assertEquals(List.of(
                "cy_remove_prop(revision, __P, discount)",
                "cy_remove_prop(revision, __P, promo)",
                "cy_remove_label(revision, __P, 'Oferta')",
                "user_revision_prop(__P, id, __P_id)"),
                gen.body().subList(gen.body().size() - 4, gen.body().size()));
        assertTrue(gen.updates());
    }

    // REMOVE p.discount RETURN p.id: el REMOVE va antes de los goals del RETURN
    @Test
    void removeAntesDelReturn() {
        var gen = CodeGen.generate(removeDescuento(), "g");
        assertEquals("cy_remove_prop(g, __P, discount)", gen.body().get(gen.body().size() - 2));
    }

    @Test
    void consultaDeLecturaNoCuentaCambios() {
        var gen = CodeGen.generate(trabajaEn(), "empresa");
        assertFalse(gen.updates());
        assertFalse(gen.goal().contains("cy_stats"));
    }
}
