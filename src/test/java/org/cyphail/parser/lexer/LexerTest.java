package org.cyphail.parser.lexer;

import org.cyphail.parser.combinators.Parsers;
import org.cyphail.parser.core.InputString;
import org.cyphail.parser.core.Result;
import org.cyphail.parser.core.TToken;
import org.cyphail.parser.core.TokenString;
import org.junit.jupiter.api.Test;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LexersTest {

    @Test
    void literalRecognizesExpectedText() {
        var source = new InputString("MATCH (m:Movie)", 0);

        var result = Lexers.Literal(TToken.KEYWORD, "MATCH").parse(source);

        assertEquals(
                Result.ok(new TokenString(TToken.KEYWORD, "MATCH"),
                        new InputString("MATCH (m:Movie)", 5)),
                result
        );
    }

    @Test
    void literalSkipsLeadingWhitespace() {
        var source = new InputString("   MATCH", 0);

        var result = Lexers.Literal(TToken.KEYWORD, "MATCH").parse(source);

        assertEquals(
                Result.ok(new TokenString(TToken.KEYWORD, "MATCH"),
                        new InputString("   MATCH", 8)),
                result
        );
    }

    @Test
    void literalFailsWhenTextDoesNotMatch() {
        var source = new InputString("CREATE (m:Movie)", 0);

        var result = Lexers.Literal(TToken.KEYWORD, "MATCH").parse(source);

        assertEquals(Result.fail("Expected 'MATCH' at position 0"), result);
    }

    @Test
    void sequenceComposesLexers() {
        var source = new InputString("MATCH movie 42", 0);

        var parser = Parsers.Sequence(
                Lexers.Literal(TToken.KEYWORD, "MATCH"),
                Lexers.Id(),
                Lexers.Number()
        );

        var result = parser.parse(source);

        assertEquals(
                Result.ok(List.of(new TokenString(TToken.KEYWORD, "MATCH"),
                                new TokenString(TToken.ID, "movie"),
                                new TokenString(TToken.NUM, "42")),
                        new InputString("MATCH movie 42", 14)),
                result
        );
    }
}