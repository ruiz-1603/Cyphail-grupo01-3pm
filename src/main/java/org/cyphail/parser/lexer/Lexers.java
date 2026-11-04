package org.cyphail.parser.lexer;

import org.cyphail.parser.core.Fail;
import org.cyphail.parser.core.InputString;
import org.cyphail.parser.core.Lexer;
import org.cyphail.parser.core.Ok;
import org.cyphail.parser.core.Result;
import org.cyphail.parser.core.TToken;
import org.cyphail.parser.core.TokenString;

import java.util.regex.Pattern;

public final class Lexers {

    private Lexers() {}

    // Lo que se salta antes de cada token (gramatica: WS, LINE_COMMENT y BLOCK_COMMENT -> skip)
    private static final String SKIP = "(?:\\s|//[^\\n\\r]*|/\\*(?s:.*?)\\*/)*";

    public static Lexer Literal(TToken type, String expected) {
        var re = Pattern.compile(SKIP + "(?<token>" + Pattern.quote(expected) + ")");
        return (InputString source) ->
                match(re, type, source, "Expected '" + expected + "'");
    }

    // DecimalInteger := [+-]?\d+
    public static Lexer Number() {
        var re = Pattern.compile(SKIP + "(?<token>[+-]?\\d+)");
        return (InputString source) ->
                match(re, TToken.NUM, source, "No number could be matched");
    }

    // doubleLiteral := [+-]?\d+.\d+   (p.ej. 25.5, 19.99)
    public static Lexer Decimal() {
        var re = Pattern.compile(SKIP + "(?<token>[+-]?\\d+\\.\\d+)");
        return (InputString source) ->
                match(re, TToken.DECIMAL, source, "No decimal could be matched");
    }

    // StringLiteral := '...' | "..."
    public static Lexer StringLiteral() {
        var doubleQuoted = Pattern.compile(SKIP + "\"(?<token>[^\"]*)\"");
        var singleQuoted = Pattern.compile(SKIP + "'(?<token>[^']*)'");
        return (InputString source) -> switch (match(doubleQuoted, TToken.STRING, source, "No string could be matched")) {
            case Ok<InputString, TokenString, String> ok -> ok;
            case Fail<InputString, TokenString, String> _ ->
                    match(singleQuoted, TToken.STRING, source, "No string could be matched");
        };
    }

    // booleanLiteral := "TRUE" | "FALSE"   (tambien en minuscula, como en los casos del profesor: {activo: true})
    public static Lexer BooleanLiteral() {
        var re = Pattern.compile(SKIP + "(?<token>(?i:true|false))(?!\\w)");
        return (InputString source) ->
                match(re, TToken.BOOL, source, "No boolean could be matched");
    }

    // Palabras clave del lenguaje: no pueden usarse como identificador (p.ej. en WHERE a.x > RETURN)
    private static final String RESERVED = "(?:MATCH|WHERE|CREATE|SET|DELETE|REMOVE|RETURN|AS|AND|OR|NOT)(?!\\w)";

    public static Lexer Id() {
        var re = Pattern.compile(SKIP + "(?!" + RESERVED + ")(?<token>[a-zA-Z_]\\w*)");
        return (InputString source) ->
                match(re, TToken.ID, source, "No id could be matched");
    }

    public static Lexer Eof() {
        var re = Pattern.compile(SKIP + "(?<token>\\z)");
        return (InputString source) ->
                match(re, TToken.EOF, source, "Expected end of input");
    }

    // Una palabra clave completa: "OR" no se reconoce dentro de "ORDEN"
    public static Lexer Keyword(String expected) {
        var re = Pattern.compile(SKIP + "(?<token>" + Pattern.quote(expected) + ")(?!\\w)");
        return (InputString source) ->
                match(re, TToken.KEYWORD, source, "Expected '" + expected + "'");
    }

    public static Lexer Symbol(String expected) {
        return Literal(TToken.SYMBOL, expected);
    }

    private static Result<InputString, TokenString, String> match(
            Pattern re, TToken type, InputString source, String error) {

        var matcher = re.matcher(source.input());
        matcher.region(source.index(), source.input().length());

        if (!matcher.lookingAt()) {
            return Result.fail(error + " at position " + source.index());
        }

        return Result.ok(
                new TokenString(type, matcher.group("token")),
                new InputString(source.input(), matcher.end())
        );
    }
}
