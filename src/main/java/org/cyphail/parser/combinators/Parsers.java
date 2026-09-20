package org.cyphail.parser.combinators;

import org.cyphail.parser.core.Fail;
import org.cyphail.parser.core.Ok;
import org.cyphail.parser.core.Parser;
import org.cyphail.parser.core.Result;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;

public final class Parsers {

    private Parsers() {}

    @SafeVarargs
    public static <I, T, R> Parser<I, List<T>, R> Sequence(Parser<I, T, R>... parsers) {
        return input -> {
            List<T> results = new ArrayList<>();
            I source = input;

            for (var p : parsers) {
                switch (p.parse(source)) {
                    case Fail<I, T, R>(R reason) -> {
                        return Result.fail(reason);
                    }
                    case Ok<I, T, R>(T token, I rest) -> {
                        results.add(token);
                        source = rest;
                    }
                }
            }

            return Result.ok(results, source);
        };
    }

    public static <I, T, R> Parser<I, T, R> Or(Parser<I, T, R> first, Parser<I, T, R> second) {
        return input -> switch (first.parse(input)) {
            case Ok<I, T, R> ok -> ok;
            case Fail<I, T, R> _ -> second.parse(input);
        };
    }

    public static <I, T, R> Parser<I, Optional<T>, R> Opt(Parser<I, T, R> parser) {
        return input -> switch (parser.parse(input)) {
            case Ok<I, T, R>(T token, I rest) -> Result.ok(Optional.of(token), rest);
            case Fail<I, T, R> _ -> Result.ok(Optional.empty(), input);
        };
    }

    public static <I, T, R> Parser<I, List<T>, R> Star(Parser<I, T, R> parser) {
        return input -> {
            List<T> results = new ArrayList<>();
            I source = input;

            while (true) {
                switch (parser.parse(source)) {
                    case Fail<I, T, R> _ -> {
                        return Result.ok(results, source);
                    }
                    case Ok<I, T, R>(T token, I rest) -> {
                        results.add(token);
                        source = rest;
                    }
                }
            }
        };
    }

    public static <I, T, R> Parser<I, List<T>, R> Plus(Parser<I, T, R> parser) {
        Parser<I, List<T>, R> star = Star(parser);

        return input -> switch (parser.parse(input)) {
            case Fail<I, T, R>(R reason) -> Result.fail(reason);
            case Ok<I, T, R>(T first, I rest) -> switch (star.parse(rest)) {
                case Fail<I, List<T>, R>(R reason) -> Result.fail(reason);
                case Ok<I, List<T>, R>(List<T> others, I end) -> {
                    List<T> all = new ArrayList<>();
                    all.add(first);
                    all.addAll(others);
                    yield Result.ok(all, end);
                }
            };
        };
    }



    public static <I, A, B, R> Parser<I, B, R> Map(Parser<I, A, R> parser, Function<A, B> mapper) {
        return input -> switch (parser.parse(input)) {
            case Fail<I, A, R>(R reason) -> Result.fail(reason);
            case Ok<I, A, R>(A token, I rest) -> Result.ok(mapper.apply(token), rest);
        };
    }
}