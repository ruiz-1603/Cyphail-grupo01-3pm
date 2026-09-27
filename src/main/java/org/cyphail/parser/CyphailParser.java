package org.cyphail.parser;

import org.cyphail.ast.*;
import org.cyphail.parser.combinators.Parsers;
import org.cyphail.parser.core.InputString;
import org.cyphail.parser.core.Parser;
import org.cyphail.parser.core.Result;
import org.cyphail.parser.lexer.Lexers;

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

    private record NodeHead(String variable, List<String> labels) {}

    // node := "(" ID (":" ID)+ properties? ")"
    static Parser<InputString, PatternNode, String> Node() {
        var variable = Parsers.Right(Lexers.Symbol("("), Lexers.Id());

        var labels = Parsers.Map(
                Parsers.Plus(Parsers.Right(Lexers.Symbol(":"), Lexers.Id())),
                tokens -> tokens.stream().map(token -> token.value()).toList()
        );

        var head = Parsers.Combine(
                variable, labels,
                (varToken, labelList) -> new NodeHead(varToken.value(), labelList)
        );

        var node = Parsers.Combine(
                head, Parsers.Opt(Properties()),
                (nodeHead, maybeProperties) ->
                        new PatternNode(nodeHead.variable(), nodeHead.labels(),
                                maybeProperties.orElse(List.of()))
        );

        return Parsers.Left(node, Lexers.Symbol(")"));
    }

    // property := ID "." ID
    static Parser<InputString, Expr, String> Property() {
        return Parsers.Map(
                Parsers.Sequence(Lexers.Id(), Lexers.Symbol("."), Lexers.Id()),
                tokens -> new PropertyAccess(tokens.get(0).value(), tokens.get(2).value())
        );
    }

    // number := NUM
    static Parser<InputString, Expr, String> Number() {
        return Parsers.Map(
                Lexers.Number(),
                token -> new NumberLit(Long.parseLong(token.value()))
        );
    }

    // stringExpr := STRING
    static Parser<InputString, Expr, String> StringExpr() {
        return Parsers.Map(
                Lexers.StringLiteral(),
                token -> new StringLit(token.value())
        );
    }

    // simpleExpr := property | number | string
    static Parser<InputString, Expr, String> SimpleExpr() {
        return Parsers.Or(Property(), Parsers.Or(Number(), StringExpr()));
    }

    // propertyEntry := ID ":" simpleExpr
    static Parser<InputString, PropertyEntry, String> PropertyEntry() {
        var key = Parsers.Left(Lexers.Id(), Lexers.Symbol(":"));

        return Parsers.Combine(
                key, SimpleExpr(),
                (keyToken, value) -> new PropertyEntry(keyToken.value(), value)
        );
    }

    // propertyEntries := propertyEntry ("," propertyEntry)*
    static Parser<InputString, List<PropertyEntry>, String> PropertyEntries() {
        var remainingProperties = Parsers.Star(
                Parsers.Right(Lexers.Symbol(","), PropertyEntry())
        );

        return Parsers.Combine(PropertyEntry(), remainingProperties, (first, rest) -> {
            var properties = new ArrayList<PropertyEntry>();
            properties.add(first);
            properties.addAll(rest);
            return List.copyOf(properties);
        });
    }

    // properties := "{" propertyEntries "}"
    static Parser<InputString, List<PropertyEntry>, String> Properties() {
        return Parsers.Right(
                Lexers.Symbol("{"),
                Parsers.Left(PropertyEntries(), Lexers.Symbol("}"))
        );
    }

    // patterns := node ("," node)*
    static Parser<InputString, List<PatternNode>, String> Patterns() {
        var remainingPatterns = Parsers.Star(
                Parsers.Right(Lexers.Symbol(","), Node())
        );

        return Parsers.Combine(Node(), remainingPatterns, (first, rest) -> {
            var patterns = new ArrayList<PatternNode>();
            patterns.add(first);
            patterns.addAll(rest);
            return List.copyOf(patterns);
        });
    }

    // alias := ("AS" ID)?
    static Parser<InputString, Optional<String>, String> Alias() {
        return Parsers.Map(
                Parsers.Opt(Parsers.Right(Lexers.Keyword("AS"), Lexers.Id())),
                maybeToken -> maybeToken.map(token -> token.value())
        );
    }

    // variableExpr := ID   (variable "pelada", sin propiedad; p.ej. RETURN q AS name)
    static Parser<InputString, Expr, String> VariableExpr() {
        return Parsers.Map(
                Lexers.Id(),
                token -> new Var(token.value())
        );
    }

    // projectionExpr := property | variableExpr
    static Parser<InputString, Expr, String> ProjectionExpr() {
        return Parsers.Or(Property(), VariableExpr());
    }

    // item := projectionExpr alias
    static Parser<InputString, ProjectionItem, String> Item() {
        return Parsers.Combine(ProjectionExpr(), Alias(), ProjectionItem::new);
    }

    // items := item ("," item)*
    static Parser<InputString, List<ProjectionItem>, String> Items() {
        var remainingItems = Parsers.Star(
                Parsers.Right(Lexers.Symbol(","), Item())
        );

        return Parsers.Combine(Item(), remainingItems, (first, rest) -> {
            var items = new ArrayList<ProjectionItem>();
            items.add(first);
            items.addAll(rest);
            return List.copyOf(items);
        });
    }

    // comparisonOp := "<=" | ">=" | "<>" | "=" | "<" | ">"
    // Ojo con el orden: los símbolos de 2 caracteres deben probarse antes que
    // sus prefijos de 1 caracter ("<=" antes que "<", ">=" antes que ">", etc.)
    static Parser<InputString, BinaryOp, String> ComparisonOp() {
        return Parsers.Or(
                Parsers.Map(Lexers.Symbol("<="), token -> BinaryOp.LTE),
                Parsers.Or(
                        Parsers.Map(Lexers.Symbol(">="), token -> BinaryOp.GTE),
                        Parsers.Or(
                                Parsers.Map(Lexers.Symbol("<>"), token -> BinaryOp.NEQ),
                                Parsers.Or(
                                        Parsers.Map(Lexers.Symbol("="), token -> BinaryOp.EQ),
                                        Parsers.Or(
                                                Parsers.Map(Lexers.Symbol("<"), token -> BinaryOp.LT),
                                                Parsers.Map(Lexers.Symbol(">"), token -> BinaryOp.GT)
                                        )
                                )
                        )
                )
        );
    }

    // comparison := simpleExpr comparisonOp simpleExpr
    static Parser<InputString, Expr, String> Comparison() {
        var tail = Parsers.Combine(ComparisonOp(), SimpleExpr(), ComparisonTail::new);

        return Parsers.Combine(
                SimpleExpr(), tail,
                (left, rest) -> new Binary(rest.op(), left, rest.right())
        );
    }

    // where := "WHERE" comparison
    static Parser<InputString, WhereClause, String> Where() {
        return Parsers.Map(
                Parsers.Right(Lexers.Keyword("WHERE"), Comparison()),
                WhereClause::new
        );
    }

    // create := "CREATE" patterns
    static Parser<InputString, UpdateClause, String> Create() {
        return Parsers.Map(
                Parsers.Right(Lexers.Keyword("CREATE"), Patterns()),
                patterns -> new CreateClause(patterns)
        );
    }

    // deleteVariables := ID ("," ID)*
    static Parser<InputString, List<String>, String> DeleteVariables() {
        var variable = Parsers.Map(Lexers.Id(), token -> token.value());

        var remainingVariables = Parsers.Star(
                Parsers.Right(Lexers.Symbol(","), variable)
        );

        return Parsers.Combine(variable, remainingVariables, (first, rest) -> {
            var variables = new ArrayList<String>();
            variables.add(first);
            variables.addAll(rest);
            return List.copyOf(variables);
        });
    }

    // delete := "DELETE" deleteVariables
    static Parser<InputString, UpdateClause, String> Delete() {
        return Parsers.Map(
                Parsers.Right(Lexers.Keyword("DELETE"), DeleteVariables()),
                variables -> new DeleteClause(variables)
        );
    }

    // update := create | delete
    static Parser<InputString, UpdateClause, String> Update() {
        return Parsers.Or(Create(), Delete());
    }

    // updates := update*
    static Parser<InputString, List<UpdateClause>, String> Updates() {
        return Parsers.Star(Update());
    }

    // query := MATCH patterns where? updates* RETURN items EOF
    static Parser<InputString, Query, String> Query() {
        var match = Parsers.Right(Lexers.Keyword("MATCH"), Patterns());

        var matchAndWhere = Parsers.Combine(
                match, Parsers.Opt(Where()), MatchWhere::new
        );

        var queryHead = Parsers.Combine(
                matchAndWhere, Updates(),
                (mw, updates) -> new QueryHead(mw.patterns(), mw.where(), updates)
        );

        var projection = Parsers.Right(Lexers.Keyword("RETURN"), Items());

        return Parsers.Left(
                Parsers.Combine(
                        queryHead, projection,
                        (head, items) -> new Query(
                                new MatchClause(head.patterns()),
                                head.where(),
                                head.updates(),
                                new ReturnClause(new Projection(items, List.of()))
                        )
                ),
                Lexers.Eof()
        );
    }

    public static Result<InputString, Query, String> parse(String text) {
        return Query().parse(new InputString(text, 0));
    }
}