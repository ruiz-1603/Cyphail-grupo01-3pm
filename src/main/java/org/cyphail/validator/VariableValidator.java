package org.cyphail.validator;

import org.cyphail.ast.*;

import java.util.*;

/**
 * Proyecto Cyphail - Grupo 01-3pm
 *
 * Reglas:
 * - Dentro de un MISMO patrón MATCH, las variables se declaran en el orden
 *   en que aparecen los nodos separados por coma. Si una propiedad de un
 *   nodo referencia una variable que todavía no ha aparecido en un nodo
 *   ANTERIOR del mismo MATCH, es un error (CASO 11: "p.id antes de p").
 * - Una vez terminado el MATCH, todas sus variables quedan disponibles
 *   para WHERE, CREATE y RETURN sin importar el orden entre ellas.
 * - Las variables introducidas por CREATE también quedan disponibles
 *   para clausulas posteriores (DELETE, RETURN).
 */
public final class VariableValidator {

    private VariableValidator() {}

    public static Optional<String> validate(Query query) {
        // 1. Validar el propio MATCH: las propiedades de cada nodo solo pueden
        //    referenciar variables de nodos declarados ANTES en el mismo MATCH.
        Set<String> declaredVars = new LinkedHashSet<>();
        for (PatternNode pattern : query.match().patterns()) {
            for (PropertyEntry prop : pattern.properties()) {
                var error = validateExpr(prop.value(), declaredVars, "MATCH");
                if (error.isPresent()) return error;
            }
            declaredVars.add(pattern.variable());
        }

        // 2. Validar WHERE (ya con todas las variables del MATCH disponibles)
        if (query.where().isPresent()) {
            var error = validateExpr(query.where().get().condition(), declaredVars, "WHERE");
            if (error.isPresent()) return error;
        }

        // 3. Validar UPDATE clauses (CREATE, DELETE), ampliando el scope
        //    con las variables que cada CREATE va introduciendo.
        for (UpdateClause update : query.updates()) {
            var error = validateUpdateClause(update, declaredVars);
            if (error.isPresent()) return error;
        }

        // 4. Validar RETURN
        for (ProjectionItem item : query.returnClause().projection().items()) {
            var error = validateExpr(item.expr(), declaredVars, "RETURN");
            if (error.isPresent()) return error;
        }

        return Optional.empty();
    }

    private static Optional<String> validateExpr(Expr expr, Set<String> declared, String clause) {
        return switch (expr) {
            case Binary(BinaryOp op, Expr left, Expr right) -> {
                var leftError = validateExpr(left, declared, clause);
                if (leftError.isPresent()) yield leftError;
                yield validateExpr(right, declared, clause);
            }
            case PropertyAccess(String varName, String propName) -> {
                if (!declared.contains(varName)) {
                    yield Optional.of(String.format(
                        "Variable '%s' is not defined in clause %s", varName, clause));
                }
                yield Optional.empty();
            }
            case Var(String name) -> {
                if (!declared.contains(name)) {
                    yield Optional.of(String.format(
                        "Variable '%s' is not defined in clause %s", name, clause));
                }
                yield Optional.empty();
            }
            case NumberLit(long v) -> Optional.empty();
            case StringLit(String s) -> Optional.empty();
        };
    }

    private static Optional<String> validateUpdateClause(UpdateClause update, Set<String> declared) {
        return switch (update) {
            case CreateClause(List<PatternNode> patterns) -> {
                for (PatternNode pattern : patterns) {
                    for (PropertyEntry prop : pattern.properties()) {
                        var error = validateExpr(prop.value(), declared, "CREATE");
                        if (error.isPresent()) yield error;
                    }
                    declared.add(pattern.variable());
                }
                yield Optional.empty();
            }
            case DeleteClause(List<String> vars) -> {
                for (String var : vars) {
                    if (!declared.contains(var)) {
                        yield Optional.of(String.format(
                            "Variable '%s' is not defined in clause DELETE", var));
                    }
                }
                yield Optional.empty();
            }
        };
    }
}