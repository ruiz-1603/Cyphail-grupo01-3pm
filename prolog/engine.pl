/*
 * Punto de entrada del engine
 * Lo carga el broker de Java al levantar SWI-Prolog:
 *     swipl -l prolog/engine.pl mqi --write_connection_values=true
 * Tambien sirve en una consola de swipl: ?- [prolog/engine].
 */

:- ensure_loaded(cy_engine).

:- ensure_loaded('graphs/amigos').
:- ensure_loaded('graphs/empresa').
:- ensure_loaded('graphs/movies').
