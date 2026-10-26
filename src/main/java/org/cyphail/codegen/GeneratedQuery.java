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
 */
public record GeneratedQuery(String head, List<String> body, List<String> columns, SymbolTable symbols) {

    /** Variable del goal externo con el id del query (lo genera el engine). */
    public static final String QUERY_ID = "__QueryId";

    /** Variable que el broker lee de la respuesta MQI: "header\nfila1\nfila2...". */
    public static final String ROWS = "Rows";

    public GeneratedQuery {
        body = List.copyOf(body);
        columns = List.copyOf(columns);
    }

    /** Clausula (Head :- Body). Si no hay goals el cuerpo es 'true'. */
    public String clause() {
        var goals = body.isEmpty() ? "true" : String.join(", ", body);
        return "(" + head + " :- " + goals + ")";
    }

    /**
     * Goal completo que el broker envia por MQI (sin punto final):
     * crear el query, ejecutarlo dejando la tabla en Rows y borrarlo.
     */
    public String goal() {
        var columnAtoms = PrologSyntax.list(columns.stream().map(PrologSyntax::atom).toList());
        return String.join(", ",
                "cy_create_query(" + QUERY_ID + ", " + clause() + ")",
                "cy_exec_query(" + QUERY_ID + ", " + columnAtoms + ", " + ROWS + ")",
                "cy_erase_query(" + QUERY_ID + ")");
    }
}
