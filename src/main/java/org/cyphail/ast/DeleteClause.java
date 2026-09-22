package org.cyphail.ast;

import java.util.List;

record DeleteClause(List<String> variables) implements UpdateClause {

}