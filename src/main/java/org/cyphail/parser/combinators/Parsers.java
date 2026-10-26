
package org.cyphail.parser.combinators;

import org.cyphail.parser.core.Fail;
import org.cyphail.parser.core.Ok;
import org.cyphail.parser.core.Parser;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Function;

public final class Parsers {

    private Parsers() {}

    // Intenta varios parsers hasta que uno funcione
    @SafeVarargs
    public static <I, T, R> Parser<I, T, List<R>> Or(Parser<I, T, R>... parsers) {

        Parser<I, T, List<R>> parser = (I source) -> {
            var reasons = new ArrayList<R>();
            for (var p : parsers) {
                switch (p.parse(source)) {
                    case Fail(R reason) -> {reasons.add(reason);}
                    case Ok(T token, I rest) -> {return new Ok<I, T, List<R>>(token, rest);}
                }
            }

            return new Fail<I, T, List<R>>(reasons);
        };

        return parser;
    }

    // Ejecuta varios parsers en secuencia
    @SafeVarargs
    public static <I, T, R> Parser<I, List<T>, R> Seq(Parser<I, T, R>... parsers) {

        Parser<I, List<T>, R> parser = (I source) -> {
            List<T> results = new ArrayList<>();
            for (var p : parsers) {
                switch (p.parse(source)) {
                    case Fail(R reason) -> {return new Fail<I, List<T>, R>(reason);}
                    case Ok(T token, I rest) -> {results.add(token);source = rest;}
                }
            }
            return new Ok<I, List<T>, R>(results, source);
        };

        return parser;
    }

    // Reconoce cero o mas repeticiones
    public static <I, T, R> Parser<I, List<T>, R> Star(Parser<I, T, R> parser) {

        Parser<I, List<T>, R> pstar = (I source) -> {
            var tokens = new ArrayList<T>();
            trying:
            while (true) {
                switch (parser.parse(source)) {
                    case Ok(T token, I rest) -> {tokens.add(token);source = rest;}
                    case Fail(R reason) -> {break trying;}
                }
            }
            return new Ok<I, List<T>, R>(tokens, source);
        };

        return pstar;
    }

    // Transforma una lista de tokens en un solo resultado
    public static <I, T, R> Parser<I, T, R> TFlat(Parser<I, List<T>, R> p, Function<List<T>, T> cat) {

        return (I source) -> switch (p.parse(source)) {
            case Fail(R reason) -> new Fail<I, T, R>(reason);
            case Ok(List<T> tokens, I rest) -> new Ok<I, T, R>(cat.apply(tokens), rest);
        };
    }

    // Transforma una lista de errores en un solo error
    public static <I, T, R> Parser<I, T, R> RFlat(Parser<I, T, List<R>> p, Function<List<R>, R> cat) {
        return (I source) -> switch (p.parse(source)) {
            case Fail(List<R> reasons) -> new Fail<I, T, R>(cat.apply(reasons));
            case Ok(T token, I rest) -> new Ok<I, T, R>(token, rest);
        };
    }

}
