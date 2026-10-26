
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

        Parser<String, java.util.List<String>, String> combined =
                Parsers.Seq(first, second);

        Result<String, java.util.List<String>, String> result =
                combined.parse("ABCDEF");

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

        Result<String, java.util.List<String>, String> result =
                Parsers.Seq(first, second).parse("ABCXYZ");

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

        Parser<String, String, String> chosen = Parsers.RFlat(
                Parsers.Or(first, second),
                reasons -> String.join(", ", reasons)
        );

        Result<String, String, String> result = chosen.parse("DEF");

        assertTrue(result instanceof Ok);

        Ok<String, String, String> ok = (Ok<String, String, String>) result;

        assertEquals("DEF", ok.token());
        assertEquals("", ok.rest());
    }


    @Test
    void tFlatTransformsParserResult() {

        Parser<String, java.util.List<String>, String> parser =
                input -> Result.ok(java.util.List.of("ABC", "DEF"), input.substring(6));

        Parser<String, String, String> combined = Parsers.TFlat(parser, tokens -> String.join("", tokens));

        Result<String, String, String> result = combined.parse("ABCDEF");

        assertTrue(result instanceof Ok);

        var ok = (Ok<String, String, String>) result;

        assertEquals("ABCDEF", ok.token());
        assertEquals("", ok.rest());
    }

    @Test
    void tFlatKeepsParserFailure() {

        Parser<String, java.util.List<String>, String> parser = input -> Result.fail("Expected ABC");

        Parser<String, String, String> combined = Parsers.TFlat(parser, tokens -> String.join("", tokens));

        Result<String, String, String> result = combined.parse("XYZ");

        assertTrue(result instanceof Fail);

        var fail = (Fail<String, String, String>) result;

        assertEquals("Expected ABC", fail.reason());
    }

}
