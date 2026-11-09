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
 * Query con CREATE/SET/REMOVE/DELETE: cy_reset_stats/0 antes y cy_stats/1 al final;
 * sin RETURN se ejecuta con cy_run_query/1 (no hay tabla, solo el resumen de cambios).
 */

:- dynamic current_graph/1.
:- dynamic cy_query/2.
:- dynamic cy_graphs_dir/1.
:- dynamic cy_stat/2.

cy_db(amigos).
cy_db(empresa).
cy_db(movies).
cy_db(p1).
cy_db(revision).

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
    consult(File),
    reset_gensym(cy_n),
    reset_gensym(cy_r).

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

% Query sin RETURN (solo actualizaciones): se recorren todas las soluciones por sus efectos

cy_run_query(QueryId) :-
    forall(cy_query(QueryId, _), true).


%%%%%%%%%%%%%% actualizaciones (CREATE, SET, REMOVE, DELETE)
% El CodeGen pone estos goals despues del MATCH/WHERE: se ejecutan una vez por fila.
% Los cambios quedan en memoria (los user_G_* son dynamic); .use G vuelve al archivo.
% SWI usa la vista logica: lo que se agrega o borra no cambia las filas que el MATCH ya esta recorriendo.

% Hecho del grafo G: cy_fact(empresa, prop, [p1, edad, 40], user_empresa_prop(p1, edad, 40))

cy_fact(G, Suffix, Args, Fact) :-
    atomic_list_concat([user, G, Suffix], '_', Name),
    Fact =.. [Name | Args].

% Id nuevo (cy_n1, cy_r1, ...) que no este usado por un nodo o relacion del grafo

cy_fresh_id(G, Prefix, Id) :-
    gensym(Prefix, Candidate),
    (cy_used_id(G, Candidate) -> cy_fresh_id(G, Prefix, Id) ; Id = Candidate).

cy_used_id(G, Id) :- cy_fact(G, node, [Id], Node), call(Node), !.
cy_used_id(G, Id) :- cy_fact(G, edge, [Id, _, _, _], Edge), call(Edge).

% CREATE

cy_new_node(G, Id) :-
    cy_fresh_id(G, cy_n, Id),
    cy_fact(G, node, [Id], Node),
    assertz(Node),
    cy_count(nodes_created).

cy_new_edge(G, Id, Type, From, To) :-
    cy_fresh_id(G, cy_r, Id),
    cy_fact(G, edge, [Id, Type, From, To], Edge),
    assertz(Edge),
    cy_count(relationships_created).

% SET x:L  /  SET x.k = v (reemplaza el valor anterior)

cy_add_label(G, Id, Label) :-
    cy_fact(G, label, [Id, Label], Fact),
    (call(Fact) -> true ; assertz(Fact), cy_count(labels_added)).

cy_set_prop(G, Id, Key, Value) :-
    cy_fact(G, prop, [Id, Key, _], Old),
    retractall(Old),
    cy_fact(G, prop, [Id, Key, Value], New),
    assertz(New),
    cy_count(properties_set).

% REMOVE x:L  /  REMOVE x.k (si no lo tiene no hace nada)

cy_remove_label(G, Id, Label) :-
    cy_fact(G, label, [Id, Label], Fact),
    (retract(Fact) -> cy_count(labels_removed) ; true).

cy_remove_prop(G, Id, Key) :-
    cy_fact(G, prop, [Id, Key, _], Fact),
    (retract(Fact) -> cy_count(properties_removed) ; true).

% DELETE x: relacion -> se borra con sus propiedades (los nodos no cambian).
% Nodo -> solo si ya no tiene relaciones (como en Cypher); si no, error y hay que usar DETACH DELETE.
% Si otra fila ya lo borro, no hace nada.
% El error se lanza como atomo: MQI solo devuelve el functor de la excepcion, asi llega el mensaje completo.

cy_delete(G, Id) :-
    cy_fact(G, edge, [Id, _, _, _], Edge), call(Edge), !,
    cy_delete_edge(G, Id).
cy_delete(G, Id) :-
    cy_fact(G, node, [Id], Node), call(Node), !,
    (cy_has_edges(G, Id)
     -> format(atom(Message), 'Cannot delete node ~w because it still has relationships (use DETACH DELETE)', [Id]),
        throw(Message)
     ;  cy_delete_node(G, Id)).
cy_delete(_, _).

% DETACH DELETE x: primero las relaciones del nodo y luego el nodo

cy_detach_delete(G, Id) :-
    cy_fact(G, node, [Id], Node), call(Node), !,
    forall(cy_incident_edge(G, Id, EdgeId), cy_delete_edge(G, EdgeId)),
    cy_delete_node(G, Id).
cy_detach_delete(G, Id) :-
    cy_delete(G, Id).

cy_incident_edge(G, Node, EdgeId) :- cy_fact(G, edge, [EdgeId, _, Node, _], Edge), call(Edge).
cy_incident_edge(G, Node, EdgeId) :- cy_fact(G, edge, [EdgeId, _, From, Node], Edge), call(Edge), From \== Node.

cy_has_edges(G, Node) :-
    cy_incident_edge(G, Node, _), !.

cy_delete_edge(G, Id) :-
    cy_fact(G, edge, [Id, _, _, _], Edge),
    retractall(Edge),
    cy_fact(G, prop, [Id, _, _], Props),
    retractall(Props),
    cy_count(relationships_deleted).

cy_delete_node(G, Id) :-
    forall(member(Suffix-Args, [node-[Id], label-[Id, _], prop-[Id, _, _]]),
           ( cy_fact(G, Suffix, Args, Fact), retractall(Fact) )),
    cy_count(nodes_deleted).


%%%%%%%%%%%%%% resumen de cambios
% "1 node(s) created, 2 property(ies) set" en el orden de cy_stat_label/2

cy_stat_label(nodes_created, 'node(s) created').
cy_stat_label(relationships_created, 'relationship(s) created').
cy_stat_label(properties_set, 'property(ies) set').
cy_stat_label(labels_added, 'label(s) added').
cy_stat_label(properties_removed, 'property(ies) removed').
cy_stat_label(labels_removed, 'label(s) removed').
cy_stat_label(relationships_deleted, 'relationship(s) deleted').
cy_stat_label(nodes_deleted, 'node(s) deleted').

cy_reset_stats :-
    retractall(cy_stat(_, _)).

cy_count(Key) :-
    (retract(cy_stat(Key, N)) -> N1 is N + 1 ; N1 = 1),
    assertz(cy_stat(Key, N1)).

cy_stats(Text) :-
    findall(Part,
            ( cy_stat_label(Key, Label), cy_stat(Key, N), format(atom(Part), '~w ~w', [N, Label]) ),
            Parts),
    (Parts = [] -> Text = 'No changes' ; atomic_list_concat(Parts, ', ', Text)).


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
