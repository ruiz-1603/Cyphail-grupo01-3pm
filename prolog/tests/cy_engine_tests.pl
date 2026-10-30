/*
 * Pruebas unitarias del engine Prolog (estilo visto en clase)
 */

:- ensure_loaded('../engine').

run_query(Head, Body, Cols, Rows) :-
    cy_create_query(Id, (Head :- Body)),
    cy_exec_query(Id, Cols, Rows),
    cy_erase_query(Id).

test_1 :-
    writeln('*** Test 1: Comparadores y grafo activo ***'),
    set_current_graph(amigos),
    get_current_graph(G),
    format('Grafo actual: ~w~n', [G]),
    cy_eq(1, 1.0),
    cy_eq('Ana', 'Ana'),
    cy_neq('Ana', 'Luis'),
    cy_lt(1, 2),
    cy_gt(3, 2),
    cy_le(2, 2),
    cy_ge(2.5, 2),
    writeln('*** Test 1 OK ***'), nl.

test_2 :-
    writeln('*** Test 2: Query MATCH (m:Movie) WHERE m.year > 2000 RETURN m.title ***'),
    run_query(
        cy_row(T),
        (user_movies_node(M), user_movies_label(M, 'Movie'),
         user_movies_prop(M, year, Y), cy_gt(Y, 2000),
         user_movies_prop(M, title, T)),
        ['m.title'],
        Rows
    ),
    writeln(Rows),
    Rows = 'm.title\nAvatar',
    writeln('*** Test 2 OK ***'), nl.

test_3 :-
    writeln('*** Test 3: Query amigos mutuos (ejemplo SPEC P2) ***'),
    run_query(
        cy_row(NA, NB),
        (user_amigos_edge(E1, amigo, A, B),
         user_amigos_edge(E2, amigo, B, A),
         cy_neq(E1, E2),
         user_amigos_prop(A, name, NA),
         user_amigos_prop(B, name, NB)),
        ['a.name', 'b.name'],
        Rows
    ),
    writeln(Rows),
    writeln('*** Test 3 OK ***'), nl.

test_4 :-
    writeln('*** Test 4: Query colegas misma empresa (ejemplo SPEC P2) ***'),
    run_query(
        cy_row(NP, NC, NE),
        (user_empresa_edge(_, 'TRABAJA_EN', P, E),
         user_empresa_edge(_, 'TRABAJA_EN', C, E),
         user_empresa_prop(P, nombre, NP),
         user_empresa_prop(C, nombre, NC),
         cy_neq(NP, NC),
         user_empresa_prop(E, nombre, NE)),
        ['p.nombre', 'c.nombre', 'e.nombre'],
        Rows
    ),
    writeln(Rows),
    writeln('*** Test 4 OK ***'), nl.

run_tests :-
    test_1,
    test_2,
    test_3,
    test_4.
