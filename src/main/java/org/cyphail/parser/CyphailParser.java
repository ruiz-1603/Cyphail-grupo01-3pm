package org.cyphail.parser;

import org.cyphail.ast.*;
import org.cyphail.parser.combinators.Parsers;
import org.cyphail.parser.core.InputString;
import org.cyphail.parser.core.Parser;
import org.cyphail.parser.core.Result;
import org.cyphail.parser.lexer.Lexers;

import java.util.List;
import java.util.Optional;

public final class CyphailParser {

    private CyphailParser() {}

    // node := "(" ID ":" ID ")"
    static Parser<InputString, PatternNode, String> Node() {
        return Parsers.Map(
                Parsers.Sequence(
                        Lexers.Symbol("("),
                        Lexers.Id(),
                        Lexers.Symbol(":"),
                        Lexers.Id(),
                        Lexers.Symbol(")")),
                tokens -> new PatternNode(
                        tokens.get(1).value(),
                        List.of(tokens.get(3).value()),
                        List.of()));
    }

    // property := ID "." ID
    static Parser<InputString, Expr, String> Property() {
        return Parsers.Map(
                Parsers.Sequence(
                        Lexers.Id(),
                        Lexers.Symbol("."),
                        Lexers.Id()),
                tokens -> new PropertyAccess(
                        tokens.get(0).value(),
                        tokens.get(2).value()));
    }

    // alias := ("AS" ID)?
    static Parser<InputString, Optional<String>, String> Alias() {
        return Parsers.Map(
                Parsers.Opt(Parsers.Right(Lexers.Keyword("AS"), Lexers.Id())),
                maybeToken -> maybeToken.map(t -> t.value()));
    }

    // item := property alias
    static Parser<InputString, ProjectionItem, String> Item() {
        return Parsers.Combine(Property(), Alias(), ProjectionItem::new);
    }

    // query := "MATCH" node "RETURN" item EOF
    static Parser<InputString, Query, String> Query() {
        return Parsers.Left(
                Parsers.Combine(
                        Parsers.Right(Lexers.Keyword("MATCH"), Node()),
                        Parsers.Right(Lexers.Keyword("RETURN"), Item()),
                        (node, item) -> new Query(
                                new MatchClause(List.of(node)),
                                Optional.empty(),
                                List.of(),
                                new ReturnClause(new Projection(List.of(item), List.of())))),
                Lexers.Eof());
    }

    public static Result<InputString, Query, String> parse(String text) {
        return Query().parse(new InputString(text, 0));
    }
}