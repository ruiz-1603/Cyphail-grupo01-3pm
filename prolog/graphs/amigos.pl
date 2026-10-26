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
/** Grafo "amigos". Casos limite: ciclo (n1<->n2), self-loop (n4), nodo sin 'age' (n5),
    nodo sin etiqueta Person (n6), relacion con propiedades (e3, e5), dos tipos de relacion. */

:- multifile cy_db/1.
:- discontiguous user_amigos_label/2, user_amigos_prop/3.
:- dynamic user_amigos_node/1, user_amigos_label/2, user_amigos_prop/3, user_amigos_edge/4.


cy_db(amigos).

user_amigos_node(n1).  user_amigos_node(n2).  user_amigos_node(n3).
user_amigos_node(n4).  user_amigos_node(n5).  user_amigos_node(n6).

user_amigos_label(n1, 'Person').  user_amigos_label(n1, 'Female').
user_amigos_label(n2, 'Person').  user_amigos_label(n2, 'Male').
user_amigos_label(n3, 'Person').  user_amigos_label(n3, 'Male').
user_amigos_label(n4, 'Person').  user_amigos_label(n4, 'Female').
user_amigos_label(n5, 'Person').  user_amigos_label(n5, 'Male').
user_amigos_label(n6, 'Robot').

user_amigos_prop(n1, name, 'Ana').      user_amigos_prop(n1, age, 28).
user_amigos_prop(n2, name, 'Luis').     user_amigos_prop(n2, age, 31).
user_amigos_prop(n3, name, 'Carlos').   user_amigos_prop(n3, age, 17).
user_amigos_prop(n4, name, 'Beatriz').  user_amigos_prop(n4, age, 34).
user_amigos_prop(n5, name, 'David').
user_amigos_prop(n6, name, 'R2').       user_amigos_prop(n6, age, 99).

user_amigos_edge(e1, amigo,  n1, n2).
user_amigos_edge(e2, amigo,  n2, n1).
user_amigos_edge(e3, amigo,  n2, n3).
user_amigos_edge(e4, amigo,  n4, n4).
user_amigos_edge(e5, conoce, n1, n4).

user_amigos_prop(e3, since, 2010).
user_amigos_prop(e5, since, 1999).