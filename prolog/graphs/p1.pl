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
/* Grafo "p1": datos de los casos de prueba del Sprint P1 (CASO 1 a CASO 12)
 *
 * Son los mismos datos que tenian los JSON del fake engine de P1 (data/movies.json,
 * books.json, person.json, order.json, client.json), pasados a hechos de Prolog para
 * que "todo P1 siga funcionando" con el engine real (Guia de Revision P2).
 * Todo en un solo grafo porque los casos mezclan etiquetas (Movie y Person, Person y Order).
 */

:- dynamic user_p1_node/1, user_p1_label/2, user_p1_prop/3, user_p1_edge/4.
:- discontiguous user_p1_node/1, user_p1_label/2, user_p1_prop/3.

% Movie (CASOS 1, 4, 5, 6)
user_p1_node(m1). user_p1_label(m1, 'Movie').
user_p1_prop(m1, title, 'Inception').     user_p1_prop(m1, year, 2010).
user_p1_node(m2). user_p1_label(m2, 'Movie').
user_p1_prop(m2, title, 'The Matrix').    user_p1_prop(m2, year, 1999).
user_p1_node(m3). user_p1_label(m3, 'Movie').
user_p1_prop(m3, title, 'Interstellar').  user_p1_prop(m3, year, 2014).
user_p1_node(m4). user_p1_label(m4, 'Movie').
user_p1_prop(m4, title, 'Titanic').       user_p1_prop(m4, year, 1997).

% Book (CASO 2)
user_p1_node(b1). user_p1_label(b1, 'Book').
user_p1_prop(b1, title, 'The Hobbit').        user_p1_prop(b1, pages, 310).
user_p1_node(b2). user_p1_label(b2, 'Book').
user_p1_prop(b2, title, '1984').              user_p1_prop(b2, pages, 328).
user_p1_node(b3). user_p1_label(b3, 'Book').
user_p1_prop(b3, title, 'The Great Gatsby').  user_p1_prop(b3, pages, 180).
user_p1_node(b4). user_p1_label(b4, 'Book').
user_p1_prop(b4, title, 'War and Peace').     user_p1_prop(b4, pages, 1225).

% Person (CASOS 3 a 11); Priscilla y Daniel tambien son Employee (CASO 3)
user_p1_node(p1). user_p1_label(p1, 'Person'). user_p1_label(p1, 'Employee').
user_p1_prop(p1, id, 1). user_p1_prop(p1, name, 'Priscilla'). user_p1_prop(p1, age, 35).
user_p1_node(p2). user_p1_label(p2, 'Person').
user_p1_prop(p2, id, 2). user_p1_prop(p2, name, 'Aaron').     user_p1_prop(p2, age, 22).
user_p1_node(p3). user_p1_label(p3, 'Person').
user_p1_prop(p3, id, 3). user_p1_prop(p3, name, 'Samael').    user_p1_prop(p3, age, 65).
user_p1_node(p4). user_p1_label(p4, 'Person'). user_p1_label(p4, 'Employee').
user_p1_prop(p4, id, 4). user_p1_prop(p4, name, 'Daniel').    user_p1_prop(p4, age, 40).
user_p1_node(p5). user_p1_label(p5, 'Person').
user_p1_prop(p5, id, 5). user_p1_prop(p5, name, 'Nicolas').   user_p1_prop(p5, age, 17).

% Order (CASOS 8 a 11): personId apunta al id de una Person
user_p1_node(o1). user_p1_label(o1, 'Order').
user_p1_prop(o1, id, 100). user_p1_prop(o1, personId, 1). user_p1_prop(o1, status, completed). user_p1_prop(o1, total, 50000).
user_p1_node(o2). user_p1_label(o2, 'Order').
user_p1_prop(o2, id, 101). user_p1_prop(o2, personId, 3). user_p1_prop(o2, status, cancelled). user_p1_prop(o2, total, 12000).
user_p1_node(o3). user_p1_label(o3, 'Order').
user_p1_prop(o3, id, 102). user_p1_prop(o3, personId, 4). user_p1_prop(o3, status, pending).   user_p1_prop(o3, total, 8000).

% Client (CASO 12): los activos son VIP
user_p1_node(c1). user_p1_label(c1, 'Client'). user_p1_label(c1, 'VIP').
user_p1_prop(c1, id, 101). user_p1_prop(c1, name, 'Carlos').  user_p1_prop(c1, status, active).   user_p1_prop(c1, rating, 95).
user_p1_node(c2). user_p1_label(c2, 'Client'). user_p1_label(c2, 'VIP').
user_p1_prop(c2, id, 102). user_p1_prop(c2, name, 'Elena').   user_p1_prop(c2, status, active).   user_p1_prop(c2, rating, 92).
user_p1_node(c3). user_p1_label(c3, 'Client').
user_p1_prop(c3, id, 103). user_p1_prop(c3, name, 'Roberto'). user_p1_prop(c3, status, inactive). user_p1_prop(c3, rating, 70).

% Sin relaciones (user_p1_edge/4 no tiene hechos): P1 solo tenia patrones de nodo.
% Al ser dynamic, un patron con relacion sobre este grafo simplemente no da filas.
