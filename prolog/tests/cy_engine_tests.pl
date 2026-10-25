:- encoding(utf8).
/** Tests plunit del engine
      swipl -g run_tests -t halt prolog/tests/cy_engine_tests.pl */
:- use_module(library(plunit)).
:- ensure_loaded('../engine').

% q(+Head, +Body, +Columnas, -Rows): ciclo completo create/exec/erase
q(Head, Body, Cols, Rows) :-
    cy_create_query(Id, (Head :- Body)),
    cy_exec_query(Id, Cols, Rows),
    cy_erase_query(Id).

filas(Rows, Sorted) :-
    atomic_list_concat([_|L], '\n', Rows), msort(L, Sorted).

:- begin_tests(comparadores).
test(numeros)            :- cy_eq(1, 1.0), cy_lt(1, 2), cy_gt(3, 2), cy_le(2, 2), cy_ge(2.5, 2).
test(texto)              :- cy_eq('Ana', 'Ana'), cy_neq('Ana', 'Luis'), cy_lt('Ana', 'Luis').
test(mezclados_fallan)   :- \+ cy_eq(3, '3'), \+ cy_gt(abc, 3), \+ cy_lt(3, abc), \+ cy_neq(3, abc).
test(no_ligado_falla)    :- \+ cy_eq(_, 1), \+ cy_gt(_, 1).
:- end_tests(comparadores).

:- begin_tests(relaciones).
test(out_in_none, true(Out-In-None == 4-4-7)) :-   % none: e1,e2,e3 en ambos sentidos + e4 una vez
    aggregate_all(count, cy_edge(amigos, out,  _, amigo, _, _), Out),
    aggregate_all(count, cy_edge(amigos, in,   _, amigo, _, _), In),
    aggregate_all(count, cy_edge(amigos, none, _, amigo, _, _), None).
test(in_invierte_extremos) :-
    findall(A-B, cy_edge(amigos, in, e3, amigo, A, B), L), L == [n3-n2].
test(none_self_loop_una_vez) :-
    findall(E, cy_edge(amigos, none, E, amigo, n4, n4), L), L == [e4].
test(tipo_libre) :-
    findall(T, cy_edge(amigos, out, e5, T, _, _), L), L == [conoce].
:- end_tests(relaciones).

:- begin_tests(queries).
test(caso1_match_simple_con_alias_de_columna) :-       % MATCH (m:Movie) RETURN m.title, m.year
    q(r(T, Y),
      ( user_movies_node(M), user_movies_label(M, 'Movie'),
        user_movies_prop(M, title, T), user_movies_prop(M, year, Y) ),
      ['m.title', year], Rows),
    filas(Rows, S), S == ['"Avatar"\t2009', '"Matrix"\t1999'].

test(propiedad_faltante_descarta_fila) :-              % 'Sin Anio' no aparece
    q(r(T), ( user_movies_node(M), user_movies_prop(M, year, _), user_movies_prop(M, title, T) ),
      ['t'], Rows),
    \+ sub_atom(Rows, _, _, _, 'Sin Anio').

test(where_filtra) :-                                  % WHERE m.year > 2000
    q(r(T), ( user_movies_node(M), user_movies_label(M, 'Movie'),
              user_movies_prop(M, year, Y), cy_gt(Y, 2000), user_movies_prop(M, title, T) ),
      ['t'], Rows),
    Rows == 't\n"Avatar"'.

test(multiples_etiquetas) :-                           % (a:Person:Employee)
    q(r(N), ( user_movies_node(A), user_movies_label(A, 'Person'), user_movies_label(A, 'Employee'),
              user_movies_prop(A, name, N) ),
      ['n'], Rows),
    Rows == 'n\n"Keanu"'.

test(sin_resultados_solo_header) :-
    q(r(N), ( user_amigos_prop(_, name, N), cy_eq(N, 'Nadie') ), ['n'], Rows),
    Rows == 'n'.

test(count_en_exec4) :-
    cy_create_query(Id, (r(N) :- user_amigos_prop(_, name, N))),
    cy_exec_query(Id, [n], _, Count), cy_erase_query(Id),
    Count == 6.

test(ciclo_amigos_mutuos) :-        % MATCH (a)-[:amigo]->(b),(b)-[:amigo]->(a) con e1 \= e2
    q(r(NA, NB),
      ( cy_edge(amigos, out, E1, amigo, A, B), cy_edge(amigos, out, E2, amigo, B, A),
        cy_neq(E1, E2),
        user_amigos_prop(A, name, NA), user_amigos_prop(B, name, NB) ),
      ['a.name', 'b.name'], Rows),
    filas(Rows, S), S == ['"Ana"\t"Luis"', '"Luis"\t"Ana"'].

test(join_colegas_spec_p2) :-       % misma empresa, p.nombre <> c.nombre
    q(r(NP, NC, NE),
      ( user_empresa_edge(_, 'TRABAJA_EN', P, E), user_empresa_edge(_, 'TRABAJA_EN', C, E),
        user_empresa_prop(P, nombre, NP), user_empresa_prop(C, nombre, NC),
        cy_neq(NP, NC), user_empresa_prop(E, nombre, NE) ),
      ['p.nombre', 'c.nombre', 'e.nombre'], Rows),
    filas(Rows, S),
    S == ['"Jose"\t"Lucia"\t"UNA"', '"Jose"\t"Marta"\t"UCR"', '"Jose"\t"Pedro"\t"UNA"',
          '"Lucia"\t"Jose"\t"UNA"', '"Lucia"\t"Pedro"\t"UNA"', '"Marta"\t"Jose"\t"UCR"',
          '"Pedro"\t"Jose"\t"UNA"', '"Pedro"\t"Lucia"\t"UNA"'].

test(propiedad_de_relacion) :-      % WHERE r.since < 2000
    q(r(NA), ( cy_edge(amigos, out, E, _, A, _), user_amigos_prop(E, since, S), cy_lt(S, 2000),
               user_amigos_prop(A, name, NA) ),
      ['a.name'], Rows),
    Rows == 'a.name\n"Ana"'.

test(null_si_no_ligado) :-
    q(r(_), true, ['x'], Rows), Rows == 'x\nnull'.

test(id_generado_y_query_inexistente) :-
    cy_create_query(Id, (r :- true)), atom(Id), cy_erase_query(Id),
    \+ cy_exec_query(Id, [], _).

test(set_current_graph) :-
    set_current_graph(empresa), get_current_graph(empresa),
    \+ set_current_graph(no_existe).
:- end_tests(queries).

:- begin_tests(accesores).
test(node_label_prop, [nondet]) :-
    cy_node(amigos, n1), cy_label(amigos, n1, 'Female'), cy_prop(amigos, n1, name, 'Ana').
test(cuenta_nodos, true(N == 6)) :-
    aggregate_all(count, cy_node(amigos, _), N).
test(grafo_inexistente_lanza, [throws(error(existence_error(procedure, _), _))]) :-
    cy_node(no_existe, _).
test(has_labels_multiples, true(L == [n2, n3, n5])) :-          % (a:Person:Male)
    findall(N, cy_has_labels(amigos, N, ['Person', 'Male']), L).
test(has_labels_ninguno) :-
    \+ cy_has_labels(amigos, _, ['Person', 'Robot']).
test(prop_opt_presente, [nondet, true(V == 28)]) :-
    cy_prop_opt(amigos, n1, age, V).
test(prop_opt_ausente_queda_null) :-         % n5 (David) no tiene age -> null, no se descarta
    q(r(N, Age), ( cy_node(amigos, X), cy_prop(amigos, X, name, N), cy_eq(N, 'David'),
                   cy_prop_opt(amigos, X, age, Age) ),
      [name, age], Rows),
    Rows == 'name\tage\n"David"\tnull'.
:- end_tests(accesores).

:- begin_tests(errores_y_limpieza).
test(error_en_query_se_propaga, [throws(error(type_error(evaluable, _), _))]) :-
    cy_create_query(Id, (r(X) :- X is foo + 1)),
    cy_exec_query(Id, [x], _).
test(create_descarta_queries_viejos) :-
    cy_create_query(Id1, (r :- true)),
    cy_create_query(Id2, (r :- true)),
    Id1 \== Id2,
    \+ cy_query_def(Id1, _, _),
    cy_erase_query(Id2).
test(erase_inexistente_no_falla) :-
    cy_erase_query(no_existe_nunca).
test(id_explicito) :-
    cy_create_query(mi_query, (r(X) :- X = 1)),
    cy_exec_query(mi_query, [x], Rows), Rows == 'x\n1', cy_erase_query(mi_query).
:- end_tests(errores_y_limpieza).