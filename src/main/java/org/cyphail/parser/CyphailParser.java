package org.cyphail.parser;

import org.cyphail.ast.*;
import org.cyphail.parser.combinators.Parsers;
import org.cyphail.parser.core.InputString;
import org.cyphail.parser.core.Parser;
import org.cyphail.parser.core.Result;
import org.cyphail.parser.lexer.Lexers;
import org.cyphail.parser.core.Ok;
import org.cyphail.parser.core.Fail;
import org.cyphail.parser.core.TokenString;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

public final class CyphailParser {

    private CyphailParser() {}

    /*
     * Records auxiliares del parser.
     * No forman parte del AST.
     */
    private record ComparisonTail(BinaryOp op, Expr right) {}

    private record MatchWhere(List<PatternNode> patterns, Optional<WhereClause> where) {}

    private record QueryHead(List<PatternNode> patterns, Optional<WhereClause> where,
                             List<UpdateClause> updates) {}


    // node := "(" ID (":" ID)+ properties? ")"
    static Parser<InputString, PatternNode, String> nodePattern() {

        var label = Parsers.TFlat(Parsers.Seq(Lexers.Symbol(":"), Lexers.Id()), tokens -> tokens.get(1));

        Parser<InputString, List<PropertyEntry>, String> optionalProperties =
                source -> {String remaining = source.input().substring(source.index()).stripLeading();
                    if (remaining.startsWith("{")) {return properties().parse(source);}
                    return new Ok<>(List.of(), source);
                };

        return Parsers.Then(Lexers.Symbol("("), open ->
                Parsers.Then(Lexers.Id(), variable ->
                        Parsers.Then(label, firstLabel ->
                                Parsers.Then(Parsers.Star(label), otherLabels ->
                                        Parsers.Then(optionalProperties, properties ->
                                                Parsers.Then(Lexers.Symbol(")"), close -> {

                                                    var labels = new ArrayList<String>();
                                                    labels.add(firstLabel.value());

                                                    for (var token : otherLabels) {labels.add(token.value());}

                                                    return source -> new Ok<>(
                                                            new PatternNode(variable.value(), List.copyOf(labels), properties), source
                                                    );
                                                }))))));
    }

    // property := ID "." ID
    static Parser<InputString, Expr, String> property() {

        var property = Parsers.Seq(Lexers.Id(), Lexers.Symbol("."), Lexers.Id());

        return (InputString source) -> {
            return switch (property.parse(source)) {
                case Fail(String reason) -> new Fail<InputString, Expr, String>(reason);
                case Ok(List<TokenString> tokens, InputString rest) ->
                        new Ok<InputString, Expr, String>(
                                new PropertyAccess(tokens.get(0).value(), tokens.get(2).value()), rest);
            };
        };
    }

    // number := NUM
    static Parser<InputString, Expr, String> number() {

        return (InputString source) -> {
            return switch (Lexers.Number().parse(source)) {
                case Fail(String reason) -> new Fail<InputString, Expr, String>(reason);
                case Ok(TokenString token, InputString rest) ->
                        new Ok<InputString, Expr, String>(new NumberLit(Long.parseLong(token.value())), rest);
            };
        };
    }

    // stringExpr := STRING
    static Parser<InputString, Expr, String> stringExpr() {

        return (InputString source) -> {
            return switch (Lexers.StringLiteral().parse(source)) {
                case Fail(String reason) -> new Fail<InputString, Expr, String>(reason);
                case Ok(TokenString token, InputString rest) ->
                        new Ok<InputString, Expr, String>(new StringLit(token.value()), rest);
            };
        };
    }

    // simpleExpr := property | number | string
    static Parser<InputString, Expr, String> simpleExpr() {
        return Parsers.RFlat(Parsers.Or(property(),number(),stringExpr()),
                reasons -> String.join(", ", reasons)
        );
    }


    // propertyEntry := ID ":" simpleExpr
    static Parser<InputString, PropertyEntry, String> propertyEntry() {

        var key = Parsers.Then(Lexers.Id(),
                keyToken -> Parsers.Then(Lexers.Symbol(":"), colon -> source -> new Ok<>(keyToken, source))
        );

        return Parsers.Then(
                key,
                keyToken -> Parsers.Then(simpleExpr(),
                        value -> source -> new Ok<>(new PropertyEntry(keyToken.value(), value), source)
                )
        );
    }

    // propertyEntries := propertyEntry ("," propertyEntry)*
    static Parser<InputString, List<PropertyEntry>, String> propertyEntries() {

        var remainingProperties = Parsers.Star(
                Parsers.Then(Lexers.Symbol(","), comma -> propertyEntry())
        );

        return Parsers.Then(propertyEntry(), first ->
                Parsers.Then(remainingProperties, rest -> {

                    var properties = new ArrayList<PropertyEntry>();
                    properties.add(first);
                    properties.addAll(rest);

                    return source -> new Ok<>(List.copyOf(properties), source);
                })
        );
    }

    // properties := "{" propertyEntries "}"

    static Parser<InputString, List<PropertyEntry>, String> properties() {

        return Parsers.Then(
                Lexers.Symbol("{"), open ->
                        Parsers.Then(
                                propertyEntries(), properties ->
                                        Parsers.Then(
                                                Lexers.Symbol("}"), close ->
                                                        source -> new Ok<>(properties, source)
                                        )
                        )
        );
    }


    // patterns := node ("," node)*

    static Parser<InputString, List<PatternNode>, String> pattern() {

        var remainingPatterns = Parsers.Star(Parsers.Then(Lexers.Symbol(","), comma -> nodePattern()));

        return Parsers.Then(nodePattern(), first -> Parsers.Then(remainingPatterns, rest -> {

                    var patterns = new ArrayList<PatternNode>();
                    patterns.add(first);
                    patterns.addAll(rest);

                    return source -> new Ok<>(List.copyOf(patterns), source);
                })
        );
    }


    // alias := ("AS" ID)?
    static Parser<InputString, Optional<String>, String> alias() {

        var alias = Parsers.Then(
                Lexers.Keyword("AS"), token -> Parsers.Then(
                                Lexers.Id(), id ->
                                        source -> new Ok<>(Optional.of(id.value()), source))
        );

        return Parsers.RFlat(
                Parsers.Or(alias, source -> new Ok<>(Optional.empty(), source)
                ), reasons -> String.join(", ", reasons)
        );
    }

    // variableExpr := ID   (variable "pelada", sin propiedad; p.ej. RETURN q AS name)
    static Parser<InputString, Expr, String> variableExpr() {

        return (InputString source) -> {
            return switch (Lexers.Id().parse(source)) {
                case Fail(String reason) -> new Fail<InputString, Expr, String>(reason);
                case Ok(TokenString token, InputString rest) ->
                        new Ok<InputString, Expr, String>(new Var(token.value()), rest);
            };
        };
    }

    // projectionExpr := property | variableExpr
    static Parser<InputString, Expr, String> projectionExpr() {
        return Parsers.RFlat(Parsers.Or(property(), variableExpr()), reasons -> String.join(", ", reasons));
    }

    // item := projectionExpr alias

    static Parser<InputString, ProjectionItem, String> projectionItem() {

        return Parsers.Then(projectionExpr(), expr ->
                Parsers.Then(alias(), alias ->
                        source -> new Ok<>(new ProjectionItem(expr, alias), source)
                )
        );
    }


    // items := item ("," item)*

    static Parser<InputString, List<ProjectionItem>, String> projectionItems() {

        var remainingItems = Parsers.Star(
                Parsers.Then(Lexers.Symbol(","), comma -> projectionItem()));

        return Parsers.Then(projectionItem(), first ->
                Parsers.Then(remainingItems, rest -> {
                    var items = new ArrayList<ProjectionItem>();
                    items.add(first);
                    items.addAll(rest);
                    return source -> new Ok<>(List.copyOf(items), source);
                })
        );
    }


    // comparisonOp := "<=" | ">=" | "<>" | "=" | "<" | ">"
    // Ojo con el orden: los símbolos de 2 caracteres deben probarse antes que
    // sus prefijos de 1 caracter ("<=" antes que "<", ">=" antes que ">", etc.)

    static Parser<InputString, BinaryOp, String> comparisonOperator() {

        var operators = Parsers.RFlat(Parsers.Or(
                        Lexers.Symbol("<="),
                        Lexers.Symbol(">="),
                        Lexers.Symbol("<>"),
                        Lexers.Symbol("="),
                        Lexers.Symbol("<"),
                        Lexers.Symbol(">")
                ),
                reasons -> String.join(", ", reasons)
        );

        return (InputString source) -> {
            switch (operators.parse(source)) {
                case Fail(String reason) -> {return new Fail<InputString, BinaryOp, String>(reason);
                }
                case Ok(TokenString token, InputString rest) -> {
                    BinaryOp op = switch (token.value()) {
                        case "<=" -> BinaryOp.LTE;
                        case ">=" -> BinaryOp.GTE;
                        case "<>" -> BinaryOp.NEQ;
                        case "="  -> BinaryOp.EQ;
                        case "<"  -> BinaryOp.LT;
                        case ">"  -> BinaryOp.GT;
                        default -> throw new IllegalStateException();
                    };
                    return new Ok<InputString, BinaryOp, String>(op, rest);
                }
            }
        };
    }


    // comparison := simpleExpr comparisonOp simpleExpr

    static Parser<InputString, Expr, String> comparisonExpression() {

        var tail = Parsers.Then(comparisonOperator(), op ->
                Parsers.Then(simpleExpr(), right ->
                        source -> new Ok<>(new ComparisonTail(op, right), source)
                )
        );

        return Parsers.Then(simpleExpr(), left ->
                Parsers.Then(tail, rest ->
                        source -> new Ok<InputString, Expr, String>(new Binary(rest.op(), left, rest.right()), source)
                )
        );
    }


    // where := "WHERE" comparison
    static Parser<InputString, WhereClause, String> whereClause() {
        return Parsers.Then(
                Lexers.Keyword("WHERE"), token ->
                        Parsers.Then(
                                comparisonExpression(), expr ->
                                        source -> new Ok<>(new WhereClause(expr), source)
                        )
        );
    }

    // create := "CREATE" patterns

    static Parser<InputString, UpdateClause, String> createClause() {
        return Parsers.Then(
                Lexers.Keyword("CREATE"), token ->
                        Parsers.Then(
                                pattern(), patterns ->
                                        source -> new Ok<>(new CreateClause(patterns), source)
                        )
        );
    }


    // deleteVariables := ID ("," ID)*
    static Parser<InputString, List<String>, String> deleteVariables() {

        Parser<InputString, String, String> variable =
                source -> switch (Lexers.Id().parse(source)) {
                    case Fail(String reason) -> new Fail<>(reason);
                    case Ok(TokenString token, InputString rest) -> new Ok<>(token.value(), rest);
                };

        var remainingVariables = Parsers.Star(Parsers.Then(Lexers.Symbol(","), comma -> variable));

        return Parsers.Then(variable, first ->
                Parsers.Then(remainingVariables, rest -> {
                    var variables = new ArrayList<String>();
                    variables.add(first);
                    variables.addAll(rest);
                    return source -> new Ok<>(List.copyOf(variables), source);
                })
        );
    }

    // delete := "DELETE" deleteVariables

    static Parser<InputString, UpdateClause, String> deleteClause() {

        return Parsers.Then(
                Lexers.Keyword("DELETE"), token ->
                        Parsers.Then(
                                deleteVariables(), variables ->
                                        source -> new Ok<>(new DeleteClause(variables), source)
                        )
        );
    }


    // update := create | delete
    static Parser<InputString, UpdateClause, String> updatingClause() {
        return Parsers.RFlat(
                Parsers.Or(createClause(), deleteClause()),
                reasons -> String.join(", ", reasons)
        );
    }

    // updates := update*
    static Parser<InputString, List<UpdateClause>, String> updates() {
        return Parsers.Star(updatingClause());
    }


    static Parser<InputString, List<ProjectionItem>, String> returnClause() {

        return Parsers.Then(
                Lexers.Keyword("RETURN"), token ->
                        Parsers.Then(
                                projectionItems(), items ->
                                        source -> new Ok<>(items, source)
                        )
        );
    }


    static Parser<InputString, MatchWhere, String> matchClause() {

        var optionalWhere = Parsers.RFlat(
                Parsers.Or(
                        Parsers.Then(whereClause(), where ->
                                source -> new Ok<>(Optional.of(where), source)
                        ),
                        source -> new Ok<>(Optional.<WhereClause>empty(), source)
                ),
                reasons -> String.join(", ", reasons)
        );

        return Parsers.Then(Lexers.Keyword("MATCH"), match ->
                Parsers.Then(pattern(), patterns ->
                        Parsers.Then(optionalWhere, where ->
                                source -> new Ok<>(new MatchWhere(patterns, where), source)
                        )
                )
        );
    }

    static Parser<InputString, MatchWhere, String> readingClause() {
        return matchClause();
    }


    static Parser<InputString, Query, String> singlePartQuery() {

        var matchAndWhere = readingClause();

        var queryHead = Parsers.Then(matchAndWhere, mw ->
                Parsers.Then(updates(), updates ->
                        source -> new Ok<>(new QueryHead(mw.patterns(), mw.where(), updates), source)
                )
        );

        var projection = returnClause();

        return Parsers.Then(queryHead, head ->
                Parsers.Then(projection, items ->
                        source -> new Ok<>(
                                new Query(
                                        new MatchClause(head.patterns()),
                                        head.where(),
                                        head.updates(), new ReturnClause(
                                                new Projection(items, List.of()))), source
                        )
                )
        );
    }


    static Parser<InputString, Query, String> singleQuery() {
        return singlePartQuery();
    }

    static Parser<InputString, Query, String> regularQuery() {
        return singleQuery();
    }

    // query := MATCH patterns where? updates* RETURN items EOF
    static Parser<InputString, Query, String> cyphail() {

        return Parsers.Then(regularQuery(), query ->
                Parsers.Then(Lexers.Eof(), eof ->
                        source -> new Ok<InputString, Query, String>(query, source)
                )
        );
    }

    public static Result<InputString, Query, String> parse(String text) {
        return cyphail().parse(new InputString(text, 0));
    }
}