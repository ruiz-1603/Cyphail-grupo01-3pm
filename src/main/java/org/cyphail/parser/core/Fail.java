package org.cyphail.parser.core;

public record Fail<I, T, R>(R reason) implements Result<I, T, R> {}
