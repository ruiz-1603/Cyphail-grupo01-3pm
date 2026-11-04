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

/* Runtime de consultas
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
:- dynamic cy_query/2.
:- dynamic cy_graphs_dir/1.

cy_db(amigos).
cy_db(empresa).
cy_db(movies).
cy_db(p1).

% Carpeta de los grafos (graphs/ junto a este archivo) sin importar desde donde se cargue el engine

:- prolog_load_context(directory, Dir),
   atom_concat(Dir, '/graphs', GraphsDir),
   retractall(cy_graphs_dir(_)),
   assert(cy_graphs_dir(GraphsDir)).


%%%%%%%%%%%%%%%%% grafo actual (.use)

get_current_graph(G) :-
    current_graph(G).

% .use G: vuelve a cargar el grafo desde graphs/G.pl, asi siempre queda como esta en disco

set_current_graph(G) :-
    cy_db(G),
    cy_load_graph(G),
    (current_graph(_) -> retract(current_graph(_)) ; true),
    assert(current_graph(G)).

% Al recargar un archivo, SWI conserva lo que se agrego con assert a sus predicados dynamic
% por eso primero se borran los hechos del grafo y luego se vuelve a cargar el archivo

cy_load_graph(G) :-
    cy_clear_graph(G),
    cy_graphs_dir(Dir),
    format(atom(File), '~w/~w.pl', [Dir, G]),
    consult(File).

cy_clear_graph(G) :-
    forall(member(Suffix-Arity, [node-1, label-2, prop-3, edge-4]),
           ( atomic_list_concat([user, G, Suffix], '_', Name),
             functor(Head, Name, Arity),
             retractall(Head) )).


%%%%%%%%%%%%% comparadores

cy_eq(X, Y) :- number(X), number(Y), !, X =:= Y.
cy_eq(X, Y) :- atom(X), atom(Y), X = Y.

cy_neq(X, Y) :- number(X), number(Y), !, (X =:= Y -> fail ; true).
cy_neq(X, Y) :- atom(X), atom(Y), X \= Y.

cy_lt(X, Y) :- number(X), number(Y), X < Y.

cy_gt(X, Y) :- number(X), number(Y), X > Y.

cy_le(X, Y) :- number(X), number(Y), X =< Y.

cy_ge(X, Y) :- number(X), number(Y), X >= Y.


%%%%%%%%%%%%%% ciclo de queries

% QueryId llega sin ligar (__QueryId en el goal del CodeGen): gensym le da un id unico
% (cy_q1, cy_q2, ...) y la clausula se guarda con ese id como en el ejemplo del SPEC

cy_create_query(QueryId, Clause) :-
    (var(QueryId) -> gensym(cy_q, QueryId) ; true),
    (Clause = (Head :- Body) -> true ; Head = Clause, Body = true),
    assert((cy_query(QueryId, Head) :- Body)).

cy_exec_query(QueryId, Columns, Rows) :-
    findall(Head, cy_query(QueryId, Head), Solutions),
    cy_format_table(Columns, Solutions, Rows).

cy_erase_query(QueryId) :-
    retractall(cy_query(QueryId, _)).


%%%%%%%%%%%%%%%%%%%%% formato de tabla
% Filas separadas por cambio de linea y columnas por tab un valor con espacios

cy_join_columns([First | Rest], Text) :-
    format(atom(Init), '~w', [First]),
    foldl([X, Acc, NAcc] >> format(atom(NAcc), '~w\t~w', [Acc, X]), Rest, Init, Text).

cy_join_lines([First | Rest], Text) :-
    format(atom(Init), '~w', [First]),
    foldl([X, Acc, NAcc] >> format(atom(NAcc), '~w~n~w', [Acc, X]), Rest, Init, Text).

cy_format_row(Head, Line) :-
    Head =.. [_ | Values],
    cy_join_columns(Values, Line).

cy_format_table(Columns, Solutions, Rows) :-
    cy_join_columns(Columns, Header),
    maplist([Head, Line] >> cy_format_row(Head, Line), Solutions, Lines),
    cy_join_lines([Header | Lines], Rows).
