:- encoding(utf8).
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

/** Grafo "movies" (casos P1: Movie con title/year, Person con name/age). */

:- multifile cy_db/1.
:- discontiguous user_movies_label/2, user_movies_prop/3.
:- dynamic user_movies_node/1, user_movies_label/2, user_movies_prop/3, user_movies_edge/4.

cy_db(movies).

user_movies_node(m1). user_movies_node(m2). user_movies_node(m3).
user_movies_node(a1). user_movies_node(a2).

user_movies_label(m1, 'Movie'). user_movies_label(m2, 'Movie'). user_movies_label(m3, 'Movie').
user_movies_label(a1, 'Person'). user_movies_label(a1, 'Employee').
user_movies_label(a2, 'Person').

user_movies_prop(m1, title, 'Matrix').   user_movies_prop(m1, year, 1999).
user_movies_prop(m2, title, 'Avatar').   user_movies_prop(m2, year, 2009).
user_movies_prop(m3, title, 'Sin Anio').
user_movies_prop(a1, name, 'Keanu').     user_movies_prop(a1, age, 60).
user_movies_prop(a2, name, 'Zoe').       user_movies_prop(a2, age, 40).

user_movies_edge(x1, 'ACTED_IN', a1, m1).
user_movies_edge(x2, 'ACTED_IN', a2, m2).