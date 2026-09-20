package org.cyphail.parser.core;

public interface IToken<T> {
    TToken type();
    T value();
}