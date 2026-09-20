package org.cyphail.parser.core;

public record TokenString(TToken type, String value) implements IToken<String> {

}