package org.cyphail.parser.combinators;

import org.cyphail.parser.core.Fail;
import org.cyphail.parser.core.Ok;
import org.cyphail.parser.core.Parser;
import org.cyphail.parser.core.Result;
import java.util.function.Function;

public final class Parsers {

    private Parsers() {
		
    }

    public static <I, A, B, R> Parser<I, B, R> sequence(Parser<I, A, R> first, Parser<I, B, R> second){ return input -> {

            Result<I, A, R> firstResult = first.parse(input);

            if (firstResult instanceof Fail<I, A, R> fail) {
                return Result.fail(fail.reason());
            }

            Ok<I, A, R> firstOk = (Ok<I, A, R>) firstResult;

            Result<I, B, R> secondResult = second.parse(firstOk.rest());

            if (secondResult instanceof Fail<I, B, R> fail) {
                return Result.fail(fail.reason());
            }

            Ok<I, B, R> secondOk = (Ok<I, B, R>) secondResult;

            return Result.ok(secondOk.token(), secondOk.rest());
		};
    }
	
	
	public static <I, T, R> Parser<I, T, R> choice( Parser<I, T, R> first, Parser<I, T, R> second) { return input -> {
        
		Result<I, T, R> firstResult = first.parse(input);

        if (firstResult instanceof Ok<I, T, R>) {
            return firstResult;
        }

        return second.parse(input);
		};
	}
	
	public static <I, A, B, R> Parser <I, B, R> map(Parser<I, A, R> parser, Function<A, B> mapper) { return input -> {

        Result<I, A, R> result = parser.parse(input);

        if (result instanceof Fail<I, A, R> fail) {
            return Result.fail(fail.reason());
        }

        Ok<I, A, R> ok = (Ok<I, A, R>) result;

        return Result.ok(
                mapper.apply(ok.token()),
                ok.rest()
			);
		};
	}
		
	public static Parser<String, String, String> literal(String expected) { return input -> {

        if (input.startsWith(expected)) {
            return Result.ok(expected, input.substring(expected.length()));
        }

        return Result.fail( "Expected '" + expected + "'");
		
		};
	}
	
	
	
		
}