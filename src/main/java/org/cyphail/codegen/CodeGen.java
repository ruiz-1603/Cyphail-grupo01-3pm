package org.cyphail.codegen;

import org.cyphail.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
 * Alcance P2 (indicacion del profesor): solo MATCH, WHERE y RETURN.
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


    /** Transpila el query para el grafo activo (el de .use). */
    public static GeneratedQuery generate(Query query, String graph) {
        rejectUpdates(query.updates());

        var start = new State(graph, SymbolTable.empty(), List.of());
        var matched = match(start, query.match());
        var filtered = where(matched, query.where());
        var head = projection(filtered, query.returnClause().projection().items());

        return new GeneratedQuery(
                call(ROW_FUNCTOR, head.texts().toArray(String[]::new)),
                head.state().goals(),
                columns(query.returnClause().projection().items()),
                head.state().symbols());
    }


    // match := "MATCH" patterns
    private static State match(State state, MatchClause match) {
        var current = state;
        for (PatternNode node : match.patterns()) {
            current = matchNode(current, node);
        }
        return current;
    }

    // (a:L1:L2 {k: v}) -> nodo (si a es nueva), una etiqueta por label y un filtro por propiedad.
    // Si 'a' ya aparecio antes (variable de join) no se vuelve a pedir user_G_node.
    private static State matchNode(State state, PatternNode node) {
        var isNew = state.symbols().variable(node.variable()).isEmpty();
        var declared = state.withSymbols(state.symbols().withVariable(node.variable()));
        var id = declared.id(node.variable());

        var withNode = isNew ? declared.emit(declared.graphCall("node", id)) : declared;
        var withLabels = withNode.emitAll(node.labels().stream()
                .map(label -> withNode.graphCall("label", id, atom(label)))
                .toList());

        var current = withLabels;
        for (PropertyEntry entry : node.properties()) {
            current = condition(current,
                    new Binary(BinaryOp.EQ, new PropertyAccess(node.variable(), entry.key()), entry.value()));
        }
        return current;
    }


    // where := "WHERE" comparison
    private static State where(State state, Optional<WhereClause> where) {
        return where.map(w -> condition(state, w.condition())).orElse(state);
    }

    // comparison -> se calculan ambos lados (left primero) y luego el comparador del engine
    private static State condition(State state, Expr expr) {
        return switch (expr) {
            case Binary(BinaryOp op, Expr left, Expr right) -> {
                var l = term(state, left);
                var r = term(l.state(), right);
                yield r.state().emit(call(comparator(op), l.text(), r.text()));
            }
            case PropertyAccess _, Var _, NumberLit _, StringLit _ ->
                    throw new CodeGenException("A condition must be a comparison: " + text(expr));
        };
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
            case StringLit(String value) -> new Term(state, atom(value));
            case Var(String name) -> new Term(state, state.id(name));
            case PropertyAccess(String variable, String property) -> property(state, variable, property);
            case Binary _ ->
                    throw new CodeGenException("A comparison cannot be used as a value: " + text(expr));
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

    // Expresion como se escribio en Cyphail (para columnas y mensajes de error)
    private static String text(Expr expr) {
        return switch (expr) {
            case PropertyAccess(String variable, String property) -> variable + "." + property;
            case Var(String name) -> name;
            case NumberLit(long value) -> Long.toString(value);
            case StringLit(String value) -> value;
            case Binary(BinaryOp op, Expr left, Expr right) -> text(left) + " " + op.symbol() + " " + text(right);
        };
    }
}
