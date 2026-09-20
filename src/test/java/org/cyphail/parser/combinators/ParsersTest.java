package org.cyphail.parser.combinators;

import org.cyphail.parser.core.Parser;
import org.cyphail.parser.core.Ok;
import org.cyphail.parser.core.Fail;
import org.cyphail.parser.core.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParsersTest {

    @Test
    void sequenceCombinesTwoParsers() {

        Parser<String, String, String> first = input -> {
            if (input.startsWith("ABC")) {
                return Result.ok("ABC", input.substring(3));
            }
            return Result.fail("Expected ABC");
        };

        Parser<String, String, String> second = input -> {
            if (input.startsWith("DEF")) {
                return Result.ok("DEF", input.substring(3));
            }
            return Result.fail("Expected DEF");
        };

        Parser<String, java.util.List<String>, String> combined = Parsers.Sequence(first, second);

        Result<String, java.util.List<String>, String> result = combined.parse("ABCDEF");

        assertTrue(result instanceof Ok);

        var ok = (Ok<String, java.util.List<String>, String>) result;

        assertEquals(java.util.List.of("ABC", "DEF"), ok.token());

        assertEquals("", ok.rest());
    }

    @Test
    void sequenceFailsWhenAnyParserFails() {

        Parser<String, String, String> first = input -> {
            if (input.startsWith("ABC")) {
                return Result.ok("ABC", input.substring(3));
            }
            return Result.fail("Expected ABC");
        };

        Parser<String, String, String> second = input -> {
            if (input.startsWith("DEF")) {
                return Result.ok("DEF", input.substring(3));
            }
            return Result.fail("Expected DEF");
        };

        Result<String, java.util.List<String>, String> result = Parsers.Sequence(first, second).parse("ABCXYZ");
        assertTrue(result instanceof Fail);

        var fail = (Fail<String, java.util.List<String>, String>) result;

        assertEquals("Expected DEF", fail.reason());
    }

    @Test
    void choiceUsesSecondParserWhenFirstFails() {

        Parser<String, String, String> first = input -> {
            if (input.startsWith("ABC")) {
                return Result.ok("ABC", input.substring(3));
            }
            return Result.fail("Expected ABC");
        };

        Parser<String, String, String> second = input -> {
            if (input.startsWith("DEF")) {
                return Result.ok("DEF", input.substring(3));
            }
            return Result.fail("Expected DEF");
        };

        Parser<String, String, String> chosen = Parsers.Or(first, second);

        Result<String, String, String> result = chosen.parse("DEF");

        assertTrue(result instanceof Ok);

        Ok<String, String, String> ok = (Ok<String, String, String>) result;

        assertEquals("DEF", ok.token());

        assertEquals("", ok.rest());
    }

    @Test
    void mapTransformsParserResult() {

        Parser<String, String, String> parser = input -> {
            if (input.startsWith("ABC")) {
                return Result.ok("ABC", input.substring(3));
            }
            return Result.fail("Expected ABC");
        };

        Parser<String, Integer, String> mapped = Parsers.Map(parser, text -> text.length());

        Result<String, Integer, String> result = mapped.parse("ABCDEF");

        assertTrue(result instanceof Ok);

        Ok<String, Integer, String> ok = (Ok<String, Integer, String>) result;

        assertEquals(3, ok.token());

        assertEquals("DEF", ok.rest());
    }

    @Test
    void mapKeepsParserFailure() {

        Parser<String, String, String> parser = input -> Result.fail("Expected ABC");

        Parser<String, Integer, String> mapped = Parsers.Map(parser, text -> text.length());

        Result<String, Integer, String> result = mapped.parse("XYZ");

        assertTrue(result instanceof Fail);

        Fail<String, Integer, String> fail = (Fail<String, Integer, String>) result;

        assertEquals("Expected ABC", fail.reason());
    }
}