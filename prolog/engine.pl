:- encoding(utf8).
/*
 Punto de entrada del engine
    Lo carga el broker de Java al levantar SWI-Prolog
        swipl -l prolog/engine.pl mqi --write_connection_values=true
    Tambien sirve en una consola de swipl:  ?- [prolog/engine]
    Solo carga archivos: no define logica propia
*/

% Runtime: comparadores, relaciones y ciclo de queries

:- ensure_loaded(cy_engine).

% Grafos de prueba para agregar uno nuevo: crear graphs/<nombre>.pl y
% agregar aqui la linea ensure_loaded

:- ensure_loaded('graphs/amigos').
:- ensure_loaded('graphs/empresa').
:- ensure_loaded('graphs/movies').