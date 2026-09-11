package org.cyphail.repl;

/*
* * Proyecto Cyphail
 * Grupo 01-3pm
 *
 * Autores:
 * - Priscilla Murillo Romero
 * - Aaron Ruiz Medina
 * - Samael Sanchez Mora
 * - Daniel Villarroel Abaduca
 * - Nicolás Zárate Hernández
*/

public final class ReplMessages {
    private ReplMessages() {}

    public static String welcome() {
        return String.join("\n",
                "────────────────────────────────────────",
                "  Cyphail-01-3pm  v0.1",
                "  ESCINF / UNA · EIF400-II-2026",
                "  Visit www.whatiscyphail.com for more information",
                "────────────────────────────────────────",
                "  Welcome to Cyphail!",
                "────────────────────────────────────────",
                "  Type '.help' for help or '.exit' to quit.\n");
    }

    public static String help() {
        return String.join("\n",
                "Available Commands:",
                "  .help      - Show this help message",
                "  .about     - Show project information",
                "  .exit      - Exit the REPL",
                "  .use       - List available graphs or select a graph",
                "             - Usage: .use <graph_name>",
                "",
                "Query Examples:",
                "  MATCH (p:Persona) RETURN p.nombre, p.edad",
                "  MATCH (p1:Persona)-[r:AMIGO_DE]->(p2:Persona) RETURN p1.nombre, type(r), p2.nombre",
                "");
    }

    public static String about() {
        return String.join("\n",
                "Project: Cyphail - Graph Query Prototype",
                "Course: EIF400-II-2026",
                "School: Universidad Nacional de Costa Rica",
                "Professor: Carlos Loría-Sáenz",
                "",
                "Team Members (Grupo 01-3pm):",
                "  - Priscilla Murillo Romero",
                "  - Aaron Ruiz Medina",
                "  - Samael Sanchez Mora",
                "  - Daniel Villarroel Abaduca",
                "  - Nicolás Zárate Hernández",
                "");
    }
}

