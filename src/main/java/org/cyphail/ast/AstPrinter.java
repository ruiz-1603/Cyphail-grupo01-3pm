package org.cyphail.ast;

import java.util.List;
import java.util.Optional;

public final class AstPrinter {

    private AstPrinter() {}

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

    private static void printMatch(StringBuilder sb, MatchClause match) {
        sb.append("  Match: {\n");
        sb.append("    Patterns: [\n");

        for (PatternNode p : match.patterns()) {
            printPattern(sb, p, "      ");
        }

        sb.append("    ]\n");
        sb.append("  }\n");
    }

    private static void printPattern(StringBuilder sb, PatternNode p, String indent) {
        sb.append(indent).append("PatternNode: {\n");
        sb.append(indent).append("  var: ").append(p.variable()).append("\n");
        sb.append(indent).append("  labels: [ ").append(String.join(", ", p.labels())).append(" ]\n");
        sb.append(indent).append("  properties: [").append(printProperties(p.properties())).append("]\n");
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

                        for (PatternNode pattern : patterns) {
                            printPattern(sb, pattern, "        ");
                        }

                        sb.append("      ]\n");
                        sb.append("    }\n");
                    }

                    case DeleteClause(var variables) ->
                            sb.append("    Delete: [ ").append(String.join(", ", variables)).append(" ]\n");
                }
            }

            sb.append("  ");
        }

        sb.append("]\n");
    }

    private static String printUpdate(UpdateClause u) {
        return switch (u) {
            case CreateClause(List<PatternNode> patterns) -> "Create: " + patterns.size() + " pattern(s)";
            case DeleteClause(List<String> vars) -> "Delete: " + String.join(", ", vars);
        };
    }

    private static void printReturn(StringBuilder sb, ReturnClause returnClause) {
        sb.append("  Return: {\n");
        sb.append("    Projection: {\n");
        sb.append("      Items: [\n");
        for (ProjectionItem item : returnClause.projection().items()) {
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
            case PropertyAccess(String v, String p) -> "(. " + v + " " + p + ")";
            case Var(String n) -> n;
            case NumberLit(long v) -> String.valueOf(v);
            case StringLit(String v) -> "\"" + v + "\"";
        };
    }
}