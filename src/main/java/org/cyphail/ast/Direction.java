package org.cyphail.ast;

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