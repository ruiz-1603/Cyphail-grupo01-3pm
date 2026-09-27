package org.cyphail.ast;
import java.util.List;

public record Projection(List<ProjectionItem> items, List<Modifier> modifiers) {

}