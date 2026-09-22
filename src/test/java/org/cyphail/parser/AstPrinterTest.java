package org.cyphail.ast;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AstPrinterTest {

    @Test
    void printsQueryWithWhereAndTwoItems() {
        var query = new Query(
                new MatchClause(List.of(new PatternNode("m", List.of("Movie"), List.of()))),
                Optional.of(new WhereClause(new Binary(BinaryOp.GT,
                        new PropertyAccess("m", "year"), new NumberLit(1990)))),
                List.of(),
                new ReturnClause(new Projection(List.of(
                        new ProjectionItem(new PropertyAccess("m", "title"), Optional.of("title")),
                        new ProjectionItem(new PropertyAccess("m", "year"), Optional.of("year"))
                ), List.of())));

        String expected = """
                Query{
                  Match: {
                    Patterns: [
                      PatternNode: {
                        var: m
                        labels: [ Movie ]
                        properties: []
                      }
                    ]
                  }
                  Where: {
                    Expr: (> (. m year) 1990)
                  }
                  Updates: []
                  Return: {
                    Projection: {
                      Items: [
                        {as (. m title) title}
                        {as (. m year) year}
                      ]
                      Modifiers: []
                    }
                  }
                }""";

        assertEquals(expected, AstPrinter.print(query));
    }
}