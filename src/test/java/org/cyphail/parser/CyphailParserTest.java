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


}