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
import java.util.function.BinaryOperator;
import java.util.function.Function;

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

    private record MatchWhere(List<PatternElement> patterns, Optional<WhereClause> where) {}

    // Contenido de "[ ... ]" de una relacion; con la direccion se vuelve un RelationshipPattern
    private record RelDetail(Optional<String> variable, List<String> types, List<PropertyEntry> properties) {

        static final RelDetail EMPTY = new RelDetail(Optional.empty(), List.of(), List.of());

        RelationshipPattern withDirection(Direction direction) {
            return new RelationshipPattern(variable, types, properties, direction);
        }
    }

    // Un operador de comparacion con su operando derecho: "> 2010"
    private record Comparison(BinaryOp op, Expr right) {}

    // clause? para clausulas que empiezan con palabra clave (MATCH, WHERE, RETURN):
    // si la palabra esta, la clausula debe estar bien formada (se reporta su error);
    // si no esta, no hay clausula y no se consume nada
    private static <T> Parser<InputString, Optional<T>, String> optionalClause(String keyword, Parser<InputString, T, String> clause) {
        return source -> switch (Lexers.Keyword(keyword).parse(source)) {
            case Fail(String reason) -> new Ok<>(Optional.empty(), source);
            case Ok(TokenString token, InputString rest) -> switch (clause.parse(source)) {
                case Fail(String reason) -> new Fail<>(reason);
                case Ok(T value, InputString next) -> new Ok<>(Optional.of(value), next);
            };
        };
    }

    // (separator item)*  : como Star, pero si aparece el separador ("," "AND" "OR" ">" ...)
    // el item que sigue es obligatorio y su error se reporta (no se retrocede en silencio)
    private static <S, T> Parser<InputString, List<T>, String> repeatAfter(
            Parser<InputString, S, String> separator, Function<S, Parser<InputString, T, String>> item) {

        return source -> {
            switch (separator.parse(source)) {
                case Fail(String reason) -> {return new Ok<>(List.of(), source);}
                case Ok(S found, InputString rest) -> {

                    switch (item.apply(found).parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(T first, InputString remaining) -> {

                            switch (repeatAfter(separator, item).parse(remaining)) {
                                case Fail(String reason) -> {return new Fail<>(reason);}
                                case Ok(List<T> others, InputString finalRest) -> {
                                    var items = new ArrayList<T>();
                                    items.add(first);
                                    items.addAll(others);
                                    return new Ok<>(List.copyOf(items), finalRest);
                                }
                            }
                        }
                    }
                }
            }
        };
    }

    // list := item ("," item)*
    private static <T> Parser<InputString, List<T>, String> commaList(Parser<InputString, T, String> item) {

        var remainingItems = repeatAfter(Lexers.Symbol(","), comma -> item);

        return source -> {
            switch (item.parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(T first, InputString rest) -> {

                    switch (remainingItems.parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(List<T> remaining, InputString finalRest) -> {
                            var items = new ArrayList<T>();
                            items.add(first);
                            items.addAll(remaining);
                            return new Ok<>(List.copyOf(items), finalRest);
                        }
                    }
                }
            }
        };
    }

    // variable := ID   (opcional en nodos y relaciones)
    static Parser<InputString, Optional<String>, String> optionalVariable() {

        Parser<InputString, Optional<String>, String> variable = source -> switch (Lexers.Id().parse(source)) {
            case Fail(String reason) -> new Fail<>(reason);
            case Ok(TokenString token, InputString rest) -> new Ok<>(Optional.of(token.value()), rest);
        };

        return Parsers.RFlat(Parsers.Or(variable, source -> new Ok<>(Optional.empty(), source)),
                reasons -> String.join(", ", reasons));
    }

    // nodeLabels := (":" labelName)*
    static Parser<InputString, List<String>, String> nodeLabels() {

        var label = Parsers.TFlat(Parsers.Seq(Lexers.Symbol(":"), Lexers.Id()), tokens -> tokens.get(1));

        return source -> switch (Parsers.Star(label).parse(source)) {
            case Fail(String reason) -> new Fail<>(reason);
            case Ok(List<TokenString> tokens, InputString rest) ->
                    new Ok<>(tokens.stream().map(TokenString::value).toList(), rest);
        };
    }

    // properties? : si viene "{" el mapa debe estar bien formado; si no, no hay propiedades
    static Parser<InputString, List<PropertyEntry>, String> optionalProperties() {

        return source -> switch (Lexers.Symbol("{").parse(source)) {
            case Ok(TokenString open, InputString rest) -> properties().parse(source);
            case Fail(String reason) -> new Ok<>(List.of(), source);
        };
    }

    // node := "(" variable? nodeLabels? properties? ")"
    static Parser<InputString, PatternNode, String> nodePattern() {

        return source -> {
            switch (Lexers.Symbol("(").parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(TokenString open, InputString rest) -> {

                    switch (optionalVariable().parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(Optional<String> variable, InputString afterVariable) -> {

                            switch (nodeLabels().parse(afterVariable)) {
                                case Fail(String reason) -> {return new Fail<>(reason);}
                                case Ok(List<String> labels, InputString afterLabels) -> {

                                    switch (optionalProperties().parse(afterLabels)) {
                                        case Fail(String reason) -> {return new Fail<>(reason);}
                                        case Ok(List<PropertyEntry> props, InputString next) -> {

                                            switch (Lexers.Symbol(")").parse(next)) {
                                                case Fail(String reason) -> {return new Fail<>(reason);}
                                                case Ok(TokenString close, InputString finalRest) -> {
                                                    return new Ok<>(new PatternNode(variable, labels, props), finalRest);
                                                }
                                            }
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

    // relTypes := (":" relTypeName)?   (un solo tipo; los tipos alternativos "|" estan fuera de P2)
    static Parser<InputString, List<String>, String> relTypes() {

        var type = Parsers.TFlat(Parsers.Seq(Lexers.Symbol(":"), Lexers.Id()), tokens -> tokens.get(1));

        Parser<InputString, List<String>, String> oneType = source -> switch (type.parse(source)) {
            case Fail(String reason) -> new Fail<>(reason);
            case Ok(TokenString token, InputString rest) -> new Ok<>(List.of(token.value()), rest);
        };

        return Parsers.RFlat(Parsers.Or(oneType, source -> new Ok<>(List.of(), source)),
                reasons -> String.join(", ", reasons));
    }

    // relationshipDetail := "[" variable? relTypes? relProperties? "]"
    static Parser<InputString, RelDetail, String> relationshipDetail() {

        return source -> {
            switch (Lexers.Symbol("[").parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(TokenString open, InputString rest) -> {

                    switch (optionalVariable().parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(Optional<String> variable, InputString afterVariable) -> {

                            switch (relTypes().parse(afterVariable)) {
                                case Fail(String reason) -> {return new Fail<>(reason);}
                                case Ok(List<String> types, InputString afterTypes) -> {

                                    switch (optionalProperties().parse(afterTypes)) {
                                        case Fail(String reason) -> {return new Fail<>(reason);}
                                        case Ok(List<PropertyEntry> props, InputString next) -> {

                                            switch (Lexers.Symbol("]").parse(next)) {
                                                case Fail(String reason) -> {return new Fail<>(reason);}
                                                case Ok(TokenString close, InputString finalRest) -> {
                                                    return new Ok<>(new RelDetail(variable, types, props), finalRest);
                                                }
                                            }
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

    // relationshipDetail? : si viene "[" el detalle debe estar bien formado
    static Parser<InputString, RelDetail, String> optionalRelationshipDetail() {

        return source -> switch (Lexers.Symbol("[").parse(source)) {
            case Ok(TokenString open, InputString rest) -> relationshipDetail().parse(source);
            case Fail(String reason) -> new Ok<>(RelDetail.EMPTY, source);
        };
    }

    // Final de una relacion que empezo con "-":  "->" es OUT, "-" es NONE ("->" se prueba primero)
    static Parser<InputString, Direction, String> arrowEnd() {

        Parser<InputString, Direction, String> out = source -> switch (Lexers.Symbol("->").parse(source)) {
            case Fail(String reason) -> new Fail<>(reason);
            case Ok(TokenString arrow, InputString rest) -> new Ok<>(Direction.OUT, rest);
        };

        Parser<InputString, Direction, String> none = source -> switch (Lexers.Symbol("-").parse(source)) {
            case Fail(String reason) -> new Fail<>(reason);
            case Ok(TokenString dash, InputString rest) -> new Ok<>(Direction.NONE, rest);
        };

        return Parsers.RFlat(Parsers.Or(out, none), reasons -> String.join(", ", reasons));
    }

    // relationshipPattern := "<-" relationshipDetail? "-"
    //                      | "-" relationshipDetail? ("->" | "-")
    static Parser<InputString, RelationshipPattern, String> relationshipPattern() {

        Parser<InputString, RelationshipPattern, String> incoming = source -> {
            switch (Lexers.Symbol("<-").parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(TokenString arrow, InputString rest) -> {

                    switch (optionalRelationshipDetail().parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(RelDetail detail, InputString remaining) -> {

                            switch (Lexers.Symbol("-").parse(remaining)) {
                                case Fail(String reason) -> {return new Fail<>(reason);}
                                case Ok(TokenString dash, InputString finalRest) -> {
                                    return new Ok<>(detail.withDirection(Direction.IN), finalRest);
                                }
                            }
                        }
                    }
                }
            }
        };

        Parser<InputString, RelationshipPattern, String> outgoingOrUndirected = source -> {
            switch (Lexers.Symbol("-").parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(TokenString dash, InputString rest) -> {

                    switch (optionalRelationshipDetail().parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(RelDetail detail, InputString remaining) -> {

                            switch (arrowEnd().parse(remaining)) {
                                case Fail(String reason) -> {return new Fail<>(reason);}
                                case Ok(Direction direction, InputString finalRest) -> {
                                    return new Ok<>(detail.withDirection(direction), finalRest);
                                }
                            }
                        }
                    }
                }
            }
        };

        return Parsers.RFlat(Parsers.Or(incoming, outgoingOrUndirected), reasons -> String.join(", ", reasons));
    }

    // patternElement := node (relationshipPattern node)?
    // (un solo arco por patron: los patrones encadenados estan fuera de P2)
    static Parser<InputString, PatternElement, String> patternElement() {

        return source -> {
            switch (nodePattern().parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(PatternNode left, InputString rest) -> {

                    switch (relationshipPattern().parse(rest)) {
                        case Fail(String reason) -> {return new Ok<>(left, rest);} // patron de un solo nodo
                        case Ok(RelationshipPattern relationship, InputString remaining) -> {

                            switch (nodePattern().parse(remaining)) {
                                case Fail(String reason) -> {return new Fail<>(reason);}
                                case Ok(PatternNode right, InputString finalRest) -> {
                                    return new Ok<>(new PathPattern(left, relationship, right), finalRest);
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

    // decimal := DECIMAL
    static Parser<InputString, Expr, String> decimal() {

        return (InputString source) -> {
            return switch (Lexers.Decimal().parse(source)) {
                case Fail(String reason) -> new Fail<InputString, Expr, String>(reason);
                case Ok(TokenString token, InputString rest) ->
                        new Ok<InputString, Expr, String>(new DecimalLit(Double.parseDouble(token.value())), rest);
            };
        };
    }

    // booleanLiteral := "TRUE" | "FALSE"
    static Parser<InputString, Expr, String> booleanExpr() {

        return (InputString source) -> {
            return switch (Lexers.BooleanLiteral().parse(source)) {
                case Fail(String reason) -> new Fail<InputString, Expr, String>(reason);
                case Ok(TokenString token, InputString rest) ->
                        new Ok<InputString, Expr, String>(new BoolLit(Boolean.parseBoolean(token.value())), rest);
            };
        };
    }

    // simpleExpr := property | decimal | number | string | boolean
    // (decimal antes que number: si no, "25.5" se leeria como 25 y quedaria ".5")
    static Parser<InputString, Expr, String> simpleExpr() {
        return Parsers.RFlat(Parsers.Or(property(), decimal(), number(), stringExpr(), booleanExpr()),
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

    // pattern := patternElement ("," patternElement)*
    static Parser<InputString, List<PatternElement>, String> pattern() {
        return commaList(patternElement());
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

    // atom := "(" expression ")" | simpleExpr | variable
    static Parser<InputString, Expr, String> atom() {

        // expression() se pide dentro del lambda (al parsear), no al construir: la gramatica es recursiva
        Parser<InputString, Expr, String> parenthesized = source -> {
            switch (Lexers.Symbol("(").parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(TokenString open, InputString rest) -> {

                    switch (expression().parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(Expr inner, InputString remaining) -> {

                            switch (Lexers.Symbol(")").parse(remaining)) {
                                case Fail(String reason) -> {return new Fail<>(reason);}
                                case Ok(TokenString close, InputString finalRest) -> {return new Ok<>(inner, finalRest);}
                            }
                        }
                    }
                }
            }
        };

        return Parsers.RFlat(Parsers.Or(parenthesized, simpleExpr(), variableExpr()),
                reasons -> String.join(", ", reasons));
    }

    // comparison := atom (comparisonOp atom)*   ->  a < b < c se agrupa a la izquierda
    static Parser<InputString, Expr, String> comparisonExpression() {

        // si aparece el operador, el operando derecho es obligatorio
        Function<BinaryOp, Parser<InputString, Comparison, String>> operand = op -> source -> switch (atom().parse(source)) {
            case Fail(String reason) -> new Fail<>(reason);
            case Ok(Expr right, InputString rest) -> new Ok<>(new Comparison(op, right), rest);
        };

        var comparisons = repeatAfter(comparisonOperator(), operand);

        return source -> {
            switch (atom().parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(Expr left, InputString rest) -> {

                    switch (comparisons.parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(List<Comparison> others, InputString finalRest) -> {
                            Expr result = others.stream()
                                    .reduce(left, (acc, c) -> new Binary(c.op(), acc, c.right()), (a, b) -> b);
                            return new Ok<>(result, finalRest);
                        }
                    }
                }
            }
        };
    }

    // notExpression := "NOT"* comparison
    static Parser<InputString, Expr, String> notExpression() {

        var nots = Parsers.Star(Lexers.Keyword("NOT"));

        return source -> {
            switch (nots.parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(List<TokenString> tokens, InputString rest) -> {

                    switch (comparisonExpression().parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(Expr operand, InputString finalRest) -> {
                            Expr result = tokens.stream().reduce(operand, (acc, not) -> new Not(acc), (a, b) -> b);
                            return new Ok<>(result, finalRest);
                        }
                    }
                }
            }
        };
    }

    // Reconoce  operand (keyword operand)*  y agrupa a la izquierda: a OR b OR c = (a OR b) OR c
    private static Parser<InputString, Expr, String> leftAssociative(
            String keyword, Parser<InputString, Expr, String> operand, BinaryOperator<Expr> combine) {

        // si aparece la palabra clave, el operando que sigue es obligatorio
        var others = repeatAfter(Lexers.Keyword(keyword), token -> operand);

        return source -> {
            switch (operand.parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(Expr first, InputString rest) -> {

                    switch (others.parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(List<Expr> remaining, InputString finalRest) -> {
                            return new Ok<>(remaining.stream().reduce(first, combine, (a, b) -> b), finalRest);
                        }
                    }
                }
            }
        };
    }

    // andExpression := notExpression ("AND" notExpression)*
    static Parser<InputString, Expr, String> andExpression() {
        return leftAssociative("AND", notExpression(), And::new);
    }

    // orExpression := andExpression ("OR" andExpression)*
    static Parser<InputString, Expr, String> orExpression() {
        return leftAssociative("OR", andExpression(), Or::new);
    }

    // expression := orExpression   (precedencia: NOT > AND > OR)
    static Parser<InputString, Expr, String> expression() {
        return orExpression();
    }

    // where := "WHERE" expression

    static Parser<InputString, WhereClause, String> whereClause() {

        return source -> {switch (Lexers.Keyword("WHERE").parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(TokenString token, InputString rest) -> {

                    switch (expression().parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(Expr expr, InputString finalRest) -> {return new Ok<>(new WhereClause(expr), finalRest);}
                    }
                }
            }
        };
    }

    // create := "CREATE" pattern
    static Parser<InputString, UpdateClause, String> createClause() {

        return source -> {
            switch (Lexers.Keyword("CREATE").parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(TokenString token, InputString rest) -> {

                    switch (pattern().parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(List<PatternElement> patterns, InputString finalRest) -> {return new Ok<>(new CreateClause(patterns), finalRest);}
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

    // setItem := property "=" expression     # SetProperty
    //          | variable nodeLabels          # SetLabels
    static Parser<InputString, SetItem, String> setItem() {

        Parser<InputString, SetItem, String> setProperty = source -> {
            switch (property().parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(Expr target, InputString rest) -> {

                    switch (Lexers.Symbol("=").parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(TokenString equals, InputString remaining) -> {

                            switch (expression().parse(remaining)) {
                                case Fail(String reason) -> {return new Fail<>(reason);}
                                case Ok(Expr value, InputString finalRest) -> {
                                    return new Ok<>(new SetProperty((PropertyAccess) target, value), finalRest);
                                }
                            }
                        }
                    }
                }
            }
        };

        Parser<InputString, SetItem, String> setLabels = source -> switch (variableWithLabels().parse(source)) {
            case Fail(String reason) -> new Fail<>(reason);
            case Ok(SetLabels labels, InputString rest) -> new Ok<>(labels, rest);
        };

        return Parsers.RFlat(Parsers.Or(setProperty, setLabels), reasons -> String.join(", ", reasons));
    }

    // variable nodeLabels   (al menos una etiqueta), p.ej. p:Vip:Activo
    static Parser<InputString, SetLabels, String> variableWithLabels() {

        return source -> {
            switch (Lexers.Id().parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(TokenString variable, InputString rest) -> {

                    switch (nodeLabels().parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(List<String> labels, InputString finalRest) -> {
                            if (labels.isEmpty()) {
                                return new Fail<>("Expected ':' at position " + rest.index());
                            }
                            return new Ok<>(new SetLabels(variable.value(), labels), finalRest);
                        }
                    }
                }
            }
        };
    }

    // set := "SET" setItem ("," setItem)*
    static Parser<InputString, UpdateClause, String> setClause() {

        return source -> {
            switch (Lexers.Keyword("SET").parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(TokenString token, InputString rest) -> {

                    switch (commaList(setItem()).parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(List<SetItem> items, InputString finalRest) -> {return new Ok<>(new SetClause(items), finalRest);}
                    }
                }
            }
        };
    }

    // removeItem := variable nodeLabels   # RemoveLabels
    //             | property              # RemoveProperty
    static Parser<InputString, RemoveItem, String> removeItem() {

        Parser<InputString, RemoveItem, String> removeLabels = source -> switch (variableWithLabels().parse(source)) {
            case Fail(String reason) -> new Fail<>(reason);
            case Ok(SetLabels labels, InputString rest) ->
                    new Ok<>(new RemoveLabels(labels.variable(), labels.labels()), rest);
        };

        Parser<InputString, RemoveItem, String> removeProperty = source -> switch (property().parse(source)) {
            case Fail(String reason) -> new Fail<>(reason);
            case Ok(Expr target, InputString rest) -> new Ok<>(new RemoveProperty((PropertyAccess) target), rest);
        };

        return Parsers.RFlat(Parsers.Or(removeLabels, removeProperty), reasons -> String.join(", ", reasons));
    }

    // remove := "REMOVE" removeItem ("," removeItem)*
    static Parser<InputString, UpdateClause, String> removeClause() {

        return source -> {
            switch (Lexers.Keyword("REMOVE").parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(TokenString token, InputString rest) -> {

                    switch (commaList(removeItem()).parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(List<RemoveItem> items, InputString finalRest) -> {return new Ok<>(new RemoveClause(items), finalRest);}
                    }
                }
            }
        };
    }

    // update := create | set | delete | remove
    static Parser<InputString, UpdateClause, String> updatingClause() {
        return Parsers.RFlat(
                Parsers.Or(createClause(), setClause(), deleteClause(), removeClause()),
                reasons -> String.join(", ", reasons));
    }

    // Una clausula de actualizacion empieza con una de estas palabras clave
    private static boolean startsUpdate(InputString source) {
        var keyword = Parsers.Or(Lexers.Keyword("CREATE"), Lexers.Keyword("SET"),
                Lexers.Keyword("DELETE"), Lexers.Keyword("REMOVE"));
        return keyword.parse(source) instanceof Ok;
    }

    // updates := update*
    // Si la palabra clave esta, la clausula debe estar bien formada (asi se reporta su error)
    static Parser<InputString, List<UpdateClause>, String> updates() {

        return source -> {
            if (!startsUpdate(source)) {
                return new Ok<>(List.of(), source);
            }
            switch (updatingClause().parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(UpdateClause first, InputString rest) -> {

                    switch (updates().parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(List<UpdateClause> others, InputString finalRest) -> {
                            var all = new ArrayList<UpdateClause>();
                            all.add(first);
                            all.addAll(others);
                            return new Ok<>(List.copyOf(all), finalRest);
                        }
                    }
                }
            }
        };
    }

    // return := "RETURN" items
    static Parser<InputString, List<ProjectionItem>, String> returnClause() {

        return source -> {
            switch (Lexers.Keyword("RETURN").parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(TokenString token, InputString rest) -> {return projectionItems().parse(rest);}
            }
        };
    }

    // match := "MATCH" pattern where?
    static Parser<InputString, MatchWhere, String> matchClause() {

        return source -> {
            switch (Lexers.Keyword("MATCH").parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(TokenString token, InputString rest) -> {

                    switch (pattern().parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(List<PatternElement> patterns, InputString remaining) -> {

                            switch (optionalClause("WHERE", whereClause()).parse(remaining)) {
                                case Fail(String reason) -> {return new Fail<>(reason);}
                                case Ok(Optional<WhereClause> where, InputString finalRest) -> {
                                    return new Ok<>(new MatchWhere(patterns, where), finalRest);
                                }
                            }
                        }
                    }
                }
            }
        };
    }

    // readingClause := match
    static Parser<InputString, MatchWhere, String> readingClause() {
        return matchClause();
    }

    // singlePartQuery := readingClause? updatingClause* returnClause?
    // (el AST de P2 tiene un solo MATCH; un query debe traer al menos una clausula)
    static Parser<InputString, Query, String> singlePartQuery() {

        return source -> {
            switch (optionalClause("MATCH", readingClause()).parse(source)) {
                case Fail(String reason) -> {return new Fail<>(reason);}
                case Ok(Optional<MatchWhere> mw, InputString rest) -> {

                    switch (updates().parse(rest)) {
                        case Fail(String reason) -> {return new Fail<>(reason);}
                        case Ok(List<UpdateClause> updates, InputString remaining) -> {

                            switch (optionalClause("RETURN", returnClause()).parse(remaining)) {
                                case Fail(String reason) -> {return new Fail<>(reason);}
                                case Ok(Optional<List<ProjectionItem>> items, InputString finalRest) -> {

                                    if (mw.isEmpty() && updates.isEmpty() && items.isEmpty()) {
                                        return new Fail<>("Expected 'MATCH', 'CREATE' or 'RETURN' at position " + source.index());
                                    }
                                    Query query = new Query(
                                            mw.map(m -> new MatchClause(m.patterns())),
                                            mw.flatMap(MatchWhere::where),
                                            updates,
                                            items.map(list -> new ReturnClause(new Projection(list, List.of()))));
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

    // cyphail := regularQuery EOF
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