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

    private record MatchWhere(List<PatternNode> patterns, Optional<WhereClause> where) {}

    // node := "(" ID (":" ID)+ properties? ")"

    static Parser<InputString, PatternNode, String> nodePattern() {

        var label = Parsers.TFlat(Parsers.Seq(Lexers.Symbol(":"), Lexers.Id()), tokens -> tokens.get(1));
        var beginning = Parsers.Seq(Lexers.Symbol("("), Lexers.Id(), label);

        Parser<InputString, List<PropertyEntry>, String> optionalProperties =
                source -> {String remaining = source.input().substring(source.index()).stripLeading();
                    if (remaining.startsWith("{")) {return properties().parse(source);}
                    return new Ok<>(List.of(), source);
                };

        return source -> {
            switch (beginning.parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(List<TokenString> tokens, InputString rest) -> {

                    switch (Parsers.Star(label).parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(List<TokenString> otherLabels, InputString remaining) -> {

                            switch (optionalProperties.parse(remaining)) {
                                case Fail(String reason) -> {return new Fail<>(reason);}
                                case Ok(List<PropertyEntry> props, InputString next) -> {

                                    switch (Lexers.Symbol(")").parse(next)) {
                                        case Fail(String reason) -> {return new Fail<>(reason);}
                                        case Ok(TokenString close, InputString finalRest) -> {

                                            var labels = new ArrayList<String>();
                                            labels.add(tokens.get(2).value());
                                            for (var token : otherLabels) {labels.add(token.value());}
                                            return new Ok<>(new PatternNode(tokens.get(1).value(), List.copyOf(labels), props), finalRest);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        };
    }

    // property := ID "." ID
    static Parser<InputString, Expr, String> property() {

        var property = Parsers.Seq(Lexers.Id(), Lexers.Symbol("."), Lexers.Id());

        return (InputString source) -> {
            return switch (property.parse(source)) {
                case Fail(String reason) -> new Fail<InputString, Expr, String>(reason);
                case Ok(List<TokenString> tokens, InputString rest) ->
                        new Ok<InputString, Expr, String>(new PropertyAccess(tokens.get(0).value(), tokens.get(2).value()), rest);
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

        var key = Parsers.Seq(Lexers.Id(), Lexers.Symbol(":"));

        return source -> switch (key.parse(source)) {
            case Fail(String reason) -> new Fail<InputString, PropertyEntry, String>(reason);
            case Ok(List<TokenString> tokens, InputString rest) ->

                    switch (simpleExpr().parse(rest)) {
                        case Fail(String reason) -> new Fail<InputString, PropertyEntry, String>(reason);
                        case Ok(Expr value, InputString finalRest) ->
                                new Ok<InputString, PropertyEntry, String>(new PropertyEntry(tokens.get(0).value(), value), finalRest);
                    };
        };
    }

    // propertyEntries := propertyEntry ("," propertyEntry)*
    static Parser<InputString, List<PropertyEntry>, String> propertyEntries() {

        Parser<InputString, PropertyEntry, String> itemWithComma = source -> {
                    switch (Lexers.Symbol(",").parse(source)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(TokenString comma, InputString rest) -> {return propertyEntry().parse(rest);}
                    }
                };

        var remainingProperties = Parsers.Star(itemWithComma);

        return source -> {switch (propertyEntry().parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(PropertyEntry first, InputString rest) -> {

                    switch (remainingProperties.parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(List<PropertyEntry> remaining, InputString finalRest) -> {
                            var properties = new ArrayList<PropertyEntry>();
                            properties.add(first);
                            properties.addAll(remaining);
                            return new Ok<>(List.copyOf(properties), finalRest);
                        }
                    }
                }
            }
        };
    }

    // properties := "{" propertyEntries "}"
    static Parser<InputString, List<PropertyEntry>, String> properties() {

        return source -> {switch (Lexers.Symbol("{").parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(TokenString open, InputString rest) -> {

                    switch (propertyEntries().parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(List<PropertyEntry> entries, InputString remaining) -> {

                            switch (Lexers.Symbol("}").parse(remaining)) {
                                case Fail(String reason) -> {return new Fail<>(reason);}
                                case Ok(TokenString close, InputString finalRest) -> {return new Ok<>(entries, finalRest);}
                            }
                        }
                    }
                }
            }
        };
    }

    // patterns := node ("," node)*
    static Parser<InputString, List<PatternNode>, String> pattern() {

        Parser<InputString, PatternNode, String> patternWithComma =
                source -> switch (Lexers.Symbol(",").parse(source)) {
                    case Fail(String reason) -> new Fail<>(reason);
                    case Ok(TokenString comma, InputString rest) -> nodePattern().parse(rest);};

        var remainingPatterns = Parsers.Star(patternWithComma);

        return source -> {switch (nodePattern().parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(PatternNode first, InputString rest) -> {

                    switch (remainingPatterns.parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(List<PatternNode> remaining, InputString finalRest) -> {
                            var patterns = new ArrayList<PatternNode>();
                            patterns.add(first);
                            patterns.addAll(remaining);
                            return new Ok<>(List.copyOf(patterns), finalRest);
                        }
                    }
                }
            }
        };
    }

    // alias := ("AS" ID)?
    static Parser<InputString, Optional<String>, String> alias() {

        var aliasParser = Parsers.Seq(Lexers.Keyword("AS"), Lexers.Id());

        Parser<InputString, Optional<String>, String> withAlias = source -> switch (aliasParser.parse(source)) {
                    case Fail(String reason) -> new Fail<>(reason);
                    case Ok(List<TokenString> tokens, InputString rest) -> new Ok<>(Optional.of(tokens.get(1).value()), rest);
                };

        return Parsers.RFlat(Parsers.Or(withAlias, source -> new Ok<>(Optional.empty(), source)),
                reasons -> String.join(", ", reasons));
    }

    // variableExpr := ID   (variable "pelada", sin propiedad; p.ej. RETURN q AS name)
    static Parser<InputString, Expr, String> variableExpr() {

        return (InputString source) -> {
            return switch (Lexers.Id().parse(source)) {
                case Fail(String reason) -> new Fail<InputString, Expr, String>(reason);
                case Ok(TokenString token, InputString rest) -> new Ok<InputString, Expr, String>(new Var(token.value()), rest);
            };
        };
    }

    // projectionExpr := property | variableExpr
    static Parser<InputString, Expr, String> projectionExpr() {
        return Parsers.RFlat(Parsers.Or(property(), variableExpr()), reasons -> String.join(", ", reasons));
    }

    // item := projectionExpr alias
    static Parser<InputString, ProjectionItem, String> projectionItem() {

        return source -> {switch (projectionExpr().parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(Expr expr, InputString rest) -> {

                    switch (alias().parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(Optional<String> name, InputString finalRest) -> {return new Ok<>(new ProjectionItem(expr, name), finalRest);}
                    }
                }
            }
        };
    }

    // items := item ("," item)*
    static Parser<InputString, List<ProjectionItem>, String> projectionItems() {

        Parser<InputString, ProjectionItem, String> itemWithComma = source -> switch (Lexers.Symbol(",").parse(source)) {
                    case Fail(String reason) -> new Fail<>(reason);
                    case Ok(TokenString comma, InputString rest) -> projectionItem().parse(rest);
                };

        var remainingItems = Parsers.Star(itemWithComma);

        return source -> {
            switch (projectionItem().parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(ProjectionItem first, InputString rest) -> {

                    switch (remainingItems.parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(List<ProjectionItem> remaining, InputString finalRest) -> {

                            var items = new ArrayList<ProjectionItem>();
                            items.add(first);
                            items.addAll(remaining);
                            return new Ok<>(List.copyOf(items), finalRest);
                        }
                    }
                }
            }
        };
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
                case Fail(String reason) -> {return new Fail<InputString, BinaryOp, String>(reason);}
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

        return source -> {
            switch (simpleExpr().parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(Expr left, InputString rest) -> {

                    switch (comparisonOperator().parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(BinaryOp op, InputString remaining) -> {

                            switch (simpleExpr().parse(remaining)) {
                                case Fail(String reason) -> {return new Fail<>(reason);}
                                case Ok(Expr right, InputString finalRest) -> {return new Ok<>(new Binary(op, left, right), finalRest);}
                            }
                        }
                    }
                }
            }
        };
    }

    // where := "WHERE" comparison

    static Parser<InputString, WhereClause, String> whereClause() {

        return source -> {switch (Lexers.Keyword("WHERE").parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(TokenString token, InputString rest) -> {

                    switch (comparisonExpression().parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(Expr expr, InputString finalRest) -> {return new Ok<>(new WhereClause(expr), finalRest);}
                    }
                }
            }
        };
    }

    // create := "CREATE" patterns
    static Parser<InputString, UpdateClause, String> createClause() {

        return source -> {
            switch (Lexers.Keyword("CREATE").parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(TokenString token, InputString rest) -> {

                    switch (pattern().parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(List<PatternNode> patterns, InputString finalRest) -> {return new Ok<>(new CreateClause(patterns), finalRest);}
                    }
                }
            }
        };
    }

    // deleteVariables := ID ("," ID)*
    static Parser<InputString, List<String>, String> deleteVariables() {

        Parser<InputString, String, String> variable = source -> switch (Lexers.Id().parse(source)) {
                    case Fail(String reason) -> new Fail<>(reason);
                    case Ok(TokenString token, InputString rest) -> new Ok<>(token.value(), rest);
                };

        Parser<InputString, String, String> variableWithComma = source -> switch (Lexers.Symbol(",").parse(source)) {
                    case Fail(String reason) -> new Fail<>(reason);
                    case Ok(TokenString comma, InputString rest) -> variable.parse(rest);
                };

        var remainingVariables = Parsers.Star(variableWithComma);

        return source -> {switch (variable.parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(String first, InputString rest) -> {

                    switch (remainingVariables.parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(List<String> remaining, InputString finalRest) -> {
                            var variables = new ArrayList<String>();
                            variables.add(first);
                            variables.addAll(remaining);
                            return new Ok<>(List.copyOf(variables), finalRest);
                        }
                    }
                }
            }
        };
    }

    // delete := "DELETE" deleteVariables
    static Parser<InputString, UpdateClause, String> deleteClause() {

        return source -> {
            switch (Lexers.Keyword("DELETE").parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(TokenString token, InputString rest) -> {

                    switch (deleteVariables().parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(List<String> variables, InputString finalRest) -> {return new Ok<>(new DeleteClause(variables), finalRest);}
                    }
                }
            }
        };
    }

    // update := create | delete
    static Parser<InputString, UpdateClause, String> updatingClause() {
        return Parsers.RFlat(
                Parsers.Or(createClause(), deleteClause()),
                reasons -> String.join(", ", reasons));
    }

    // updates := update*
    static Parser<InputString, List<UpdateClause>, String> updates() {
        return Parsers.Star(updatingClause());
    }

    static Parser<InputString, List<ProjectionItem>, String> returnClause() {

        return source -> {
            switch (Lexers.Keyword("RETURN").parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(TokenString token, InputString rest) -> {return projectionItems().parse(rest);}
            }
        };
    }

    static Parser<InputString, MatchWhere, String> matchClause() {

        return source -> {
            switch (Lexers.Keyword("MATCH").parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(TokenString token, InputString rest) -> {

                    switch (pattern().parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(List<PatternNode> patterns, InputString remaining) -> {

                            Optional<WhereClause> where = Optional.empty();
                            InputString finalRest = remaining;

                            switch (whereClause().parse(remaining)) {
                                case Fail(String reason) -> {// WHERE es opcional
                                }
                                case Ok(WhereClause clause, InputString next) -> {where = Optional.of(clause);finalRest = next;}
                            }
                            return new Ok<>(new MatchWhere(patterns, where), finalRest);
                        }
                    }
                }
            }
        };
    }

    static Parser<InputString, MatchWhere, String> readingClause() {
        return matchClause();
    }

    static Parser<InputString, Query, String> singlePartQuery() {

        return source -> {
            switch (readingClause().parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(MatchWhere mw, InputString rest) -> {

                    switch (updates().parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(List<UpdateClause> updates, InputString remaining) -> {

                            switch (returnClause().parse(remaining)) {
                                case Fail(String reason) -> {return new Fail<>(reason);}
                                case Ok(List<ProjectionItem> items, InputString finalRest) -> {

                                    Query query = new Query(
                                            new MatchClause(mw.patterns()),
                                            mw.where(), updates,
                                            new ReturnClause(new Projection(items, List.of())));
                                    return new Ok<>(query, finalRest);
                                }
                            }
                        }
                    }
                }
            }
        };
    }

    static Parser<InputString, Query, String> singleQuery() {
        return singlePartQuery();
    }

    static Parser<InputString, Query, String> regularQuery() {
        return singleQuery();
    }

    // query := MATCH patterns where? updates* RETURN items EOF
    static Parser<InputString, Query, String> cyphail() {

        return source -> {
            switch (regularQuery().parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(Query query, InputString rest) -> {

                    switch (Lexers.Eof().parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(TokenString token, InputString finalRest) -> {return new Ok<>(query, finalRest);}
                    }
                }
            }
        };
    }

    public static Result<InputString, Query, String> parse(String text) {
        return cyphail().parse(new InputString(text, 0));
    }
}