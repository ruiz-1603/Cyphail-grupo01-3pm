package org.cyphail.ast;

import java.util.List;

public record DeleteClause(List<String> variables) implements UpdateClause { }