package org.cyphail.ast;

import java.util.List;
import java.util.Optional;

/**
 * Vista .tree del AST.
 * La salida de los queries P1 (MATCH + RETURN con nodos) no cambia: los tests de P1 la comparan.
 * Una variable anonima se imprime como "_".
 */
public final class AstPrinter {

    private AstPrinter() {}

    static final String ANONYMOUS = "_";

    public static String print(Query query) {
        StringBuilder sb = new StringBuilder();
        sb.append("Query{\n");
        printMatch(sb, query.match());
        printWhere(sb, query.where());
        printUpdates(sb, query.updates());
        printReturn(sb, query.returnClause());
        sb.append("}");
        return sb.toString();
    }

    private static void printMatch(StringBuilder sb, Optional<MatchClause> match) {
        if (match.isEmpty()) {
            sb.append("  Match: {}\n");
            return;
        }
        sb.append("  Match: {\n");
        sb.append("    Patterns: [\n");

        for (PatternElement p : match.get().patterns()) {
            printElement(sb, p, "      ");
        }

        sb.append("    ]\n");
        sb.append("  }\n");
    }

    private static void printElement(StringBuilder sb, PatternElement element, String indent) {
        switch (element) {
            case PatternNode node -> printPattern(sb, node, indent);
            case PathPattern(PatternNode left, RelationshipPattern rel, PatternNode right) -> {
                sb.append(indent).append("Path: {\n");
                printPattern(sb, left, indent + "  ");
                printRelationship(sb, rel, indent + "  ");
                printPattern(sb, right, indent + "  ");
                sb.append(indent).append("}\n");
            }
        }
    }

    private static void printPattern(StringBuilder sb, PatternNode p, String indent) {
        sb.append(indent).append("PatternNode: {\n");
        sb.append(indent).append("  var: ").append(p.variable().orElse(ANONYMOUS)).append("\n");
        sb.append(indent).append("  labels: [ ").append(String.join(", ", p.labels())).append(" ]\n");
        sb.append(indent).append("  properties: [").append(printProperties(p.properties())).append("]\n");
        sb.append(indent).append("}\n");
    }

    private static void printRelationship(StringBuilder sb, RelationshipPattern r, String indent) {
        sb.append(indent).append("Relationship: {\n");
        sb.append(indent).append("  var: ").append(r.variable().orElse(ANONYMOUS)).append("\n");
        sb.append(indent).append("  direction: ").append(r.direction()).append("\n");
        sb.append(indent).append("  types: [ ").append(String.join(" | ", r.types())).append(" ]\n");
        sb.append(indent).append("  properties: [").append(printProperties(r.properties())).append("]\n");
        sb.append(indent).append("}\n");
    }

    private static String printProperties(List<PropertyEntry> properties) {
        if (properties.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < properties.size(); i++) {
            if (i > 0) sb.append(", ");
            PropertyEntry e = properties.get(i);
            sb.append(e.key()).append(": ").append(expr(e.value()));
        }
        return sb.toString();
    }

    private static void printWhere(StringBuilder sb, Optional<WhereClause> where) {
        sb.append("  Where: {");
        where.ifPresent(w -> sb.append("\n    Expr: ").append(expr(w.condition())).append("\n  "));
        sb.append("}\n");
    }

    private static void printUpdates(StringBuilder sb, List<UpdateClause> updates) {
        sb.append("  Updates: [");
        if (!updates.isEmpty()) {sb.append("\n");
            for (UpdateClause update : updates) {
                switch (update) {
                    case CreateClause(var patterns) -> {
                        sb.append("    Create: {\n");
                        sb.append("      Patterns: [\n");

                        for (PatternElement pattern : patterns) {
                            printElement(sb, pattern, "        ");
                        }

                        sb.append("      ]\n");
                        sb.append("    }\n");
                    }

                    case SetClause(List<SetItem> items) ->
                            printItems(sb, "Set", items.stream().map(AstPrinter::setItem).toList());

                    case RemoveClause(List<RemoveItem> items) ->
                            printItems(sb, "Remove", items.stream().map(AstPrinter::removeItem).toList());

                    case DeleteClause(boolean detach, List<String> variables) ->
                            sb.append(detach ? "    DetachDelete: [ " : "    Delete: [ ")
                                    .append(String.join(", ", variables)).append(" ]\n");
                }
            }

            sb.append("  ");
        }

        sb.append("]\n");
    }

    // Set: [ ... ] / Remove: [ ... ] con un elemento por linea
    private static void printItems(StringBuilder sb, String name, List<String> items) {
        sb.append("    ").append(name).append(": [\n");
        for (String item : items) {
            sb.append("      ").append(item).append("\n");
        }
        sb.append("    ]\n");
    }

    private static String setItem(SetItem item) {
        return switch (item) {
            case SetProperty(PropertyAccess target, Expr value) -> "(= " + expr(target) + " " + expr(value) + ")";
            case SetLabels(String variable, List<String> labels) -> labels(variable, labels);
        };
    }

    private static String removeItem(RemoveItem item) {
        return switch (item) {
            case RemoveProperty(PropertyAccess target) -> expr(target);
            case RemoveLabels(String variable, List<String> labels) -> labels(variable, labels);
        };
    }

    private static String labels(String variable, List<String> labels) {
        return "(labels " + variable + " " + String.join(" ", labels) + ")";
    }

    private static void printReturn(StringBuilder sb, Optional<ReturnClause> returnClause) {
        if (returnClause.isEmpty()) {
            sb.append("  Return: {}\n");
            return;
        }
        sb.append("  Return: {\n");
        sb.append("    Projection: {\n");
        sb.append("      Items: [\n");
        for (ProjectionItem item : returnClause.get().projection().items()) {
            sb.append("        ").append(printItem(item)).append("\n");
        }
        sb.append("      ]\n");
        sb.append("      Modifiers: []\n");
        sb.append("    }\n");
        sb.append("  }\n");
    }

    private static String printItem(ProjectionItem item) {
        return item.alias()
                .map(alias -> "{as " + expr(item.expr()) + " " + alias + "}")
                .orElseGet(() -> "{" + expr(item.expr()) + "}");
    }

    private static String expr(Expr e) {
        return switch (e) {
            case Binary(BinaryOp op, Expr l, Expr r) ->
                    "(" + op.symbol() + " " + expr(l) + " " + expr(r) + ")";
            case And(Expr l, Expr r) -> "(AND " + expr(l) + " " + expr(r) + ")";
            case Or(Expr l, Expr r) -> "(OR " + expr(l) + " " + expr(r) + ")";
            case Not(Expr operand) -> "(NOT " + expr(operand) + ")";
            case PropertyAccess(String v, String p) -> "(. " + v + " " + p + ")";
            case Var(String n) -> n;
            case NumberLit(long v) -> String.valueOf(v);
            case DecimalLit(double v) -> String.valueOf(v);
            case StringLit(String v) -> "\"" + v + "\"";
        };
    }
}