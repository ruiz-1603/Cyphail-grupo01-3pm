package org.cyphail.ast;
import java.util.Optional;

public record ProjectionItem(Expr expr, Optional<String> alias) {

}