/*
 * Proyecto Cyphail
 * Grupo 01-3pm
 *
 * Autores:
 * - Priscilla Murillo Romero
 * - Aaron Ruiz Medina
 * - Samael Sanchez Mora
 * - Daniel Villarroel Abaduca
 * - Nicolas Zarate Hernandez
 */
/* Grafo "revision": datos de los casos C11 (SET multiple) y C12 (REMOVE multiple)
 * de la Guia de Revision P2 y de los ejemplos de actualizacion del SPEC P2 (II.4),
 * que usan etiquetas que no estan en los otros grafos. El SPEC asume que esos datos
 * "ya se crearon usando CREATE (o manualmente para hacer pruebas)".
 *
 *   C11: MATCH (u:Student {id: "4-666-000"})-[r:attends]->(c:Course {nrc: "50691"})
 *        SET r.grade = 9.5, u.activo = true, c.cupo = 30
 *   C12: MATCH (p:Product {code: "PROD-99"})
 *        REMOVE p.discount, p.promo, p:Oferta
 *   SPEC: MATCH (a:Autor {nombre: 'Bill Gates'}), (b:Libro {titulo: 'Fake Windows Millenium'})
 *         CREATE (a)-[:ESCRIBIO]->(b)
 *   SPEC: MATCH (a:Person:Female)-[r:KNOWS]->(b:Person:Male) WHERE r.since < 2000 DELETE r
 */

:- dynamic user_revision_node/1, user_revision_label/2, user_revision_prop/3, user_revision_edge/4.
:- discontiguous user_revision_node/1, user_revision_label/2, user_revision_prop/3, user_revision_edge/4.

% Student y Course (C11)
user_revision_node(s1). user_revision_label(s1, 'Student').
user_revision_prop(s1, id, '4-666-000'). user_revision_prop(s1, name, 'Daniel'). user_revision_prop(s1, activo, false).
user_revision_node(s2). user_revision_label(s2, 'Student').
user_revision_prop(s2, id, '1-234-567'). user_revision_prop(s2, name, 'Priscilla'). user_revision_prop(s2, activo, true).

user_revision_node(k1). user_revision_label(k1, 'Course').
user_revision_prop(k1, nrc, '50691'). user_revision_prop(k1, name, 'Paradigmas de Programacion'). user_revision_prop(k1, cupo, 25).
user_revision_node(k2). user_revision_label(k2, 'Course').
user_revision_prop(k2, nrc, '50690'). user_revision_prop(k2, name, 'Paradigmas de Programacion'). user_revision_prop(k2, cupo, 25).

user_revision_edge(a1, attends, s1, k1). user_revision_prop(a1, grade, 7.0).
user_revision_edge(a2, attends, s2, k1). user_revision_prop(a2, grade, 8.5).
user_revision_edge(a3, attends, s2, k2). user_revision_prop(a3, grade, 9.0).

% Product (C12): PROD-99 esta en oferta
user_revision_node(x1). user_revision_label(x1, 'Product'). user_revision_label(x1, 'Oferta').
user_revision_prop(x1, id, 99). user_revision_prop(x1, code, 'PROD-99'). user_revision_prop(x1, name, 'Teclado').
user_revision_prop(x1, discount, 15). user_revision_prop(x1, promo, 'Black Friday').
user_revision_node(x2). user_revision_label(x2, 'Product').
user_revision_prop(x2, id, 100). user_revision_prop(x2, code, 'PROD-100'). user_revision_prop(x2, name, 'Mouse').

% Autor y Libro (SPEC II.4, MATCH + CREATE): todavia no hay arco ESCRIBIO
user_revision_node(w1). user_revision_label(w1, 'Autor').
user_revision_prop(w1, nombre, 'Bill Gates').
user_revision_node(w2). user_revision_label(w2, 'Libro').
user_revision_prop(w2, titulo, 'Fake Windows Millenium').

% Person:Female -[:KNOWS]-> Person:Male (SPEC II.4, MATCH + WHERE + DELETE):
% solo el arco de 1995 cumple since < 2000; el de 2010 y el que llega a una Female se quedan
user_revision_node(h1). user_revision_label(h1, 'Person'). user_revision_label(h1, 'Female').
user_revision_prop(h1, name, 'Ana').
user_revision_node(h2). user_revision_label(h2, 'Person'). user_revision_label(h2, 'Male').
user_revision_prop(h2, name, 'Luis').
user_revision_node(h3). user_revision_label(h3, 'Person'). user_revision_label(h3, 'Male').
user_revision_prop(h3, name, 'Carlos').
user_revision_node(h4). user_revision_label(h4, 'Person'). user_revision_label(h4, 'Female').
user_revision_prop(h4, name, 'Beatriz').

user_revision_edge(q1, 'KNOWS', h1, h2). user_revision_prop(q1, since, 1995).
user_revision_edge(q2, 'KNOWS', h1, h3). user_revision_prop(q2, since, 2010).
user_revision_edge(q3, 'KNOWS', h4, h1). user_revision_prop(q3, since, 1990).
