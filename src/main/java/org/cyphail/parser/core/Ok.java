package org.cyphail.parser.core;

public record Ok <I, T, R>(T token, I rest)implements Result<I, T, R> {}
