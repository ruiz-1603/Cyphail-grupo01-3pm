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

test_5 :-
    writeln('*** Test 5: Relacion con propiedad (CASO 4 P2) ***'),
    % MATCH (a:Persona)-[r:CONOCE {desde: 2015}]->(b:Persona) RETURN a.nombre, b.nombre, r.desde
    run_query(
        cy_row(NA, NB, D),
        (user_empresa_edge(R, 'CONOCE', A, B),
         user_empresa_label(A, 'Persona'),
         user_empresa_label(B, 'Persona'),
         user_empresa_prop(R, desde, D), cy_eq(D, 2015),
         user_empresa_prop(A, nombre, NA),
         user_empresa_prop(B, nombre, NB)),
        ['a.nombre', 'b.nombre', 'r.desde'],
        Rows
    ),
    writeln(Rows),
    Rows = 'a.nombre\tb.nombre\tr.desde\nMarta\tJose\t2015',
    writeln('*** Test 5 OK ***'), nl.

test_6 :-
    writeln('*** Test 6: Direccion inversa <- (CASO 3 P2) ***'),
    % MATCH (e:Empresa {nombre: 'Acme'})<-[r:TRABAJA_EN]-(p:Persona) RETURN p.nombre, r
    run_query(
        cy_row(NP, R),
        (user_empresa_edge(R, 'TRABAJA_EN', P, E),
         user_empresa_label(E, 'Empresa'),
         user_empresa_prop(E, nombre, NE), cy_eq(NE, 'Acme'),
         user_empresa_label(P, 'Persona'),
         user_empresa_prop(P, nombre, NP)),
        ['p.nombre', 'r'],
        Rows
    ),
    writeln(Rows),
    Rows = 'p.nombre\tr\nLucia\tr6',
    writeln('*** Test 6 OK ***'), nl.

test_7 :-
    writeln('*** Test 7: WHERE con OR / NOT; la fila sale una sola vez (CASO 10 P2) ***'),
    % (t.desde >= 2018 OR s.porcentaje > 25.5) AND NOT k.desde = 2020
    run_query(
        cy_row(NP, NQ, NE),
        (user_empresa_edge(T, 'TRABAJA_EN', P, E),
         user_empresa_prop(P, activo, Act), cy_eq(Act, true),
         user_empresa_prop(T, cargo, C), cy_eq(C, 'Dev'),
         user_empresa_edge(S, 'SOCIO_DE', Q, E),
         (user_empresa_edge(K, 'CONOCE', P, Q) ; user_empresa_edge(K, 'CONOCE', Q, P)),
         cy_neq(T, S), cy_neq(T, K), cy_neq(S, K),
         once((user_empresa_prop(T, desde, TD), cy_ge(TD, 2018)
             ; user_empresa_prop(S, porcentaje, SP), cy_gt(SP, 25.5))),
         \+ (user_empresa_prop(K, desde, KD), cy_eq(KD, 2020)),
         user_empresa_prop(P, nombre, NP),
         user_empresa_prop(Q, nombre, NQ),
         user_empresa_prop(E, nombre, NE)),
        ['p.nombre', 'q.nombre', 'e.nombre'],
        Rows
    ),
    writeln(Rows),
    Rows = 'p.nombre\tq.nombre\te.nombre\nMarta\tJose\tUCR',
    writeln('*** Test 7 OK ***'), nl.

test_8 :-
    writeln('*** Test 8: Patron sin resultados ***'),
    run_query(
        cy_row(NA),
        (user_empresa_edge(_, 'CONOCE', A, _),
         user_empresa_prop(A, nombre, NA), cy_eq(NA, 'Nadie')),
        ['a.nombre'],
        Rows
    ),
    writeln(Rows),
    Rows = 'a.nombre',
    writeln('*** Test 8 OK ***'), nl.

test_9 :-
    writeln('*** Test 9: .use vuelve a cargar el grafo desde su archivo ***'),
    set_current_graph(empresa),
    assert(user_empresa_node(temporal)),
    set_current_graph(empresa),
    \+ user_empresa_node(temporal),
    writeln('El nodo agregado en memoria ya no esta: el grafo quedo como en disco'),
    writeln('*** Test 9 OK ***'), nl.

test_10 :-
    writeln('*** Test 10: cada query tiene su propio id (cy_create_query) ***'),
    cy_create_query(Q1, (cy_row(a) :- true)),
    cy_create_query(Q2, (cy_row(b) :- true)),
    format('Ids: ~w ~w~n', [Q1, Q2]),
    Q1 \== Q2,
    cy_exec_query(Q1, [x], Rows1),
    Rows1 = 'x\na',
    cy_erase_query(Q1),
    cy_exec_query(Q2, [x], Rows2),
    Rows2 = 'x\nb',
    cy_erase_query(Q2),
    \+ cy_query(_, _),
    writeln('*** Test 10 OK ***'), nl.

test_11 :-
    writeln('*** Test 11: valor con espacios = una sola columna (CASO 1 P1) ***'),
    set_current_graph(p1),
    run_query(
        cy_row(T, Y),
        (user_p1_node(M), user_p1_label(M, 'Movie'),
         user_p1_prop(M, title, T), cy_eq(T, 'The Matrix'),
         user_p1_prop(M, year, Y)),
        ['m.title', year],
        Rows
    ),
    writeln(Rows),
    Rows = 'm.title\tyear\nThe Matrix\t1999',
    writeln('*** Test 11 OK ***'), nl.

run_tests :-
    test_1,
    test_2,
    test_3,
    test_4,
    test_5,
    test_6,
    test_7,
    test_8,
    test_9,
    test_10,
    test_11.
