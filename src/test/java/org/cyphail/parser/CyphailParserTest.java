package org.cyphail.parser;

import org.cyphail.ast.*;

import java.util.Optional;
import org.cyphail.parser.core.InputString;
import org.cyphail.parser.core.Result;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CyphailParserTest {

    private static Query movieTitleQuery() {
        return new Query(
                new MatchClause(List.of(
                        new PatternNode("m", List.of("Movie"), List.of()))),
                Optional.empty(),
                List.of(),
                new ReturnClause(new Projection(
                        List.of(new ProjectionItem(
                                new PropertyAccess("m", "title"),
                                Optional.empty())),
                        List.of())));
    }

    @Test
    void parsesSimpleMatchReturn() {
        String text = "MATCH (m:Movie) RETURN m.title";

        var result = CyphailParser.parse(text);

        assertEquals(
                Result.ok(movieTitleQuery(), new InputString(text, text.length())),
                result
        );
    }

    @Test
    void parsesQueryWithLineBreaksAndExtraSpaces() {
        String text = "  MATCH (m:Movie)\n RETURN m.title  ";

        var result = CyphailParser.parse(text);

        assertEquals(
                Result.ok(movieTitleQuery(), new InputString(text, text.length())),
                result
        );
    }

    @Test
    void failsWhenQueryDoesNotStartWithMatch() {
        var result = CyphailParser.parse("CREATE (m:Movie) RETURN m.title");

        assertEquals(Result.fail("Expected 'MATCH' at position 0"), result);
    }

    @Test
    void failsWhenReturnItemHasNoProperty() {
        var result = CyphailParser.parse("MATCH (m:Movie) RETURN m");

        assertEquals(Result.fail("Expected '.' at position 24"), result);
    }

    @Test
    void failsWhenThereIsTrailingText() {
        var result = CyphailParser.parse("MATCH (m:Movie) RETURN m.title x");

        assertEquals(Result.fail("Expected end of input at position 30"), result);
    }

    @Test
    void parsesItemWithAlias() {
        String text = "MATCH (m:Movie) RETURN m.title AS title";

        var result = CyphailParser.parse(text);

        var expected = new Query(
                new MatchClause(List.of(
                        new PatternNode("m", List.of("Movie"), List.of()))),
                Optional.empty(),
                List.of(),
                new ReturnClause(new Projection(
                        List.of(new ProjectionItem(
                                new PropertyAccess("m", "title"),
                                Optional.of("title"))),
                        List.of())));

        assertEquals(Result.ok(expected, new InputString(text, text.length())), result);
    }

    @Test
    void parsesCase1WithMultipleProjectionItems() {
        String text = """
                MATCH (m:Movie)
                RETURN m.title,
                       m.year AS year
                """;

        var result = CyphailParser.parse(text);

        var expected = new Query(
                new MatchClause(List.of(
                        new PatternNode("m", List.of("Movie"), List.of())
                )),
                Optional.empty(),
                List.of(),
                new ReturnClause(new Projection(
                        List.of(
                                new ProjectionItem(new PropertyAccess("m", "title"), Optional.empty()),
                                new ProjectionItem(new PropertyAccess("m", "year"), Optional.of("year"))
                        ),
                        List.of()
                ))
        );

        assertEquals(Result.ok(expected, new InputString(text, text.length())), result);
    }

    @Test
    void parsesCase2WithWhereComparison() {
        String text = """
                MATCH (b:Book)
                WHERE b.pages < 300
                RETURN b.title,
                       b.pages AS totalPages
                """;

        var result = CyphailParser.parse(text);

        var expected = new Query(
                new MatchClause(List.of(
                        new PatternNode("b", List.of("Book"), List.of())
                )),
                Optional.of(new WhereClause(
                        new Binary(BinaryOp.LT, new PropertyAccess("b", "pages"), new NumberLit(300))
                )),
                List.of(),
                new ReturnClause(new Projection(
                        List.of(
                                new ProjectionItem(new PropertyAccess("b", "title"), Optional.empty()),
                                new ProjectionItem(new PropertyAccess("b", "pages"), Optional.of("totalPages"))
                        ),
                        List.of()
                ))
        );

        assertEquals(Result.ok(expected, new InputString(text, text.length())), result);
    }

    @Test
    void parsesCase3() {
        String text = """
                MATCH (a:Person:Employee {id: 1})
                WHERE a.age > 30
                RETURN a.name AS name,
                       a.age AS age
                """;

        var result = CyphailParser.parse(text);

        var expected = new Query(
                new MatchClause(List.of(
                        new PatternNode(
                                "a",
                                List.of("Person", "Employee"),
                                List.of(new PropertyEntry("id", new NumberLit(1)))
                        )
                )),
                Optional.of(new WhereClause(
                        new Binary(BinaryOp.GT, new PropertyAccess("a", "age"), new NumberLit(30))
                )),
                List.of(),
                new ReturnClause(new Projection(
                        List.of(
                                new ProjectionItem(new PropertyAccess("a", "name"), Optional.of("name")),
                                new ProjectionItem(new PropertyAccess("a", "age"), Optional.of("age"))
                        ),
                        List.of()
                ))
        );

        assertEquals(Result.ok(expected, new InputString(text, text.length())), result);
    }

    @Test
    void parsesCase4() {
        String text = """
                MATCH (m:Movie), (p:Person)
                WHERE m.year > 2000
                RETURN m.title AS title,
                       p.name AS actor
                """;

        var result = CyphailParser.parse(text);

        var expected = new Query(
                new MatchClause(List.of(
                        new PatternNode("m", List.of("Movie"), List.of()),
                        new PatternNode("p", List.of("Person"), List.of())
                )),
                Optional.of(new WhereClause(
                        new Binary(BinaryOp.GT, new PropertyAccess("m", "year"), new NumberLit(2000))
                )),
                List.of(),
                new ReturnClause(new Projection(
                        List.of(
                                new ProjectionItem(new PropertyAccess("m", "title"), Optional.of("title")),
                                new ProjectionItem(new PropertyAccess("p", "name"), Optional.of("actor"))
                        ),
                        List.of()
                ))
        );

        assertEquals(Result.ok(expected, new InputString(text, text.length())), result);
    }

    @Test
    void parsesCase5() {
        String text = """
                MATCH (m:Movie), (p:Person)
                WHERE m.year <> p.age
                RETURN m.title AS title,
                       p.name AS name
                """;

        var result = CyphailParser.parse(text);

        var expected = new Query(
                new MatchClause(List.of(
                        new PatternNode("m", List.of("Movie"), List.of()),
                        new PatternNode("p", List.of("Person"), List.of())
                )),
                Optional.of(new WhereClause(
                        new Binary(
                                BinaryOp.NEQ,
                                new PropertyAccess("m", "year"),
                                new PropertyAccess("p", "age")
                        )
                )),
                List.of(),
                new ReturnClause(new Projection(
                        List.of(
                                new ProjectionItem(new PropertyAccess("m", "title"), Optional.of("title")),
                                new ProjectionItem(new PropertyAccess("p", "name"), Optional.of("name"))
                        ),
                        List.of()
                ))
        );

        assertEquals(Result.ok(expected, new InputString(text, text.length())), result);
    }

    @Test
    void parsesCase6() {
        String text = """
                MATCH (m:
                Movie {year: 1999}), (p:Person {age: 40})
                WHERE m.year <> p.age
                RETURN m.title AS title,
                       p.name AS name
                """;

        var result = CyphailParser.parse(text);

        var expected = new Query(
                new MatchClause(List.of(
                        new PatternNode(
                                "m",
                                List.of("Movie"),
                                List.of(new PropertyEntry("year", new NumberLit(1999)))
                        ),
                        new PatternNode(
                                "p",
                                List.of("Person"),
                                List.of(new PropertyEntry("age", new NumberLit(40)))
                        )
                )),
                Optional.of(new WhereClause(new Binary(
                                            BinaryOp.NEQ,
                                            new PropertyAccess("m", "year"),
                                            new PropertyAccess("p", "age")
                        )
                )),
                List.of(),
                new ReturnClause(new Projection(
                        List.of(
                                new ProjectionItem(new PropertyAccess("m", "title"), Optional.of("title")),
                                new ProjectionItem(new PropertyAccess("p", "name"), Optional.of("name"))
                        ),
                        List.of()
                ))
        );

        assertEquals(Result.ok(expected, new InputString(text, text.length())), result);
    }

    @Test
    void parsesCase7() {
        String text = """
                MATCH (p:Person)
                WHERE p.age > 18
                CREATE (c:Certificate {issuedTo: "adult", year: 2026})
                RETURN p.name AS name,
                       p.age AS age
                """;

        var result = CyphailParser.parse(text);

        var expected = new Query(
                new MatchClause(List.of(
                        new PatternNode("p", List.of("Person"), List.of())
                )),
                Optional.of(new WhereClause(
                        new Binary(BinaryOp.GT, new PropertyAccess("p", "age"), new NumberLit(18))
                )),
                List.of(
                        new CreateClause(List.of(new PatternNode(
                                                         "c",
                                                         List.of("Certificate"),
                                                         List.of(
                                                                 new PropertyEntry("issuedTo", new StringLit("adult")),
                                                                 new PropertyEntry("year", new NumberLit(2026))
                                                         )
                                )
                        ))
                ),
                new ReturnClause(new Projection(
                        List.of(
                                new ProjectionItem(new PropertyAccess("p", "name"), Optional.of("name")),
                                new ProjectionItem(new PropertyAccess("p", "age"), Optional.of("age"))
                        ),
                        List.of()
                ))
        );

        assertEquals(Result.ok(expected, new InputString(text, text.length())), result);
    }

    @Test
    void parsesCase8() {
        String text = """
                MATCH (p:Person {id: 1}), (o:Order {personId: p.id})
                RETURN p.name AS name,
                       o.total AS total
                """;

        var result = CyphailParser.parse(text);

        var expected = new Query(new MatchClause(List.of(
                                 new PatternNode(
                                        "p",
                                        List.of("Person"),
                                        List.of(new PropertyEntry("id", new NumberLit(1)))
                                 ),
                                 new PatternNode(
                                        "o",
                                        List.of("Order"),
                                        List.of(new PropertyEntry(
                                                "personId",
                                                new PropertyAccess("p", "id")
                                        ))
                                 )
                )),
                Optional.empty(),
                List.of(),
                new ReturnClause(new Projection(
                        List.of(
                                new ProjectionItem(new PropertyAccess("p", "name"), Optional.of("name")),
                                new ProjectionItem(new PropertyAccess("o", "total"), Optional.of("total"))
                        ),
                        List.of()
                ))
        );

        assertEquals(Result.ok(expected, new InputString(text, text.length())), result);
    }

    @Test
    void parsesCase9() {
        String text = """
                MATCH (p:Person), (o:Order {personId: p.id, status: "cancelled"})
                WHERE p.age > 60
                CREATE (a:Archive {id: o.id, name: "retired", year: 2026})
                DELETE o
                RETURN p.name AS name
                """;

        var result = CyphailParser.parse(text);

        var expected = new Query(
                new MatchClause(List.of(new PatternNode("p", List.of("Person"), List.of()),
                                        new PatternNode(
                                                "o",
                                                List.of("Order"),
                                                List.of(
                                                        new PropertyEntry("personId", new PropertyAccess("p", "id")),
                                                        new PropertyEntry("status", new StringLit("cancelled"))
                                                )
                                        )
                )),
                Optional.of(new WhereClause(new Binary(BinaryOp.GT, new PropertyAccess("p", "age"), new NumberLit(60))
                )),
                List.of(new CreateClause(List.of(
                                new PatternNode(
                                        "a",
                                        List.of("Archive"),
                                        List.of(
                                                new PropertyEntry("id", new PropertyAccess("o", "id")),
                                                new PropertyEntry("name", new StringLit("retired")),
                                                new PropertyEntry("year", new NumberLit(2026))
                                        )
                                )
                        )),
                        new DeleteClause(List.of("o"))
                ),
                new ReturnClause(new Projection(
                        List.of(new ProjectionItem(new PropertyAccess("p", "name"), Optional.of("name"))),
                        List.of()
                ))
        );

        assertEquals(Result.ok(expected, new InputString(text, text.length())), result);
    }
}