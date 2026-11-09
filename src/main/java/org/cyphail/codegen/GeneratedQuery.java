package org.cyphail.codegen;

import java.util.List;

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
 * Resultado de transpilar un query Cyphail.
 *
 * @param head    cabeza de la clausula: cy_row(...) con una variable por columna, en orden
 * @param body    goals del cuerpo, en el orden en que Prolog los ejecuta
 * @param columns nombres de las columnas del RETURN (alias o texto de la expresion)
 * @param symbols tabla variable Cyphail -> variable Prolog, para armar la tabla en Java
 * @param updates el query tiene CREATE/SET/REMOVE/DELETE: el engine cuenta los cambios
 */
public record GeneratedQuery(String head, List<String> body, List<String> columns, SymbolTable symbols,
                             boolean updates) {

    /** Variable del goal externo con el id del query (lo genera el engine). */
    public static final String QUERY_ID = "__QueryId";

    /** Variable que el broker lee de la respuesta MQI: "header\nfila1\nfila2...". */
    public static final String ROWS = "Rows";

    /** Variable con el resumen de cambios de un query con actualizaciones ("1 node(s) created, ..."). */
    public static final String STATS = "Stats";

    public GeneratedQuery {
        body = List.copyOf(body);
        columns = List.copyOf(columns);
    }

    /** Query de solo lectura (MATCH, WHERE, RETURN). */
    public GeneratedQuery(String head, List<String> body, List<String> columns, SymbolTable symbols) {
        this(head, body, columns, symbols, false);
    }

    /** Clausula (Head :- Body). Si no hay goals el cuerpo es 'true'. */
    public String clause() {
        var goals = body.isEmpty() ? "true" : String.join(", ", body);
        return "(" + head + " :- " + goals + ")";
    }

    /**
     * Goal completo que el broker envia por MQI (sin punto final):
     * crear el query, ejecutarlo dejando la tabla en Rows y borrarlo.
     * Con actualizaciones se reinician los contadores antes y el resumen queda en Stats;
     * sin RETURN no hay tabla: cy_run_query solo recorre las filas para aplicar los cambios.
     */
    public String goal() {
        var columnAtoms = PrologSyntax.list(columns.stream().map(PrologSyntax::atom).toList());
        var run = columns.isEmpty()
                ? "cy_run_query(" + QUERY_ID + ")"
                : "cy_exec_query(" + QUERY_ID + ", " + columnAtoms + ", " + ROWS + ")";
        var steps = List.of(
                "cy_create_query(" + QUERY_ID + ", " + clause() + ")",
                run,
                "cy_erase_query(" + QUERY_ID + ")");
        return updates
                ? String.join(", ", "cy_reset_stats", String.join(", ", steps), "cy_stats(" + STATS + ")")
                : String.join(", ", steps);
    }
}
