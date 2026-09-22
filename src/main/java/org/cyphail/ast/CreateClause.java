package org.cyphail.ast;
import java.util.List;

record CreateClause(List<PatternNode> patterns) implements UpdateClause {

}