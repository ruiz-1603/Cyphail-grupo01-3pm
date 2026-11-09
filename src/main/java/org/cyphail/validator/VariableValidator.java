package org.cyphail.validator;

/*
 * Proyecto Cyphail
 * Grupo 01-3pm
 *
 * Autores:
 * - Priscilla Murillo Romero
 * - Aaron Ruiz Medina
 * - Samael Sanchez Mora
 * - Daniel Villarroel Abaduca
 * - Nicolás Zárate Hernández
 */

import org.cyphail.ast.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.cyphail.ast.ExprText.text;

/**
 * Proyecto Cyphail - Grupo 01-3pm
 *
 * Semantica estatica (se corre despues del parser y antes de .tree o del CodeGen).
 *
 * Reglas de P1:
 * - Dentro de un MISMO patron, las variables se declaran en el orden en que
 *   aparecen. Si una propiedad referencia una variable que todavia no ha
 *   aparecido ANTES, es un error (CASO 11: "p.id antes de p").
 * - Una vez terminado el MATCH, todas sus variables quedan disponibles
 *   para WHERE, CREATE, SET, REMOVE, DELETE y RETURN.
 * - Las variables introducidas por CREATE tambien quedan disponibles
 *   para clausulas posteriores.
 *
 * Reglas nuevas de P2:
 * - Una variable de relacion no se repite en el query: (a)-[r]->(b), (c)-[r]->(b) es error.
 *   Una variable de nodo si se puede repetir (variable de join).
 * - Una variable no puede ser de nodo y de relacion a la vez: (a:N), (b)-[a]->(c) es error.
 * - Ninguna variable (ni alias) puede empezar con "__": ese prefijo es del codigo Prolog generado.
 * - En CREATE cada relacion necesita direccion (-> o <-) y exactamente un tipo.
 * - SET/REMOVE de etiquetas solo aplica a nodos.
 * - Dos columnas del RETURN no pueden tener el mismo nombre.
 */
public final class VariableValidator {

    private VariableValidator() {}

    static final String RESERVED_PREFIX = "__";

    /** Devuelve el mensaje del primer error, o vacio si el query es valido. */
    public static Optional<String> validate(Query query) {
        try {
            analyze(query);
            return Optional.empty();
        } catch (ValidationException e) {
            return Optional.of(e.getMessage());
        }
    }

    /**
     * Valida el query y devuelve su tabla de simbolos.
     * @throws ValidationException con el primer error encontrado
     */
    public static VariableTable analyze(Query query) {
        if (query.where().isPresent() && query.match().isEmpty()) {
            throw new ValidationException("WHERE requires a MATCH clause");
        }

        // 0. Una variable de nodo reutilizada (join) no puede cambiar de etiquetas
        checkSameLabels(query);

        // 1. MATCH: declara nodos y relaciones en orden
        var table = query.match()
                .map(match -> declarePatterns(VariableTable.empty(), match.patterns(), "MATCH"))
                .orElse(VariableTable.empty());

        // 2. WHERE, ya con todas las variables del MATCH
        for (WhereClause where : query.where().stream().toList()) {
            checkExpr(where.condition(), table, "WHERE");
        }

        // 3. CREATE, SET, REMOVE, DELETE en orden; CREATE puede declarar variables nuevas
        for (UpdateClause update : query.updates()) {
            table = update(table, update);
        }

        // 4. RETURN
        var finalTable = table;
        return query.returnClause()
                .map(ret -> projection(finalTable, ret.projection()))
                .orElse(finalTable);
    }

    // ---------------------------------------------------------------- patrones

    /*
     * Casos P2 (Validaciones, "etiqueta no repetida"): si una variable de nodo se repite,
     * todas las apariciones que traen etiquetas deben traer las mismas.
     * (e:Empresa) ... (e) es valido; (e:Empresa) ... (e:Ciudad) es error.
     */
    private static void checkSameLabels(Query query) {
        var patterns = Stream.concat(
                query.match().stream().flatMap(match -> match.patterns().stream()),
                query.updates().stream()
                        .flatMap(update -> update instanceof CreateClause(var created)
                                ? created.stream() : Stream.empty()));

        var labelsByVariable = patterns
                .flatMap(VariableValidator::nodesOf)
                .filter(node -> node.variable().isPresent() && !node.labels().isEmpty())
                .collect(Collectors.groupingBy(node -> node.variable().get(), LinkedHashMap::new,
                        Collectors.mapping(node -> Set.copyOf(node.labels()), Collectors.toCollection(LinkedHashSet::new))));

        labelsByVariable.forEach((variable, labelSets) -> {
            if (labelSets.size() > 1) {
                throw new ValidationException(String.format(
                        "Node variable '%s' cannot have different labels in the same query: %s",
                        variable, labelSets.stream().map(Set::toString).collect(Collectors.joining(" and "))));
            }
        });
    }

    private static Stream<PatternNode> nodesOf(PatternElement element) {
        return switch (element) {
            case PatternNode node -> Stream.of(node);
            case PathPattern(PatternNode left, RelationshipPattern rel, PatternNode right) -> Stream.of(left, right);
        };
    }

    private static VariableTable declarePatterns(VariableTable table, List<? extends PatternElement> patterns,
                                                 String clause) {
        var current = table;
        for (PatternElement element : patterns) {
            current = switch (element) {
                case PatternNode node -> declareNode(current, node, clause);
                case PathPattern(PatternNode left, RelationshipPattern rel, PatternNode right) -> {
                    var withLeft = declareNode(current, left, clause);
                    var withRel = declareRelationship(withLeft, rel, clause);
                    yield declareNode(withRel, right, clause);
                }
            };
        }
        return current;
    }

    // Las propiedades se revisan ANTES de declarar la variable del nodo (regla del CASO 11)
    private static VariableTable declareNode(VariableTable table, PatternNode node, String clause) {
        checkProperties(node.properties(), table, clause);
        return node.variable()
                .map(name -> {
                    checkName(name);
                    if (table.kindOf(name).orElse(VarKind.NODE) == VarKind.RELATIONSHIP) {
                        throw new ValidationException(String.format(
                                "Variable '%s' is a relationship and cannot be used as a node", name));
                    }
                    return table.withVariable(name, VarKind.NODE);
                })
                .orElse(table);
    }

    private static VariableTable declareRelationship(VariableTable table, RelationshipPattern rel, String clause) {
        if (clause.equals("CREATE")) {
            checkCreatable(rel);
        }
        checkProperties(rel.properties(), table, clause);
        return rel.variable()
                .map(name -> {
                    checkName(name);
                    table.kindOf(name).ifPresent(kind -> {
                        throw new ValidationException(kind == VarKind.NODE
                                ? String.format("Variable '%s' is a node and cannot be used as a relationship", name)
                                : String.format("Relationship variable '%s' cannot be repeated in the same query", name));
                    });
                    return table.withVariable(name, VarKind.RELATIONSHIP);
                })
                .orElse(table);
    }

    private static void checkCreatable(RelationshipPattern rel) {
        var shown = rel.variable().orElse("relationship");
        if (rel.direction() == Direction.NONE) {
            throw new ValidationException(String.format(
                    "CREATE needs a direction (-> or <-) for '%s'", shown));
        }
        if (rel.types().size() != 1) {
            throw new ValidationException(String.format(
                    "CREATE needs exactly one type for '%s'", shown));
        }
    }

    private static void checkProperties(List<PropertyEntry> properties, VariableTable table, String clause) {
        for (PropertyEntry property : properties) {
            checkExpr(property.value(), table, clause);
        }
    }

    // ---------------------------------------------------------------- updates

    private static VariableTable update(VariableTable table, UpdateClause update) {
        return switch (update) {
            case CreateClause(var patterns) -> declarePatterns(table, patterns, "CREATE");
            case SetClause(List<SetItem> items) -> {
                for (SetItem item : items) {
                    switch (item) {
                        case SetProperty(PropertyAccess target, Expr value) -> {
                            checkExpr(target, table, "SET");
                            checkExpr(value, table, "SET");
                        }
                        case SetLabels(String variable, var labels) -> requireNode(variable, table, "SET");
                    }
                }
                yield table;
            }
            case RemoveClause(List<RemoveItem> items) -> {
                for (RemoveItem item : items) {
                    switch (item) {
                        case RemoveProperty(PropertyAccess target) -> checkExpr(target, table, "REMOVE");
                        case RemoveLabels(String variable, var labels) -> requireNode(variable, table, "REMOVE");
                    }
                }
                yield table;
            }
            case DeleteClause(boolean detach, List<String> variables) -> {
                for (String variable : variables) {
                    requireDeclared(variable, table, "DELETE");
                }
                yield table;
            }
        };
    }

    private static void requireNode(String variable, VariableTable table, String clause) {
        requireDeclared(variable, table, clause);
        if (table.kindOf(variable).orElseThrow() != VarKind.NODE) {
            throw new ValidationException(String.format(
                    "Labels can only be changed on nodes, but '%s' is a relationship (clause %s)", variable, clause));
        }
    }

    // ---------------------------------------------------------------- RETURN

    private static VariableTable projection(VariableTable table, Projection projection) {
        var columns = new ArrayList<String>();
        var seen = new HashSet<String>();
        for (ProjectionItem item : projection.items()) {
            checkExpr(item.expr(), table, "RETURN");
            item.alias().ifPresent(VariableValidator::checkName);
            var column = item.alias().orElseGet(() -> text(item.expr()));
            if (!seen.add(column)) {
                throw new ValidationException(String.format(
                        "Column '%s' is defined more than once in RETURN", column));
            }
            columns.add(column);
        }
        return table.withColumns(columns);
    }

    // ---------------------------------------------------------------- expresiones y nombres

    private static void checkExpr(Expr expr, VariableTable table, String clause) {
        switch (expr) {
            case Binary(BinaryOp op, Expr left, Expr right) -> {
                checkExpr(left, table, clause);
                checkExpr(right, table, clause);
            }
            case And(Expr left, Expr right) -> {
                checkExpr(left, table, clause);
                checkExpr(right, table, clause);
            }
            case Or(Expr left, Expr right) -> {
                checkExpr(left, table, clause);
                checkExpr(right, table, clause);
            }
            case Not(Expr operand) -> checkExpr(operand, table, clause);
            case PropertyAccess(String variable, String property) -> requireDeclared(variable, table, clause);
            case Var(String name) -> requireDeclared(name, table, clause);
            case NumberLit n -> { }
            case DecimalLit d -> { }
            case StringLit s -> { }
            case BoolLit b -> { }
        }
    }

    private static void requireDeclared(String variable, VariableTable table, String clause) {
        if (!table.isDeclared(variable)) {
            throw new ValidationException(String.format(
                    "Variable '%s' is not defined in clause %s", variable, clause));
        }
    }

    private static void checkName(String name) {
        if (name.startsWith(RESERVED_PREFIX)) {
            throw new ValidationException(String.format(
                    "Variable '%s' cannot start with '%s' (reserved for generated Prolog variables)",
                    name, RESERVED_PREFIX));
        }
    }
}