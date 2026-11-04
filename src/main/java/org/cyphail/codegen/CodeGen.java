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
 * y switch con pattern matching sobre las interfaces sealed (Expr, UpdateClause).
 * Alcance P2 (indicacion del profesor): solo MATCH (nodos y relaciones), WHERE (AND/OR/NOT) y RETURN.
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
    }

    // Termino Prolog que representa un valor, junto con el estado que dejo al calcularlo
    private record Term(State state, String text) {}

    // Lista de terminos calculados en orden (columnas del RETURN)
    private record Terms(State state, List<String> texts) {}

    // Nodo o relacion ya declarado: el estado y el termino Prolog que lo identifica (__A, __E1, ...)
    private record Element(State state, String id) {}


    /** Transpila el query para el grafo activo (el de .use). */
    public static GeneratedQuery generate(Query query, String graph) {
        rejectUpdates(query.updates());
        var matchClause = query.match()
                .orElseThrow(() -> new CodeGenException("A query without MATCH is not supported yet"));
        var items = query.returnClause()
                .orElseThrow(() -> new CodeGenException("A query without RETURN is not supported yet"))
                .projection().items();

        var start = new State(graph, SymbolTable.empty(), List.of());
        var matched = match(start, matchClause);
        var filtered = where(matched, query.where());
        var head = projection(filtered, items);

        return new GeneratedQuery(
                call(ROW_FUNCTOR, head.texts().toArray(String[]::new)),
                head.state().goals(),
                columns(items),
                head.state().symbols());
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


    // Por indicacion del profesor, P2 solo ejecuta MATCH, WHERE y RETURN: las clausulas
    // de actualizacion se parsean y se ven con .tree, pero no se transpilan
    private static void rejectUpdates(List<UpdateClause> updates) {
        updates.stream().findFirst().ifPresent(update -> {
            var clause = switch (update) {
                case CreateClause _ -> "CREATE";
                case SetClause _ -> "SET";
                case RemoveClause _ -> "REMOVE";
                case DeleteClause _ -> "DELETE";
            };
            throw new CodeGenException(clause + " is not supported in Sprint P2 (only MATCH, WHERE and RETURN)");
        });
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