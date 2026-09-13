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

        Parser<String, String, String> first =
                input -> {
                    if (input.startsWith("ABC")) {
                        return Result.ok(
                                "ABC",
                                input.substring(3)
                        );
                    }

                    return Result.fail("Expected ABC");
                };

        Parser<String, String, String> second =
                input -> {
                    if (input.startsWith("DEF")) {
                        return Result.ok(
                                "DEF",
                                input.substring(3)
                        );
                    }

                    return Result.fail("Expected DEF");
                };

        Parser<String, String, String> combined =
                Parsers.sequence(first, second);

        Result<String, String, String> result =
                combined.parse("ABCDEF");

        assertTrue(result instanceof org.cyphail.parser.core.Ok);

        var ok = (org.cyphail.parser.core.Ok<String, String, String>) result;

        assertEquals("DEF", ok.token());
        assertEquals("", ok.rest());
    }
	
	@Test
    void choiceUsesSecondParserWhenFirstFails() {

        Parser<String, String, String> first =
                input -> {
                    if (input.startsWith("ABC")) {
                        return Result.ok(
                                "ABC",
                                input.substring(3)
                        );
                    }

                    return Result.fail("Expected ABC");
                };

        Parser<String, String, String> second =
                input -> {
                    if (input.startsWith("DEF")) {
                        return Result.ok(
                                "DEF",
                                input.substring(3)
                        );
                    }

                    return Result.fail("Expected DEF");
                };

        Parser<String, String, String> chosen =
                Parsers.choice(first, second);

        Result<String, String, String> result =
                chosen.parse("DEF");

        assertTrue(result instanceof Ok);

        Ok<String, String, String> ok =
                (Ok<String, String, String>) result;

        assertEquals("DEF", ok.token());
        assertEquals("", ok.rest());
    }
	
	@Test
	void mapTransformsParserResult() {

		Parser<String, String, String> parser =
				input -> {
					if (input.startsWith("ABC")) {
						return Result.ok(
								"ABC",
								input.substring(3)
						);
					}

					return Result.fail("Expected ABC");
				};

		Parser<String, Integer, String> mapped =
				Parsers.map(
						parser,
						text -> text.length()
				);

		Result<String, Integer, String> result =
				mapped.parse("ABCDEF");

		assertTrue(result instanceof Ok);

		Ok<String, Integer, String> ok =
				(Ok<String, Integer, String>) result;

		assertEquals(3, ok.token());
		assertEquals("DEF", ok.rest());
	}
	
	
	
	@Test
	void mapKeepsParserFailure() {

		Parser<String, String, String> parser =
				input -> Result.fail("Expected ABC");

		Parser<String, Integer, String> mapped =
				Parsers.map(
						parser,
						text -> text.length()
				);

		Result<String, Integer, String> result =
				mapped.parse("XYZ");

		assertTrue(result instanceof Fail);

		Fail<String, Integer, String> fail =
				(Fail<String, Integer, String>) result;

		assertEquals("Expected ABC", fail.reason());
	}
	
	@Test
	void literalRecognizesExpectedText() {

		Parser<String, String, String> parser =
				Parsers.literal("MATCH");

		Result<String, String, String> result =
				parser.parse("MATCH (m:Movie)");

		assertTrue(result instanceof Ok);

		Ok<String, String, String> ok =
				(Ok<String, String, String>) result;

		assertEquals("MATCH", ok.token());
		assertEquals(" (m:Movie)", ok.rest());
	}
		
	@Test
	void literalFailsWhenTextDoesNotMatch() {

		Parser<String, String, String> parser =
				Parsers.literal("MATCH");

		Result<String, String, String> result =
				parser.parse("CREATE (m:Movie)");

		assertTrue(result instanceof Fail);

		Fail<String, String, String> fail =
				(Fail<String, String, String>) result;

		assertEquals("Expected 'MATCH'", fail.reason());
	}
	
	
	
	
	
	
	
}