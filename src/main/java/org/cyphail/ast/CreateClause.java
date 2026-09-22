package org.cyphail.ast;

import java.util.List;

public record CreateClause(List<PatternNode> patterns) implements UpdateClause { }