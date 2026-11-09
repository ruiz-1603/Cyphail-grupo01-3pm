package org.cyphail.codegen;

import org.cyphail.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.cyphail.ast.ExprText.text;
import static org.cyphail.codegen.PrologSyntax.atom;
import static org.cyphail.codegen.PrologSyntax.call;
import static org.cyphail.codegen.PrologSyntax.graphPredicate;

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

/**
 * CodeGen: transpila un AST Cyphail a goals de Prolog para prolog/cy_engine.pl.
 *
 * Es un visitor del AST escrito como en AstPrinter: un metodo por tipo de nodo
 * y switch con pattern matching sobre las interfaces sealed (Expr, UpdateClause, SetItem, RemoveItem).
 * Orden de los goals: MATCH (nodos y relaciones), WHERE (AND/OR/NOT), actualizaciones
 * (CREATE, SET, REMOVE, DELETE, en el orden del query) y al final RETURN. Como Prolog
 * recorre todas las soluciones, cada actualizacion se aplica una vez por fila del MATCH.
 * El estado (tabla de simbolos + goals emitidos) es inmutable y pasa de un
 * metodo al siguiente: cada metodo recibe un State y devuelve uno nuevo.
 *
 * Ejemplo (grafo amigos):
 *   MATCH (a:Person) WHERE a.age > 18 RETURN a.name
 * produce el cuerpo
 *   user_amigos_node(__A), user_amigos_label(__A, 'Person'),
 *   user_amigos_prop(__A, age, __A_age), cy_gt(__A_age, 18),
 *   user_amigos_prop(__A, name, __A_name)
 * con cabeza cy_row(__A_name) y columna 'a.name'.
 */
public final class CodeGen {

    private CodeGen() {}

    // Functor de la cabeza; el engine solo usa sus argumentos (ver cy_format_row)
    static final String ROW_FUNCTOR = "cy_row";

    /*
     * Records auxiliares del CodeGen.
     * No forman parte del AST.
     */
    private record State(String graph, SymbolTable symbols, List<String> goals) {

        State emit(String goal) {
            return emitAll(List.of(goal));
        }

        State emitAll(List<String> newGoals) {
            var all = new ArrayList<>(goals);
            all.addAll(newGoals);
            return new State(graph, symbols, List.copyOf(all));
        }

        State withSymbols(SymbolTable newSymbols) {
            return new State(graph, newSymbols, goals);
        }

        String id(String variable) {
            return symbols.variable(variable).orElseThrow(() -> CodeGenException.undefinedVariable(variable));
        }

        // Llamada a un predicado de datos del grafo: user_G_suffix(args...)
        String graphCall(String suffix, String... args) {
            return call(graphPredicate(graph, suffix), args);
        }

        // Llamada a una actualizacion del engine, con el grafo como primer argumento: cy_set_prop(G, ...)
        String updateCall(String predicate, String... args) {
            var all = new ArrayList<String>();
            all.add(atom(graph));
            all.addAll(List.of(args));
            return call(predicate, all.toArray(String[]::new));
        }

        // Despues de cambiar variable.property el calculo anterior ya no sirve
        State forget(String variable, String property) {
            return withSymbols(symbols.withoutProperty(variable, property));
        }
    }

    // Termino Prolog que representa un valor, junto con el estado que dejo al calcularlo
    private record Term(State state, String text) {}

    // Lista de terminos calculados en orden (columnas del RETURN)
    private record Terms(State state, List<String> texts) {}

    // Nodo o relacion ya declarado: el estado y el termino Prolog que lo identifica (__A, __E1, ...)
    private record Element(State state, String id) {}


    /** Transpila el query para el grafo activo (el de .use). */
    public static GeneratedQuery generate(Query query, String graph) {
        var hasUpdates = !query.updates().isEmpty();
        // Sin MATCH solo tiene sentido un query que crea (CREATE ... RETURN ...)
        if (query.match().isEmpty() && !hasUpdates) {
            throw new CodeGenException("A query without MATCH is not supported yet");
        }
        // Sin RETURN solo tiene sentido un query que cambia el grafo
        if (query.returnClause().isEmpty() && !hasUpdates) {
            throw new CodeGenException("A query without RETURN is not supported yet");
        }
        var items = query.returnClause().map(r -> r.projection().items()).orElse(List.of());

        var start = new State(graph, SymbolTable.empty(), List.of());
        var matched = query.match().map(m -> match(start, m)).orElse(start);
        var filtered = where(matched, query.where());
        var updated = updates(filtered, query.updates());
        var head = projection(updated, items);

        return new GeneratedQuery(
                head(head.texts()),
                head.state().goals(),
                columns(items),
                head.state().symbols(),
                hasUpdates);
    }

    // cy_row(V1, ..., Vn); sin RETURN no hay columnas y la cabeza es solo el atomo cy_row
    private static String head(List<String> values) {
        return values.isEmpty() ? ROW_FUNCTOR : call(ROW_FUNCTOR, values.toArray(String[]::new));
    }


    // match := "MATCH" pattern
    // Al final se exige que las relaciones del MATCH sean distintas entre si ("Arco solo una vez", SPEC)
    private static State match(State state, MatchClause match) {
        var current = state;
        var edges = new ArrayList<String>();
        for (PatternElement element : match.patterns()) {
            var matched = switch (element) {
                case PatternNode node -> matchNode(current, node);
                case PathPattern path -> matchPath(current, path);
            };
            current = matched.state();
            if (element instanceof PathPattern) edges.add(matched.id());
        }
        return distinctEdges(current, edges);
    }

    // Variable del query -> su variable Prolog (la misma si ya aparecio: variable de join).
    // Sin variable (nodo o relacion anonimos) -> nombre fresco __N1, __E1, ...
    private static Element declare(State state, Optional<String> variable, String anonymousPrefix) {
        return variable
                .map(name -> {
                    var declared = state.withSymbols(state.symbols().withVariable(name));
                    return new Element(declared, declared.id(name));
                })
                .orElseGet(() -> {
                    var fresh = state.symbols().nextFresh(anonymousPrefix);
                    return new Element(state.withSymbols(state.symbols().withUsed(fresh)), fresh);
                });
    }

    // (a:L1:L2 {k: v}) solo -> nodo (si a es nueva), una etiqueta por label y un filtro por propiedad.
    // Si 'a' ya aparecio antes (variable de join) no se vuelve a pedir user_G_node.
    private static Element matchNode(State state, PatternNode node) {
        var isNew = node.variable().flatMap(name -> state.symbols().variable(name)).isEmpty();
        var declared = declare(state, node.variable(), "N");
        var withNode = isNew
                ? declared.state().emit(declared.state().graphCall("node", declared.id()))
                : declared.state();
        return new Element(nodeConstraints(withNode, node, declared.id()), declared.id());
    }

    // Etiquetas y propiedades inline de un nodo ya declarado
    private static State nodeConstraints(State state, PatternNode node, String id) {
        var withLabels = state.emitAll(node.labels().stream()
                .map(label -> state.graphCall("label", id, atom(label)))
                .toList());
        return properties(withLabels, node.variable(), id, node.properties());
    }

    // (a)-[r:T {k: v}]->(b): como en el SPEC, primero el arco (que liga a los dos nodos),
    // luego las etiquetas/propiedades de cada nodo y al final las propiedades de la relacion.
    // Los nodos de un arco no necesitan user_G_node: el arco ya los liga.
    private static Element matchPath(State state, PathPattern path) {
        var left = declare(state, path.left().variable(), "N");
        var right = declare(left.state(), path.right().variable(), "N");
        var relationship = declare(right.state(), path.relationship().variable(), "E");

        var current = relationship.state();
        current = current.emit(edge(current, path.relationship(), relationship.id(), left.id(), right.id()));
        current = nodeConstraints(current, path.left(), left.id());
        current = nodeConstraints(current, path.right(), right.id());
        current = properties(current, path.relationship().variable(), relationship.id(), path.relationship().properties());
        return new Element(current, relationship.id());
    }

    // user_G_edge(E, Tipo, Desde, Hacia); sin tipo -> '_'
    //   OUT  (a)-[e]->(b): user_G_edge(E, T, A, B)
    //   IN   (a)<-[e]-(b): user_G_edge(E, T, B, A)
    //   NONE (a)-[e]-(b) : en cualquier sentido, (uno ; otro)
    private static String edge(State state, RelationshipPattern relationship, String id, String left, String right) {
        var type = relationship.types().isEmpty() ? "_" : atom(relationship.types().getFirst());
        var outgoing = state.graphCall("edge", id, type, left, right);
        var incoming = state.graphCall("edge", id, type, right, left);
        return switch (relationship.direction()) {
            case OUT -> outgoing;
            case IN -> incoming;
            case NONE -> "(" + outgoing + " ; " + incoming + ")";
        };
    }

    // {k: v} de un nodo o relacion. Con variable se filtra como WHERE x.k = v (y x.k queda en la tabla
    // para reutilizarse en el RETURN); sin variable se pide la propiedad con un nombre fresco __V1, ...
    private static State properties(State state, Optional<String> variable, String id, List<PropertyEntry> entries) {
        var current = state;
        for (PropertyEntry entry : entries) {
            current = variable.isPresent()
                    ? condition(current, new Binary(BinaryOp.EQ, new PropertyAccess(variable.get(), entry.key()), entry.value()))
                    : anonymousProperty(current, id, entry);
        }
        return current;
    }

    private static State anonymousProperty(State state, String id, PropertyEntry entry) {
        var value = state.symbols().nextFresh("V");
        var declared = state.withSymbols(state.symbols().withUsed(value))
                .emit(state.graphCall("prop", id, atom(entry.key()), value));
        var expected = term(declared, entry.value());
        return expected.state().emit(call("cy_eq", value, expected.text()));
    }

    // cy_neq(__E1, __E2) para cada par de relaciones del MATCH
    private static State distinctEdges(State state, List<String> edges) {
        var current = state;
        for (int i = 0; i < edges.size(); i++) {
            for (int j = i + 1; j < edges.size(); j++) {
                current = current.emit(call("cy_neq", edges.get(i), edges.get(j)));
            }
        }
        return current;
    }


    // where := "WHERE" expression
    private static State where(State state, Optional<WhereClause> where) {
        return where.map(w -> condition(state, w.condition())).orElse(state);
    }

    // comparison -> se calculan ambos lados (left primero) y luego el comparador del engine
    // AND -> un goal tras otro;  OR -> once(( izq ; der ));  NOT -> \+ ( ... )
    // OR va dentro de once: es un filtro, si ambas ramas se cumplen la fila no debe salir dos veces
    private static State condition(State state, Expr expr) {
        return switch (expr) {
            case Binary(BinaryOp op, Expr left, Expr right) -> {
                var l = term(state, left);
                var r = term(l.state(), right);
                yield r.state().emit(call(comparator(op), l.text(), r.text()));
            }
            case And(Expr left, Expr right) -> condition(condition(state, left), right);
            case Or(Expr left, Expr right) -> state.emit("once((" + branch(state, left) + " ; " + branch(state, right) + "))");
            case Not(Expr operand) -> state.emit("\\+ (" + branch(state, operand) + ")");
            case PropertyAccess _, Var _, NumberLit _, DecimalLit _, StringLit _, BoolLit _ ->
                    throw new CodeGenException("A condition must be a comparison: " + text(expr));
        };
    }

    /*
     * Goals de una rama de OR o del operando de NOT, separados por coma.
     * Cada rama pide las propiedades que use: si en una rama falta la propiedad, esa rama falla
     * pero la otra puede cumplirse (a OR b). Lo que la rama agrega a la tabla de simbolos se descarta,
     * porque fuera de la rama no se sabe si quedo ligado; si se vuelve a pedir, se pide con el mismo
     * nombre (el valor de una propiedad es unico, asi que unifica con lo que ya hubiera).
     * Limitacion conocida (null, logica tri-valuada): NOT x.p = v tambien se cumple si x no tiene p.
     */
    private static String branch(State state, Expr expr) {
        var inner = condition(new State(state.graph(), state.symbols(), List.of()), expr);
        return String.join(", ", inner.goals());
    }

    // Comparadores de prolog/cy_engine.pl
    private static String comparator(BinaryOp op) {
        return switch (op) {
            case EQ -> "cy_eq";
            case NEQ -> "cy_neq";
            case LT -> "cy_lt";
            case GT -> "cy_gt";
            case LTE -> "cy_le";
            case GTE -> "cy_ge";
        };
    }

    // Valor de una expresion como termino Prolog
    private static Term term(State state, Expr expr) {
        return switch (expr) {
            case NumberLit(long value) -> new Term(state, Long.toString(value));
            case DecimalLit(double value) -> new Term(state, Double.toString(value));
            case StringLit(String value) -> new Term(state, atom(value));
            case BoolLit(boolean value) -> new Term(state, Boolean.toString(value));
            case Var(String name) -> new Term(state, state.id(name));
            case PropertyAccess(String variable, String property) -> property(state, variable, property);
            case Binary _, And _, Or _, Not _ ->
                    throw new CodeGenException("A condition cannot be used as a value: " + text(expr));
        };
    }

    // a.name -> user_G_prop(__A, name, __A_name), solo la primera vez que se usa
    private static Term property(State state, String variable, String property) {
        var known = state.symbols().property(variable, property);
        if (known.isPresent()) return new Term(state, known.get());

        var declared = state.withSymbols(state.symbols().withProperty(variable, property));
        var value = declared.symbols().property(variable, property).orElseThrow();
        var fetched = declared.emit(declared.graphCall("prop", declared.id(variable), atom(property), value));
        return new Term(fetched, value);
    }

    // Calcula varias expresiones en orden, acumulando los goals que necesite cada una
    private static Terms terms(State state, List<Expr> exprs) {
        var current = state;
        var texts = new ArrayList<String>();
        for (Expr expr : exprs) {
            var t = term(current, expr);
            current = t.state();
            texts.add(t.text());
        }
        return new Terms(current, List.copyOf(texts));
    }


    // updates := (create | set | remove | delete)*
    // Cada clausula emite goals con efecto en el engine (cy_new_node, cy_set_prop, ...),
    // en el orden en que aparecen en el query
    private static State updates(State state, List<UpdateClause> updates) {
        var current = state;
        for (UpdateClause update : updates) {
            current = switch (update) {
                case CreateClause(var patterns) -> create(current, patterns);
                case SetClause(List<SetItem> items) -> set(current, items);
                case RemoveClause(List<RemoveItem> items) -> remove(current, items);
                case DeleteClause(boolean detach, List<String> variables) -> delete(current, detach, variables);
            };
        }
        return current;
    }

    // create := "CREATE" pattern ("," pattern)*
    private static State create(State state, List<? extends PatternElement> patterns) {
        var current = state;
        for (PatternElement element : patterns) {
            current = switch (element) {
                case PatternNode node -> createNode(current, node).state();
                case PathPattern path -> createPath(current, path);
            };
        }
        return current;
    }

    // Nodo del CREATE. Si la variable ya existe (MATCH o CREATE anterior) se reutiliza tal cual,
    // como (a) y (b) en CREATE (a)-[:CONOCE]->(b). Si no: cy_new_node, sus etiquetas y sus propiedades.
    private static Element createNode(State state, PatternNode node) {
        var bound = node.variable().flatMap(name -> state.symbols().variable(name));
        if (bound.isPresent()) {
            if (!node.labels().isEmpty() || !node.properties().isEmpty()) {
                throw new CodeGenException("Variable '" + node.variable().get()
                        + "' is already declared: CREATE cannot add labels or properties to it (use SET)");
            }
            return new Element(state, bound.get());
        }
        var declared = declare(state, node.variable(), "N");
        var id = declared.id();
        var created = declared.state().emit(declared.state().updateCall("cy_new_node", id));
        return new Element(setProperties(addLabels(created, id, node.labels()), id, node.properties()), id);
    }

    // (a)-[r:T {k: v}]->(b) del CREATE: primero los nodos (nuevos o ya existentes) y luego el arco.
    // Como en Cypher, la relacion debe tener exactamente un tipo y una direccion.
    private static State createPath(State state, PathPattern path) {
        var relationship = path.relationship();
        if (relationship.types().size() != 1) {
            throw new CodeGenException("A relationship in CREATE must have exactly one type");
        }
        relationship.variable()
                .filter(name -> state.symbols().variable(name).isPresent())
                .ifPresent(name -> {
                    throw new CodeGenException("Relationship variable '" + name + "' is already declared");
                });

        var left = createNode(state, path.left());
        var right = createNode(left.state(), path.right());
        var forward = switch (relationship.direction()) {
            case OUT -> true;
            case IN -> false;
            case NONE -> throw new CodeGenException("A relationship in CREATE must have a direction (-> or <-)");
        };
        var from = forward ? left.id() : right.id();
        var to = forward ? right.id() : left.id();

        var declared = declare(right.state(), relationship.variable(), "E");
        var id = declared.id();
        var type = atom(relationship.types().getFirst());
        var created = declared.state().emit(declared.state().updateCall("cy_new_edge", id, type, from, to));
        return setProperties(created, id, relationship.properties());
    }

    private static State addLabels(State state, String id, List<String> labels) {
        return state.emitAll(labels.stream()
                .map(label -> state.updateCall("cy_add_label", id, atom(label)))
                .toList());
    }

    // {k: v} de un elemento creado: se calcula el valor (puede ser o.id) y se guarda con cy_set_prop
    private static State setProperties(State state, String id, List<PropertyEntry> entries) {
        var current = state;
        for (PropertyEntry entry : entries) {
            var value = term(current, entry.value());
            current = value.state().emit(value.state().updateCall("cy_set_prop", id, atom(entry.key()), value.text()));
        }
        return current;
    }

    // set := "SET" item ("," item)*     item := x.k "=" expr | x:L1:L2
    private static State set(State state, List<SetItem> items) {
        var current = state;
        for (SetItem item : items) {
            current = switch (item) {
                case SetProperty(PropertyAccess(String variable, String property), Expr value) -> {
                    var t = term(current, value);
                    var id = t.state().id(variable);
                    yield t.state().emit(t.state().updateCall("cy_set_prop", id, atom(property), t.text()))
                            .forget(variable, property);
                }
                case SetLabels(String variable, List<String> labels) -> addLabels(current, current.id(variable), labels);
            };
        }
        return current;
    }

    // remove := "REMOVE" item ("," item)*     item := x.k | x:L1:L2
    private static State remove(State state, List<RemoveItem> items) {
        var current = state;
        for (RemoveItem item : items) {
            current = switch (item) {
                case RemoveProperty(PropertyAccess(String variable, String property)) ->
                        current.emit(current.updateCall("cy_remove_prop", current.id(variable), atom(property)))
                                .forget(variable, property);
                case RemoveLabels(String variable, List<String> labels) -> {
                    var id = current.id(variable);
                    var base = current;
                    yield current.emitAll(labels.stream()
                            .map(label -> base.updateCall("cy_remove_label", id, atom(label)))
                            .toList());
                }
            };
        }
        return current;
    }

    // delete := ["DETACH"] "DELETE" var ("," var)*
    // El engine decide si es nodo o relacion; un nodo con relaciones solo se borra con DETACH
    private static State delete(State state, boolean detach, List<String> variables) {
        var predicate = detach ? "cy_detach_delete" : "cy_delete";
        return state.emitAll(variables.stream()
                .map(variable -> state.updateCall(predicate, state.id(variable)))
                .toList());
    }


    // return := "RETURN" items -> un argumento de cy_row por item.
    // Restriccion P2: una variable de patron devuelve solo su id.
    private static Terms projection(State state, List<ProjectionItem> items) {
        return terms(state, items.stream().map(ProjectionItem::expr).toList());
    }

    // Nombre de columna: el alias si hay, si no el texto del query (a.name, a)
    private static List<String> columns(List<ProjectionItem> items) {
        return items.stream()
                .map(item -> item.alias().orElseGet(() -> text(item.expr())))
                .toList();
    }
}