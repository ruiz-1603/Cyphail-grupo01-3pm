package org.cyphail.parser.lexer;

import org.cyphail.parser.core.InputString;
import org.cyphail.parser.core.Lexer;
import org.cyphail.parser.core.Result;
import org.cyphail.parser.core.TToken;
import org.cyphail.parser.core.TokenString;

import java.util.regex.Pattern;

public final class Lexers {

    private Lexers() {}

    public static Lexer Literal(TToken type, String expected) {
        var re = Pattern.compile("\\s*(?<token>" + Pattern.quote(expected) + ")");
        return (InputString source) ->
                match(re, type, source, "Expected '" + expected + "'");
    }

    public static Lexer Number() {
        var re = Pattern.compile("\\s*(?<token>\\d+)");
        return (InputString source) ->
                match(re, TToken.NUM, source, "No number could be matched");
    }

    public static Lexer Id() {
        var re = Pattern.compile("\\s*(?<token>[a-zA-Z_]\\w*)");
        return (InputString source) ->
                match(re, TToken.ID, source, "No id could be matched");
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