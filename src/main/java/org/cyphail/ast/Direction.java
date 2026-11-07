package org.cyphail.ast;

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
 * Direccion de una relacion, leida de izquierda a derecha en el patron.
 */
public enum Direction {
    /** (a)-[r]->(b): de a hacia b */
    OUT,
    /** (a)<-[r]-(b): de b hacia a */
    IN,
    /** (a)-[r]-(b): en cualquier sentido */
    NONE
}