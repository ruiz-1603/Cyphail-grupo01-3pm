package org.cyphail.mqi;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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

class MqiResponseParserTest {

    @Test
    void parseFalseReturnsFailure() {
        PrologResult result = MqiResponseParser.parse("false");
        assertInstanceOf(PrologResult.Failure.class, result);
    }

    @Test
    void parseTrueBooleanReturnsSuccessWithEmptyBindingMap() {
        PrologResult result = MqiResponseParser.parse("true");
        assertInstanceOf(PrologResult.Success.class, result);
        var success = (PrologResult.Success) result;
        assertEquals(1, success.rows().size());
        assertTrue(success.rows().getFirst().isEmpty());
    }

    @Test
    void parseSuccessWithRowsBinding() {
        String json = """
                {
                  "args": [
                    [
                      [
                        {"args": ["Rows", "a.name b.name\\njuan maria"], "functor": "="}
                      ]
                    ]
                  ],
                  "functor": "true"
                }
                """;
        PrologResult result = MqiResponseParser.parse(json);
        assertInstanceOf(PrologResult.Success.class, result);
        var success = (PrologResult.Success) result;
        assertEquals(1, success.rows().size());
        Map<String, Object> row = success.rows().getFirst();
        assertEquals("a.name b.name\njuan maria", row.get("Rows"));
    }

    @Test
    void parseMultipleSolutionsAndTypes() {
        String json = """
                {
                  "functor": "true",
                  "args": [
                    [
                      [
                        {"functor": "=", "args": ["Name", "Ana"]},
                        {"functor": "=", "args": ["Age", 28]},
                        {"functor": "=", "args": ["Score", 9.5]}
                      ],
                      [
                        {"functor": "=", "args": ["Name", "Luis"]},
                        {"functor": "=", "args": ["Age", 31]},
                        {"functor": "=", "args": ["Score", 8.0]}
                      ]
                    ]
                  ]
                }
                """;
        PrologResult result = MqiResponseParser.parse(json);
        var success = assertInstanceOf(PrologResult.Success.class, result);
        assertEquals(2, success.rows().size());
        assertEquals("Ana", success.rows().get(0).get("Name"));
        assertEquals(28L, success.rows().get(0).get("Age"));
        assertEquals(9.5, success.rows().get(0).get("Score"));
        assertEquals("Luis", success.rows().get(1).get("Name"));
    }

    @Test
    void parseExceptionReturnsPrologError() {
        String json = """
                {
                  "functor": "exception",
                  "args": [
                    {"functor": "error", "args": ["syntax_error", "unexpected_eof"]}
                  ]
                }
                """;
        PrologResult result = MqiResponseParser.parse(json);
        var error = assertInstanceOf(PrologResult.PrologError.class, result);
        var compound = assertInstanceOf(MqiResponseParser.Compound.class, error.term());
        assertEquals("error", compound.functor());
        assertEquals(List.of("syntax_error", "unexpected_eof"), compound.args());
    }
}
