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
/* este es el runtime de consultas (solo lectura) sobre SWI-Prolog

    Datos de cada grafo G:
      cy_db(G)                              el grafo existe
      user_G_node(Id)                       nodo
      user_G_label(NodeId, Label)           etiqueta (una fila por etiqueta)
      user_G_edge(EId, Type, From, To)      relacion dirigida
      user_G_prop(NodeOrEdgeId, Key, Value) propiedad de nodo o relacion

    Ciclo de un query: cy_create_query/2 -> cy_exec_query/3 -> cy_erase_query/1
    Los argumentos de Head, en orden, son las columnas del RETURN
*/

:- dynamic cy_current/1.         % grafo activo (.use)
:- dynamic cy_query_def/3.       % cy_query_def(QueryId, Head, Body)
:- dynamic cy_query_counter/1.   % ultimo numero de id de query usado
:- multifile cy_db/1.            % se define en un archivo por grafo

cy_query_counter(0).

% Separador de columnas de la tabla
cy_col_sep(' ').


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% Grafo actual (.use) %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

get_current_graph(G) :-
    cy_current(G).

% Falla si G no existe.
set_current_graph(G) :-
    cy_db(G),
    retractall(cy_current(_)),
    assertz(cy_current(G)).


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% Relaciones con direccion %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

% Llama a user_G_edge/4; el nombre se arma en ejecucion segun G
cy_user_edge(G, E, T, From, To) :-
    atomic_list_concat([user, G, edge], '_', Name),
    Goal =.. [Name, E, T, From, To],
    call(Goal).

%! cy_edge(+G, +Dir, ?E, ?Type, ?A, ?B)
%  out: (A)-[E]->(B) | in: (A)<-[E]-(B) | none: en cualquier sentido
cy_edge(G, out, E, T, A, B) :-
    cy_user_edge(G, E, T, A, B).
cy_edge(G, in, E, T, A, B) :-
    cy_user_edge(G, E, T, B, A).

% 'A \== B' va despues del call para que un self-loop cuente una sola vez
cy_edge(G, none, E, T, A, B) :-
    (   cy_user_edge(G, E, T, A, B)
    ;   cy_user_edge(G, E, T, B, A), A \== B
    ).


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% Comparadores %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Numero con numero por valor; texto con texto. Tipos mezclados o valores sin
% ligar hacen fallar el goal (la fila se descarta). Los literales de texto
% deben ser atomos ('Ana')

cy_comparable(X, Y) :- number(X), number(Y), !.
cy_comparable(X, Y) :- \+ number(X), \+ number(Y), atomic(X), atomic(Y).

cy_eq(X, Y) :- number(X), number(Y), !, X =:= Y.
cy_eq(X, Y) :- cy_comparable(X, Y), X == Y.

cy_neq(X, Y) :- cy_comparable(X, Y), \+ cy_eq(X, Y).

cy_lt(X, Y) :- number(X), number(Y), !, X < Y.
cy_lt(X, Y) :- cy_comparable(X, Y), X @< Y.

cy_gt(X, Y) :- number(X), number(Y), !, X > Y.
cy_gt(X, Y) :- cy_comparable(X, Y), X @> Y.

cy_le(X, Y) :- number(X), number(Y), !, X =< Y.
cy_le(X, Y) :- cy_comparable(X, Y), X @=< Y.

cy_ge(X, Y) :- number(X), number(Y), !, X >= Y.
cy_ge(X, Y) :- cy_comparable(X, Y), X @>= Y.


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% Ciclo de queries %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

% Id nuevo: cy_q1, cy_q2, ...
cy_next_query_id(Id) :-
    retract(cy_query_counter(N)),
    N1 is N + 1,
    assertz(cy_query_counter(N1)),
    format(atom(Id), 'cy_q~w', [N1]).

%! cy_create_query(?QueryId, +Clause)
%  Clause = (Head :- Body) | Head. Si QueryId no esta ligado se genera uno
%  Descarta queries viejos para no acumular basura si alguno fallo antes del erase

cy_create_query(QueryId, Clause) :-
    (   Clause = (Head :- Body) -> true ; Head = Clause, Body = true ),
    retractall(cy_query_def(_, _, _)),
    (   var(QueryId) -> cy_next_query_id(QueryId) ; true ),
    assertz(cy_query_def(QueryId, Head, Body)).

cy_erase_query(QueryId) :-
    retractall(cy_query_def(QueryId, _, _)).

cy_exec_query(QueryId, Columns, Rows) :-
    cy_exec_query(QueryId, Columns, Rows, _).

%! cy_exec_query(+QueryId, +Columns, -Rows, -Count)
%  Rows: atomo "header\nfila1\nfila2" (solo header si no hay soluciones).
%  Count: numero de soluciones. Falla si QueryId no existe.

cy_exec_query(QueryId, Columns, Rows, Count) :-
    cy_query_def(QueryId, Head, Body),
    findall(Head, Body, Solutions),
    length(Solutions, Count),
    cy_format_table(Columns, Solutions, Rows).


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% Formato de tabla %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

cy_format_table(Columns, Solutions, Rows) :-
    cy_col_sep(Sep),
    atomic_list_concat(Columns, Sep, Header),
    maplist(cy_format_row(Sep), Solutions, Lines),
    atomic_list_concat([Header|Lines], '\n', Rows).

cy_format_row(Sep, Head, Line) :-
    Head =.. [_|Values],
    maplist(cy_format_value, Values, Cells),
    atomic_list_concat(Cells, Sep, Line).

% Sin ligar -> null; cualquier otro valor se imprime tal cual (sin comillas)
cy_format_value(V, null) :- var(V), !.
cy_format_value(V, A) :- format(atom(A), '~w', [V]).