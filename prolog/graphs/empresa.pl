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
/* Grafo "empresa" (ejemplo del SPEC: Persona-[:TRABAJA_EN]->Empresa)
 *
 * Modelado como si se hubiera creado desde el repl (ejercicio 0, sesion 23/10) para probar
 * los casos de referencia de P2: TRABAJA_EN {cargo, desde}, CONOCE {desde}, SOCIO_DE {porcentaje}.
 */

:- dynamic user_empresa_node/1, user_empresa_label/2, user_empresa_prop/3, user_empresa_edge/4.
:- discontiguous user_empresa_prop/3, user_empresa_edge/4.

user_empresa_node(p1). user_empresa_node(p2). user_empresa_node(p3).
user_empresa_node(p4). user_empresa_node(c1). user_empresa_node(c2).
user_empresa_node(c3).

user_empresa_label(p1, 'Persona').  user_empresa_label(p2, 'Persona').
user_empresa_label(p3, 'Persona').  user_empresa_label(p4, 'Persona').
user_empresa_label(c1, 'Empresa').  user_empresa_label(c2, 'Empresa').
user_empresa_label(c3, 'Empresa').

user_empresa_prop(p1, nombre, 'Marta').  user_empresa_prop(p1, edad, 40).  user_empresa_prop(p1, activo, true).
user_empresa_prop(p2, nombre, 'Jose').   user_empresa_prop(p2, edad, 35).  user_empresa_prop(p2, activo, true).
user_empresa_prop(p3, nombre, 'Lucia').  user_empresa_prop(p3, edad, 29).  user_empresa_prop(p3, activo, false).
user_empresa_prop(p4, nombre, 'Pedro').
user_empresa_prop(c1, nombre, 'UCR').    user_empresa_prop(c1, pais, 'CR').
user_empresa_prop(c2, nombre, 'UNA').    user_empresa_prop(c2, pais, 'CR').
user_empresa_prop(c3, nombre, 'Acme').   user_empresa_prop(c3, pais, 'US').

% TRABAJA_EN: Persona -> Empresa
user_empresa_edge(r1, 'TRABAJA_EN', p1, c1).
user_empresa_edge(r2, 'TRABAJA_EN', p2, c1).
user_empresa_edge(r3, 'TRABAJA_EN', p3, c2).
user_empresa_edge(r4, 'TRABAJA_EN', p4, c2).
user_empresa_edge(r5, 'TRABAJA_EN', p2, c2).
user_empresa_edge(r6, 'TRABAJA_EN', p3, c3).

user_empresa_prop(r1, rol, 'Dev').    user_empresa_prop(r1, cargo, 'Dev').  user_empresa_prop(r1, desde, 2019).
user_empresa_prop(r2, rol, 'QA').     user_empresa_prop(r2, cargo, 'QA').   user_empresa_prop(r2, desde, 2016).
user_empresa_prop(r3, cargo, 'Dev').  user_empresa_prop(r3, desde, 2017).
user_empresa_prop(r5, cargo, 'Dev').  user_empresa_prop(r5, desde, 2021).
user_empresa_prop(r6, cargo, 'Dev').  user_empresa_prop(r6, desde, 2015).

% CONOCE: Persona -> Persona
user_empresa_edge(k1, 'CONOCE', p1, p2).
user_empresa_edge(k2, 'CONOCE', p2, p3).
user_empresa_edge(k3, 'CONOCE', p3, p1).
user_empresa_edge(k4, 'CONOCE', p4, p2).

user_empresa_prop(k1, desde, 2015).
user_empresa_prop(k2, desde, 2012).
user_empresa_prop(k3, desde, 1998).
user_empresa_prop(k4, desde, 2020).

% SOCIO_DE: Persona -> Empresa
user_empresa_edge(s1, 'SOCIO_DE', p2, c1).
user_empresa_edge(s2, 'SOCIO_DE', p4, c2).

user_empresa_prop(s1, porcentaje, 30.5).
user_empresa_prop(s2, porcentaje, 20).
