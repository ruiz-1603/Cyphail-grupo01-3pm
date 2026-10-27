package org.cyphail.ast;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.cyphail.ast.P2Asts.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AstPrinterP2Test {

    @Test
    void imprimeRelacionesAnonimasConDireccion() {
        assertEquals("""
                Query{
                  Match: {
                    Patterns: [
                      Path: {
                        PatternNode: {
                          var: p
                          labels: [ Persona ]
                          properties: []
                        }
                        Relationship: {
                          var: _
                          direction: OUT
                          types: [ TRABAJA_EN ]
                          properties: []
                        }
                        PatternNode: {
                          var: e
                          labels: [ Empresa ]
                          properties: []
                        }
                      }
                      Path: {
                        PatternNode: {
                          var: c
                          labels: [ Persona ]
                          properties: []
                        }
                        Relationship: {
                          var: _
                          direction: OUT
                          types: [ TRABAJA_EN ]
                          properties: []
                        }
                        PatternNode: {
                          var: e
                          labels: [  ]
                          properties: []
                        }
                      }
                    ]
                  }
                  Where: {
                    Expr: (<> (. p nombre) (. c nombre))
                  }
                  Updates: []
                  Return: {
                    Projection: {
                      Items: [
                        {(. p nombre)}
                        {(. c nombre)}
                        {(. e nombre)}
                      ]
                      Modifiers: []
                    }
                  }
                }""", AstPrinter.print(trabajaEn()));
    }

    @Test
    void imprimeCreateSinMatchConPropiedadesDeRelacion() {
        assertEquals("""
                Query{
                  Match: {}
                  Where: {}
                  Updates: [
                    Create: {
                      Patterns: [
                        Path: {
                          PatternNode: {
                            var: a
                            labels: [ Persona ]
                            properties: [nombre: "Marta"]
                          }
                          Relationship: {
                            var: r
                            direction: OUT
                            types: [ TRABAJA_EN ]
                            properties: [rol: "Dev"]
                          }
                          PatternNode: {
                            var: e
                            labels: [ Empresa ]
                            properties: [nombre: "UCR"]
                          }
                        }
                      ]
                    }
                  ]
                  Return: {
                    Projection: {
                      Items: [
                        {a}
                        {r}
                        {e}
                      ]
                      Modifiers: []
                    }
                  }
                }""", AstPrinter.print(createSinMatch()));
    }

    @Test
    void imprimeDeleteSinReturn() {
        assertEquals("""
                Query{
                  Match: {
                    Patterns: [
                      Path: {
                        PatternNode: {
                          var: a
                          labels: [ Person, Female ]
                          properties: []
                        }
                        Relationship: {
                          var: r
                          direction: OUT
                          types: [ KNOWS ]
                          properties: []
                        }
                        PatternNode: {
                          var: b
                          labels: [ Person, Male ]
                          properties: []
                        }
                      }
                    ]
                  }
                  Where: {
                    Expr: (< (. r since) 2000)
                  }
                  Updates: [
                    Delete: [ r ]
                  ]
                  Return: {}
                }""", AstPrinter.print(deleteRelacion()));
    }

    @Test
    void imprimeSetConDecimal() {
        assertEquals("""
                Query{
                  Match: {
                    Patterns: [
                      PatternNode: {
                        var: p
                        labels: [ Product ]
                        properties: [code: "PROD-99"]
                      }
                    ]
                  }
                  Where: {}
                  Updates: [
                    Set: [
                      (= (. p precio) 19.99)
                    ]
                  ]
                  Return: {
                    Projection: {
                      Items: [
                        {p}
                      ]
                      Modifiers: []
                    }
                  }
                }""", AstPrinter.print(setPrecio()));
    }

    @Test
    void imprimeRemoveYEtiquetas() {
        var query = new Query(
                match(node("p", "Product")),
                Optional.empty(),
                List.of(new RemoveClause(List.of(
                                new RemoveProperty(prop("p", "discount")),
                                new RemoveLabels("p", List.of("Oferta")))),
                        new SetClause(List.of(new SetLabels("p", List.of("Agotado", "Viejo"))))),
                returning(item(prop("p", "id"))));

        assertEquals("""
                Query{
                  Match: {
                    Patterns: [
                      PatternNode: {
                        var: p
                        labels: [ Product ]
                        properties: []
                      }
                    ]
                  }
                  Where: {}
                  Updates: [
                    Remove: [
                      (. p discount)
                      (labels p Oferta)
                    ]
                    Set: [
                      (labels p Agotado Viejo)
                    ]
                  ]
                  Return: {
                    Projection: {
                      Items: [
                        {(. p id)}
                      ]
                      Modifiers: []
                    }
                  }
                }""", AstPrinter.print(query));
    }

    @Test
    void imprimeAndOrNotYDireccionesInNone() {
        // MATCH (a)<-[r:KNOWS|LIKES]-(b), (a)-[s]-(:City)
        // WHERE NOT a.age < 18 AND (r.since > 2000 OR b.vip = 1)
        // DETACH DELETE s
        var query = new Query(
                match(path(node("a"), rel("r", Direction.IN, "KNOWS", "LIKES"), node("b")),
                        path(node("a"), rel("s", Direction.NONE), anonymousNode("City"))),
                where(new And(
                        new Not(new Binary(BinaryOp.LT, prop("a", "age"), new NumberLit(18))),
                        new Or(new Binary(BinaryOp.GT, prop("r", "since"), new NumberLit(2000)),
                                new Binary(BinaryOp.EQ, prop("b", "vip"), new NumberLit(1))))),
                List.of(new DeleteClause(true, List.of("s"))),
                Optional.empty());

        var printed = AstPrinter.print(query);

        assertTrue(printed.contains("direction: IN\n"));
        assertTrue(printed.contains("types: [ KNOWS | LIKES ]\n"));
        assertTrue(printed.contains("direction: NONE\n"));
        assertTrue(printed.contains("var: _\n"));
        assertTrue(printed.contains(
                "Expr: (AND (NOT (< (. a age) 18)) (OR (> (. r since) 2000) (= (. b vip) 1)))"));
        assertTrue(printed.contains("DetachDelete: [ s ]"));
    }
}