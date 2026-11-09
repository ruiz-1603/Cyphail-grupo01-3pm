# Cyphail - Graph Query Processing Prototype

> A graph query processing prototype inspired by Cypher (Neo4j), built as part of the Programming Paradigms course at Universidad Nacional de Costa Rica.

## Project Overview

**Cyphail** is an interactive graph query processor designed to explore and apply three core programming paradigms through progressive practical implementation:

- **Object-Oriented Programming (OOP)** — Modular software architecture, Command Pattern (`CommandRegistry`), and domain modeling.
- **Functional Programming (FP)** — Hand-written parser combinators (`Or`, `Seq`, `Star`, `TFlat`, `RFlat`), immutable AST structures with Java Records, sealed types and pattern matching.
- **Logic Programming (LP)** — Query execution in SWI-Prolog: the query is transpiled to Prolog goals that the engine resolves by unification and backtracking.

This is **Sprint P2 (Octubre–Noviembre 2026)**. The full flow is:

1. `Repl` reads a statement (multiline, ending with `;`) or runs a `.cyphail` script with `.load`.
2. `CyphailParser` builds the AST, now with **relationship patterns** (`-[r:TYPE {k: v}]->`, `<-[...]-`, `-[...]-`), `AND`/`OR`/`NOT`, decimals, booleans, `CREATE` without `MATCH`, `SET` and `REMOVE`.
3. `VariableValidator` checks static semantics (P1 rules + P2 rules on relationships, labels and the `__` prefix).
4. `CodeGen` transpiles the AST into Prolog goals and keeps the variable mapping (`SymbolTable`).
5. `PrologEngine` (broker) starts SWI-Prolog, keeps an **MQI** socket session and sends the goal.
6. The engine (`prolog/cy_engine.pl` + `prolog/graphs/*.pl`) runs the query and answers with JSON, parsed with Gson.
7. `Repl` shows the table.

The Sprint P1 fake engine (`FakePrologEngine`, JSON data) was removed, as required by the P2 SPEC.

**Course:** EIF400-II-2026: Paradigmas de Programación  
**Institution:** Escuela de Informática — Universidad Nacional de Costa Rica  
**Professor:** Dr. Carlos Loría-Sáenz  
**Date:** Noviembre 2026  

---

## Team Members (Grupo 01 - 3:00 PM)

| Name | Student ID |
|------|------------|
| Priscilla Murillo Romero | 402640465 |
| Aaron Ruiz Medina | 118060056 |
| Samael Sánchez Mora | 119470968 |
| Daniel Villarroel Abaduca | A00150150 |
| Nicolás Zárate Hernández | 402320641 |

---

## Prerequisites

- **Java Development Kit (JDK):** 26
- **Apache Maven:** 3.8.1 or higher
- **SWI-Prolog:** 9.x or higher, with the `swipl` command available in the `PATH` (it includes the `mqi` library)

### Environment Verification

```bash
java --version
mvn --version
swipl --version
```

---

## Getting Started

### Build the Project

From the project root directory, build and package the application using Maven:

```bash
mvn clean package
```

This will:
- Compile main and test sources.
- Execute the complete test suite (153 JUnit 5 tests: parser, AST, validator, CodeGen, MQI, REPL and an end-to-end test that starts SWI-Prolog).
- Generate an executable Uber/Shaded JAR containing all dependencies at:
  ```
  target/cyphail-1.0-SNAPSHOT.jar
  ```

The Prolog engine has its own tests:

```bash
cd prolog/tests
swipl -g run_tests -t halt cy_engine_tests.pl
```

It runs 11 tests (`test_1` … `test_11`, the style seen in class), all of them must print `OK`.

### Run the Application

It can be started **from any folder**: the broker finds the project root from the location of the jar (`target/`) and starts `swipl -l prolog/engine.pl mqi` there. Relative paths given to `.load` are resolved from the folder where cyphail was started.

#### Option 1: Direct Java Execution (Cross-platform)
```bash
java -jar target/cyphail-1.0-SNAPSHOT.jar repl
```

#### Option 2: Script Execution
- **Windows:**
  ```cmd
  cyphail.bat repl
  ```
- **macOS / Linux:**
  ```bash
  ./cyphail repl
  ```

---

## Usage Guide & REPL Commands

### System Commands

All system commands begin with a dot (`.`):

| Command | Description |
|---------|-------------|
| `.help` | Display available commands and operational instructions |
| `.about` | Show project metadata, version, and author information |
| `.exit` / `.quit` | Terminate the interactive REPL session (also closes SWI-Prolog) |
| `.use` | List the graphs available in the Prolog engine (`cy_db/1`) |
| `.use <graph_name>` | Select the current graph and **reload it** from `prolog/graphs/<graph_name>.pl` (e.g., `.use empresa`, `.use p1`) |
| `.tree <query>` | Parse a query and display its AST in pre-order notation |
| `.load "<path>"` | Read a `.cyphail` script, then parse and execute each statement (e.g., `.load "data/casos_p2.cyphail"`, `.load "data/casos_p1.cyphail"`) |

### Statements

A statement can span several lines (the REPL shows `...>` while it waits for more). The `;` is **optional** (P2 review sheet): a statement ends with `;`, with a blank line, or when a line starting with a dot-command (`.use`, `.tree`, ...) is typed. The same rules apply inside `.load` scripts, where `//` comment lines are ignored.

```cypher
cyphail> .use empresa
cyphail> MATCH (a:Persona)-[r:CONOCE]->(b:Persona)
   ...> WHERE r.desde > 2010 AND b.edad >= 30
   ...> RETURN a.nombre, b.nombre, r.desde;

a.nombre  b.nombre  r.desde
--------  --------  -------
Marta     Jose      2015
Pedro     Jose      2020

(2 row(s))
```

### AST Inspection with `.tree`

The `.tree` command invokes `CyphailParser`, builds the internal AST and prints it with `AstPrinter`:

```cypher
cyphail> .tree MATCH (m:Movie) WHERE m.year > 1990 RETURN m.title AS title, m.year AS year
```

**Output:**
```
Query{
  Match: {
    Patterns: [
      PatternNode: {
        var: m
        labels: [ Movie ]
        properties: []
      }
    ]
  }
  Where: {
    Expr: (> (. m year) 1990)
  }
  Updates: []
  Return: {
    Projection: {
      Items: [
        {as (. m title) title}
        {as (. m year) year}
      ]
      Modifiers: []
    }
  }
}
```

If the query has a static semantic error (for example an undefined variable, cases C10/C11), `.tree` **still shows the tree** and then the error. A syntax error has no tree, so only the error is shown:
```
cyphail> .tree MATCH (p:Person) WHERE q.age > 60 RETURN q AS name;
Query{
  ...
}
ERROR: Variable 'q' is not defined in clause WHERE
```

---

## Prolog Engine and Graphs (`prolog/`)

Each graph `G` is a set of Prolog facts, as if it had been created from the REPL:

| Fact | Meaning |
|------|---------|
| `cy_db(G)` | the graph exists (this is what `.use` lists) |
| `user_G_node(Id)` | node |
| `user_G_label(NodeId, Label)` | node label |
| `user_G_edge(EId, Type, From, To)` | directed relationship |
| `user_G_prop(NodeOrEdgeId, Key, Value)` | property of a node or a relationship |

Graphs: `prolog/graphs/amigos.pl`, `prolog/graphs/empresa.pl` (used by the P2 reference cases: `TRABAJA_EN`, `CONOCE`, `SOCIO_DE`), `prolog/graphs/movies.pl`, `prolog/graphs/p1.pl` (the data of the Sprint P1 JSON files: `Movie`, `Book`, `Person`, `Order`, `Client`, used by the P1 cases) and `prolog/graphs/revision.pl` (`Student`-`attends`->`Course`, `Product`, `Autor`/`Libro` and `Person:Female`-`KNOWS`->`Person:Male`, used by cases C11 and C12 of the P2 review sheet and by the update examples of SPEC P2 II.4). To add a graph, create `prolog/graphs/g.pl`, declare `cy_db(g)` in `cy_engine.pl` and load it in `engine.pl`.

`.use G` always **reloads** the graph (`set_current_graph/1` -> `cy_load_graph/1`): it first removes the `user_G_*` facts in memory (`retractall`) and then consults `graphs/G.pl` again, so the graph is always exactly what the file says.

The runtime (`cy_engine.pl`) provides the comparators `cy_eq cy_neq cy_lt cy_gt cy_le cy_ge`, the query cycle `cy_create_query/2 → cy_exec_query/3 → cy_erase_query/1` and the table formatting (`cy_format_table`).

It also provides the **updates** used by `CREATE`, `SET`, `REMOVE` and `DELETE` (they `assertz`/`retract` the `user_G_*` facts, which are `dynamic`):

| Predicate | Used by |
|-----------|---------|
| `cy_new_node(G, Id)`, `cy_new_edge(G, Id, Type, From, To)` | `CREATE` (new ids `cy_n1`, `cy_r1`, ...) |
| `cy_set_prop(G, Id, Key, Value)`, `cy_add_label(G, Id, Label)` | `CREATE {k: v}`, `SET x.k = v`, `SET x:L` |
| `cy_remove_prop(G, Id, Key)`, `cy_remove_label(G, Id, Label)` | `REMOVE x.k`, `REMOVE x:L` |
| `cy_delete(G, Id)`, `cy_detach_delete(G, Id)` | `DELETE x` (a node with relationships is an error), `DETACH DELETE x` |
| `cy_reset_stats/0`, `cy_stats(Text)` | summary of changes shown by the REPL (`1 node(s) created, 2 property(ies) set`) |

Changes live **in memory only**: `.use G` reloads the graph from its file, so a demo can always start again from the same data.

---

## Test Cases (Sprint P1: C1 to C12)

"Todo P1 debe funcionar" (P2 review sheet): the twelve P1 cases run on the `p1` graph with `.load "data/casos_p1.cyphail"`. Cases 1-9 are executed (for example C2 -> `The Great Gatsby 180`, C3 -> `Priscilla 35`, C8 -> `Priscilla 50000`); C7 creates one `Certificate` per adult (4 nodes) and C9 archives Samael's cancelled order and deletes it (after each one the script shows the change and reloads the graph with `.use p1`); cases 10-12 show the tree and the semantic error.

The parser and AST validator support the complete suite of required test cases:

| Case | Category | Key Characteristics | Status |
|------|----------|---------------------|--------|
| **C1** | Projections & Aliases | Single pattern, multiple projections with `AS` | ✅ Verified |
| **C2** | WHERE Comparisons | Single pattern, `<` comparison, dot property access | ✅ Verified |
| **C3** | Multiple Labels | Multi-label node `(a:Person:Employee {id: 1})`, properties | ✅ Verified |
| **C4** | Disconnected Patterns | Multiple MATCH patterns `(m:Movie), (p:Person)` | ✅ Verified |
| **C5** | Cross-Pattern Attributes | Comparison between different node attributes `m.year <> p.age` | ✅ Verified |
| **C6** | Patterns with Literals | Patterns with property literals `{year: 1999}, {age: 40}` | ✅ Verified |
| **C7** | CREATE Updates | MATCH combined with `CREATE (c:Certificate ...)` | ✅ Verified |
| **C8** | Simple Property Reference | MATCH with referenced property `{personId: p.id}` | ✅ Verified |
| **C9** | Full Lifecycle | `MATCH`, `WHERE`, `CREATE`, `DELETE`, and `RETURN` combined | ✅ Verified |
| **C10** | Undefined Variable Detection | Variable `q` in WHERE/RETURN not present in MATCH | ✅ Detects error |
| **C11** | Out-of-Order Variable Reference | `p.id` used in first pattern before `(p:Person)` declared | ✅ Detects error |
| **C12** | **"Sorpresas" (Team Custom Case)** | Multi-label VIP promotion, `>=` operator, CREATE referencing MATCH, DELETE, and RETURN projecting newly created variables | ✅ Verified |

### Details on CASO 12 ("Sorpresas")

Designed specifically to demonstrate advanced parser and semantic capabilities not covered in C1–C11:
```cypher
MATCH (c:Client:VIP {status: "active"})
WHERE c.rating >= 90
CREATE (b:Badge:Gold {clientId: c.id, level: "elite", year: 2026})
DELETE c
RETURN b.clientId AS id, b.level AS badgeLevel
```

**Technical Highlights of C12:**
1. **Multi-label matching with property filter:** `(c:Client:VIP {status: "active"})`.
2. **Comparison operator `>=`:** Validates operator precedence and non-strict comparison.
3. **Multi-label node creation with property binding:** `CREATE (b:Badge:Gold {clientId: c.id, ...})`.
4. **Deletion of source node:** `DELETE c`.
5. **Dynamic Scope Expansion:** `RETURN` projects properties of `b`, proving that `VariableValidator` correctly expands its scope with variables declared in `CREATE` clauses.

---

## Test Cases (Sprint P2: reference cases)

Cases from `EIF400-II-2026-Cypahil-P2-10-Casos_Prueba_Unitarios.md`, executed on the `empresa` graph (C11 and C12 on the `revision` graph). All of them can be run with `.load "data/casos_p2.cyphail"`; the exact ASTs are checked in `CyphailParserP2Test`. After each case that changes the graph, the script shows the change with a `MATCH` and reloads the graph with `.use`, so the following cases always see the same data. C7 uses Marta and Pedro instead of Ana and Luis, who are not in `empresa` (review sheet: *"su caso se ajusta al ejemplo de grafo ya disponible en el engine"*).

| Case | What it exercises | Status |
|------|-------------------|--------|
| **1** | `-[r]->` without type, `RETURN r` (relationship id) | ✅ Parsed and executed |
| **2** | `-[:TYPE]->` without variable | ✅ Parsed and executed |
| **3** | Reverse direction `<-[r:T]-` and undirected `-[r:T]-` (`IN`, `OUT`, `NONE` in the AST) | ✅ Parsed and executed |
| **4** | Relationship properties `{desde: 2015}` and `r.desde` in RETURN | ✅ Parsed and executed |
| **5** | WHERE on relationship and node properties with `AND` | ✅ Parsed and executed |
| **6** | Inline properties + WHERE with `OR` / `NOT`, strings with `'...'` | ✅ Parsed and executed |
| **7** | MATCH with two patterns + `CREATE` reusing variables | ✅ Executed: `1 relationship(s) created, 2 property(ies) set` |
| **8** | `CREATE` without MATCH, boolean `true`, `RETURN a, r, e` | ✅ Executed: 2 nodes + 1 relationship, returns their new ids |
| **9** | `DELETE r` of a relationship with WHERE | ✅ Executed: `1 relationship(s) deleted` (Lucia → Marta, 1998) |
| **10** | Three patterns with shared variables, parentheses, `AND`/`OR`/`NOT`, decimals | ✅ Parsed and executed |
| **8b** | Two patterns sharing `e` (join variable without repeating the label) | ✅ Parsed and executed |
| **8c** | Disconnected patterns (cartesian product; one edge is never used twice) | ✅ Parsed and executed |
| **11** (review sheet) | Multiple `SET` items (`SET r.grade = 9.5, u.activo = true, ...`) | ✅ Executed on `revision`: `3 property(ies) set` |
| **12** (review sheet) | Multiple `REMOVE` items (`REMOVE p.discount, p.promo, p:Oferta`) | ✅ Executed on `revision`: `2 property(ies) removed, 1 label(s) removed` |

**SPEC P2 examples:** `.load "data/spec_p2.cyphail"` runs every example of the SPEC: the relationship pattern of II.3 (`empresa`), the transpiler example (`amigos`) and the six update examples of II.4 (`revision`: `CREATE (a)-[:ESCRIBIO]->(b)` between Bill Gates and his book, `CREATE` without `MATCH`, `DELETE r` of the `KNOWS` edge of 1995, `SET p.precio = 19.99`, `REMOVE p.discount` and `SET r.grade = 9.5`).

**Validations defined by the team** (the reference document asks to fix them with a test):
- **Repeated label:** a node variable reused with *different* labels (`(e:Empresa)` … `(e:Ciudad)`) is an error; reusing it without a label (`(e)`) is valid (`VariableValidatorTest`).
- **Relationship uniqueness:** the same relationship variable cannot appear twice (validator, SPEC II.6); different relationships of the same `MATCH` must match different edges (`cy_neq(__E1, __E2)`, as in the SPEC example).

---

## CodeGen: Cyphail → Prolog Transpiler (Sprint P2)

The `codegen` package transpiles a validated AST into Prolog goals that the SWI-Prolog engine (`prolog/cy_engine.pl`) executes through MQI. `CodeGen` is an AST visitor written with sealed types and pattern-matching `switch`; it threads an immutable state (symbol table + emitted goals) from one method to the next.

```java
GeneratedQuery gen = CodeGen.generate(query, currentGraph);  // currentGraph: the one selected with .use
gen.goal();     // Prolog goal sent through MQI (no final period)
gen.columns();  // RETURN column names, in order (alias or query text)
gen.symbols();  // SymbolTable: Cyphail variable -> Prolog variable
```

### Generated goal

The SPEC example (graph `amigos`):

```text
MATCH (a)-[:amigo]->(b), (b)-[:amigo]->(a)
WHERE a.age > 18 AND a.age = b.age
RETURN a.name, b.name
```
```prolog
cy_create_query(__QueryId, (cy_row(__A_name, __B_name) :-
    user_amigos_edge(__E1, amigo, __A, __B), user_amigos_edge(__E2, amigo, __B, __A), cy_neq(__E1, __E2),
    user_amigos_prop(__A, age, __A_age), cy_gt(__A_age, 18),
    user_amigos_prop(__B, age, __B_age), cy_eq(__A_age, __B_age),
    user_amigos_prop(__A, name, __A_name), user_amigos_prop(__B, name, __B_name))),
cy_exec_query(__QueryId, ['a.name', 'b.name'], Rows), cy_erase_query(__QueryId)
```

The engine binds `Rows` to the table as text (`header\nrow1\nrow2...`, columns separated by a tab, so a value with spaces such as `'The Matrix'` stays in one column); with no results it holds only the header. The broker extracts that binding from the MQI JSON response.

`__QueryId` arrives unbound: `cy_create_query/2` gives it a unique id with `gensym` (`cy_q1`, `cy_q2`, ...) and stores the clause as `cy_query(Id, Head) :- Body`; `cy_exec_query/3` and `cy_erase_query/1` only use the clause of that id.

### Translation rules

| Cyphail | Prolog |
|---------|--------|
| `(a:L1:L2)` alone | `user_G_node(__A), user_G_label(__A, 'L1'), user_G_label(__A, 'L2')` |
| `(a:L)` again (join variable) | only `user_G_label(__A, 'L')` |
| `(a)-[r:T]->(b)` | `user_G_edge(__R, 'T', __A, __B)` (the edge binds both nodes) |
| `(a)<-[r:T]-(b)` | `user_G_edge(__R, 'T', __B, __A)` |
| `(a)-[r:T]-(b)` | `(user_G_edge(__R, 'T', __A, __B) ; user_G_edge(__R, 'T', __B, __A))` |
| `-[r]->` (no type) / `-[:T]->` (no variable) | type `_` / fresh variable `__E1` (anonymous node: `__N1`) |
| two relationships in one MATCH | `cy_neq(__E1, __E2)` |
| `{name: "Ana"}` (node or relationship) | `user_G_prop(__A, name, __A_name), cy_eq(__A_name, 'Ana')` |
| `a.age > 18` | `user_G_prop(__A, age, __A_age), cy_gt(__A_age, 18)` |
| `= <> < > <= >=` | `cy_eq cy_neq cy_lt cy_gt cy_le cy_ge` |
| `x AND y` / `x OR y` / `NOT x` | `x, y` / `once((x ; y))` / `\+ (x)` |
| `RETURN a.name AS nombre` | argument `__A_name` of `cy_row`, column `nombre` |
| `RETURN a` / `RETURN r` | argument `__A` / `__R`: only the node or relationship id (P2 restriction) |
| `CREATE (a:L {k: v})` (new `a`) | `cy_new_node(G, __A), cy_add_label(G, __A, 'L'), cy_set_prop(G, __A, k, v)` |
| `CREATE (a)-[r:T {k: v}]->(b)` | `a`/`b` reused if already declared, then `cy_new_edge(G, __R, 'T', __A, __B), cy_set_prop(G, __R, k, v)` |
| `SET x.k = v` / `SET x:L` | `cy_set_prop(G, __X, k, v)` / `cy_add_label(G, __X, 'L')` |
| `REMOVE x.k` / `REMOVE x:L` | `cy_remove_prop(G, __X, k)` / `cy_remove_label(G, __X, 'L')` |
| `DELETE x` / `DETACH DELETE x` | `cy_delete(G, __X)` / `cy_detach_delete(G, __X)` |

`G` is the current graph. A property is fetched only once even if it is used several times; a row whose property is missing is discarded. Each `OR` branch fetches the properties it uses, so `a OR b` holds when only one side has the property; `once` keeps a row from appearing twice when both sides hold.

**Known limitation (null, three-valued logic):** `NOT x.p = v` also holds when `x` has no property `p` (negation as failure), while Cypher would give `null`.

### Variable mapping

- Cyphail variables become Prolog variables with the `__` prefix and the first letter in uppercase: `a` → `__A`, `persona` → `__Persona`. That is why a Cyphail variable cannot start with `__` (SPEC II.7).
- Property computations get their own variable: `a.name` → `__A_name`.
- `SymbolTable` keeps these associations to build the final table in Java, and resolves collisions with a `_N` suffix (`a` and `A` → `__A`, `__A_2`). `__QueryId`, `Rows` and `Stats` are reserved.

### Updates (CREATE, SET, REMOVE, DELETE)

The update goals go **after** the `MATCH`/`WHERE` goals and **before** the `RETURN` ones, in the order of the query. Since the engine collects every solution, each update runs **once per row** of the `MATCH`, as in Cypher (C7 of P1 creates one `Certificate` per adult). A query without `MATCH` (`CREATE ... RETURN ...`) runs once.

- After `SET x.k` or `REMOVE x.k` the old value of `x.k` is forgotten in the symbol table, so a later `RETURN x.k` reads the new value (`__X_k_2`).
- A query with updates is wrapped as `cy_reset_stats, ..., cy_stats(Stats)`; the REPL shows the summary after the table. Without `RETURN` there is no table: `cy_run_query/1` only runs the rows and the summary is shown.
- `CREATE` follows Cypher: a relationship needs exactly one type and a direction; a variable already declared is reused but cannot get new labels or properties (use `SET`).
- `DELETE` of a node that still has relationships fails (`Cannot delete node ... use DETACH DELETE`); deleting a relationship never touches its nodes.
- **Known limitation:** there are no transactions; if a `DELETE` fails on a later row, the changes of the previous rows stay (`.use G` restores the graph).

Out of the sprint (per the reference cases document): chained patterns (more than one relationship in the same pattern), variable length `*1..3`, alternative types `:A|B`, `DETACH DELETE`. The parser does not accept `DETACH DELETE`, but `CodeGen` (`DeleteClause(true, ...)`) and the engine (`cy_detach_delete/2`) already support it.

---

## Project Structure

```
Cyphail-grupo01-3pm/
├── pom.xml                                   # Maven configuration (Java 26, Shade, JUnit 5, Gson)
├── README.md                                 # Official project documentation
├── cyphail.bat                               # Windows execution script
├── cyphail                                   # Unix execution script
├── data/                                     # .cyphail scripts for .load
│   ├── casos_p1.cyphail                      # P1 cases (graph p1)
│   ├── casos_p2.cyphail                      # P2 reference cases
│   ├── spec_p2.cyphail                       # examples of the SPEC P2 (II.3, II.4, transpiler)
│   └── sample_queries.cyphail
├── prolog/                                   # SWI-Prolog engine
│   ├── engine.pl                             # Entry point loaded by the broker
│   ├── cy_engine.pl                          # Runtime: comparators, query cycle, updates, table format
│   ├── graphs/ (amigos.pl, empresa.pl, movies.pl, p1.pl, revision.pl)
│   └── tests/cy_engine_tests.pl
└── src/
    ├── main/java/org/cyphail/
    │   ├── Main.java                         # Application entrypoint
    │   ├── ast/                              # AST model (records, sealed interfaces) & AstPrinter
    │   │   ├── Query.java / MatchClause.java / WhereClause.java / ReturnClause.java
    │   │   ├── PatternElement.java / PatternNode.java / PathPattern.java / RelationshipPattern.java / Direction.java
    │   │   ├── UpdateClause.java / CreateClause.java / DeleteClause.java / SetClause.java / RemoveClause.java ...
    │   │   ├── Expr.java / Binary.java / And.java / Or.java / Not.java / PropertyAccess.java / Var.java
    │   │   ├── NumberLit.java / DecimalLit.java / StringLit.java / BoolLit.java
    │   │   ├── ExprText.java                     # Cyphail text of an expression (columns, errors)
    │   │   └── AstPrinter.java
    │   ├── codegen/                          # Cyphail -> Prolog transpiler
    │   │   ├── CodeGen.java / GeneratedQuery.java
    │   │   ├── SymbolTable.java / PrologSyntax.java
    │   │   └── CodeGenException.java
    │   ├── cli/                              # Picocli command bindings
    │   ├── engine/                           # Engine interface and PrologEngine (MQI broker)
    │   ├── mqi/                              # MQI client and JSON response parser (adapted from the professor's demo)
    │   ├── parser/                           # Functional combinator parser
    │   │   ├── CyphailParser.java
    │   │   ├── combinators/Parsers.java
    │   │   ├── core/ (Parser, Result, Ok, Fail, InputString, TokenString, Lexer)
    │   │   └── lexer/Lexers.java
    │   ├── repl/                             # Interactive REPL & Command Pattern
    │   ├── util/                             # Table formatting, I/O helpers, Statements (trailing ';')
    │   └── validator/                        # Static semantic validation (VariableValidator, VariableTable)
    └── test/java/org/cyphail/
        ├── ast/ (AstPrinterP2Test, P2Asts)
        ├── codegen/ (CodeGenTest, CodeGenP2Test, PrologSyntaxTest)
        ├── mqi/MqiResponseParserTest.java
        ├── parser/ (CyphailParserTest, CyphailParserP1Test, CyphailParserP2Test, AstPrinterTest, ...)
        ├── repl/ (PrologEngineAndReplP2Test, TreeCommandTest)
        └── validator/VariableValidatorTest.java
```

---

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 26 | Core programming language (Records, Pattern Matching, Sealed Types) |
| **Maven** | 3.8.1+ | Build lifecycle, dependency management, and Uber JAR packaging |
| **SWI-Prolog** | 9.x+ | Query engine, reached through MQI (Machine Query Interface) |
| **picocli** | 4.7.6 | Command line interface parsing |
| **Gson** | 2.11.0 | Parsing of the MQI JSON responses |
| **JUnit Jupiter** | 5.10.2 | Unit testing framework (scope: test) |
| **Maven Shade Plugin** | 3.5.1 | Packaging self-contained executable JAR |

---

## AI Usage Declaration

This project was developed with assistance from generative AI tools adhering strictly to course integrity guidelines:

- **Assisting AI:** Claude AI (`claude-sonnet-4`)
- **Nature of Assistance:** Conceptual explanations of monadic parser combinators in Java, design patterns (Command Pattern), AST structure guidance, and debugging assistance. No automated black-box code generators were used.
- **Representative Prompts:**
  * *"Actúa como un profesor y tutor experto en Programación Funcional en Java. El objetivo de este sprint es construir a mano un Parser de combinadores para analizar queries de tipo Cypher. Explícame el concepto de Result como sum type (Ok/Fail) y cómo encadenar combinadores de frase."*
  * *"Guíame paso a paso para estructurar un Visitor/Printer para un AST representado con Java Records, de modo que imprima expresiones binarias en formato prefijo pre-order y mantenga validación estricta de variables no definidas."*

---

## References

- [Neo4j Cypher Manual](https://neo4j.com/docs/cypher-manual/)
- [Java 26 Language Specification](https://docs.oracle.com/en/java/javase/26/)
- [Apache Maven Shade Plugin Documentation](https://maven.apache.org/plugins/maven-shade-plugin/)
- Course Material EIF400-II-2026 — Dr. Carlos Loría-Sáenz

---

## License

Academic project for educational purposes within the **Universidad Nacional de Costa Rica (UNA)**.
