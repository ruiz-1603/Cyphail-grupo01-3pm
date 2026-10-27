package org.cyphail.mqi;

import java.util.List;
import java.util.Map;

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
 * Three outcomes for a goal sent to SWI-Prolog via MQI.
 * Adapted from course MQI Demo (EIF400-II-2026).
 */
public sealed interface PrologResult {

    /**
     * Goal succeeded. One row per solution; keys are the goal variables.
     * Goal succeeded without bindings (e.g., assertz): a single empty map in rows.
     */
    record Success(List<Map<String, Object>> rows) implements PrologResult {}

    /** Goal failed: no solutions. Not an error. */
    record Failure() implements PrologResult {}

    /** Prolog exception (such as existence_error, type_error, syntax_error, timeout...). */
    record PrologError(Object term) implements PrologResult {}
}
