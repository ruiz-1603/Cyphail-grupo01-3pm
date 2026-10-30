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
/* Runtime de consultas sobre SWI-Prolog
 *
 * Datos de cada grafo G:
 *   cy_db(G)                              el grafo existe
 *   user_G_node(Id)                       nodo
 *   user_G_label(NodeId, Label)           etiqueta
 *   user_G_edge(EId, Type, From, To)      relacion dirigida
 *   user_G_prop(NodeOrEdgeId, Key, Value) propiedad de nodo o relacion
 *
 * Ciclo de un query: cy_create_query/2 -> cy_exec_query/3 -> cy_erase_query/1
 */

:- dynamic current_graph/1.
:- dynamic cy_query/1.

cy_db(amigos).
cy_db(empresa).
cy_db(movies).


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% Grafo actual (.use) %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

get_current_graph(G) :-
    current_graph(G).

set_current_graph(G) :-
    cy_db(G),
    (current_graph(_) -> retract(current_graph(_)) ; true),
    assert(current_graph(G)).


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% Comparadores %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

cy_eq(X, Y) :- number(X), number(Y), !, X =:= Y.
cy_eq(X, Y) :- atom(X), atom(Y), X = Y.

cy_neq(X, Y) :- number(X), number(Y), !, (X =:= Y -> fail ; true).
cy_neq(X, Y) :- atom(X), atom(Y), X \= Y.

cy_lt(X, Y) :- number(X), number(Y), X < Y.

cy_gt(X, Y) :- number(X), number(Y), X > Y.

cy_le(X, Y) :- number(X), number(Y), X =< Y.

cy_ge(X, Y) :- number(X), number(Y), X >= Y.


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% Ciclo de queries %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

cy_create_query(QueryId, Clause) :-
    (QueryId = cy_q -> true ; true),
    (Clause = (Head :- Body) -> true ; Head = Clause, Body = true),
    retractall(cy_query(_)),
    assert((cy_query(Head) :- Body)).

cy_exec_query(_QueryId, Columns, Rows) :-
    findall(Head, cy_query(Head), Solutions),
    cy_format_table(Columns, Solutions, Rows).

cy_erase_query(_QueryId) :-
    retractall(cy_query(_)).


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% Formato de tabla %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

cy_join_space([First | Rest], Text) :-
    format(atom(Init), '~w', [First]),
    foldl([X, Acc, NAcc] >> format(atom(NAcc), '~w ~w', [Acc, X]), Rest, Init, Text).

cy_join_lines([First | Rest], Text) :-
    format(atom(Init), '~w', [First]),
    foldl([X, Acc, NAcc] >> format(atom(NAcc), '~w~n~w', [Acc, X]), Rest, Init, Text).

cy_format_row(Head, Line) :-
    Head =.. [_ | Values],
    cy_join_space(Values, Line).

cy_format_table(Columns, Solutions, Rows) :-
    cy_join_space(Columns, Header),
    maplist([Head, Line] >> cy_format_row(Head, Line), Solutions, Lines),
    cy_join_lines([Header | Lines], Rows).
