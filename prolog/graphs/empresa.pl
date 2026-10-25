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

/** Grafo "empresa" (ejemplo del SPEC: Persona-[:TRABAJA_EN]->Empresa). */
:- multifile cy_db/1.
:- discontiguous user_empresa_label/2, user_empresa_prop/3.
:- dynamic user_empresa_node/1, user_empresa_label/2, user_empresa_prop/3, user_empresa_edge/4.

cy_db(empresa).

user_empresa_node(p1). user_empresa_node(p2). user_empresa_node(p3).
user_empresa_node(p4). user_empresa_node(c1). user_empresa_node(c2).

user_empresa_label(p1, 'Persona').  user_empresa_label(p2, 'Persona').
user_empresa_label(p3, 'Persona').  user_empresa_label(p4, 'Persona').
user_empresa_label(c1, 'Empresa').  user_empresa_label(c2, 'Empresa').

user_empresa_prop(p1, nombre, 'Marta').  user_empresa_prop(p1, edad, 40).
user_empresa_prop(p2, nombre, 'Jose').   user_empresa_prop(p2, edad, 35).
user_empresa_prop(p3, nombre, 'Lucia').  user_empresa_prop(p3, edad, 29).
user_empresa_prop(p4, nombre, 'Pedro').
user_empresa_prop(c1, nombre, 'UCR').
user_empresa_prop(c2, nombre, 'UNA').

user_empresa_edge(r1, 'TRABAJA_EN', p1, c1).
user_empresa_edge(r2, 'TRABAJA_EN', p2, c1).
user_empresa_edge(r3, 'TRABAJA_EN', p3, c2).
user_empresa_edge(r4, 'TRABAJA_EN', p4, c2).
user_empresa_edge(r5, 'TRABAJA_EN', p2, c2).

user_empresa_prop(r1, rol, 'Dev').
user_empresa_prop(r2, rol, 'QA').