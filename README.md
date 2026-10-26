# Cyphail - Graph Query Processing Prototype

> A graph query processing prototype inspired by Cypher (Neo4j), built as part of the Programming Paradigms course at Universidad Nacional de Costa Rica.

## Project Overview

**Cyphail** is an interactive graph query processor designed to explore and apply three core programming paradigms through progressive practical implementation:

- **Object-Oriented Programming (OOP)** — Modular software architecture, Command Pattern (`CommandRegistry`), and domain modeling.
- **Functional Programming (FP)** — Pure monadic parser combinators (`Or`, `Star`, `Plus`, `Opt`, `Sequence`, `Combine`), immutable AST structures with Java Records, and functional validation streams.
- **Logic Programming (LP)** — Graph database logic execution (preparation for SWI-Prolog unification in Sprint P2).

This is **Sprint P1 (Setiembre 2026)**, which extends the initial prototype (P1.1) by incorporating:
1. **Functional Parser Combinators** built from scratch in Java to parse Cypher queries (`MATCH`, `WHERE`, `CREATE`, `DELETE`, `RETURN`).
2. **AST (Abstract Syntax Tree) Generation & Pretty-Printing** via the `.tree` REPL command in a pre-order pseudo-JSON representation.
3. **Semantic Scope Validation (`VariableValidator`)** detecting undefined variables or out-of-order declarations with descriptive messages in English.
4. **Decoupled Data Architecture ("Sin Alambramiento")** loading dynamic datasets directly from disk (`data/*.json`), allowing hot-editing of graphs without recompilation.
5. **Comprehensive Test Suite** verifying cases **C1 through C11** and custom extension **C12 ("Sorpresas")**.

**Course:** EIF400-II-2026: Paradigmas de Programación  
**Institution:** Escuela de Informática — Universidad Nacional de Costa Rica  
**Professor:** Dr. Carlos Loría-Sáenz  
**Date:** Setiembre 2026  

---

## Team Members (Grupo 01 - 3:00 PM)

| Name | Student ID |
|------|------------|
| Priscilla Murillo Romero | 402640465 |
| Aaron Ruiz Medina | 402620823 |
| Samael Sánchez Mora | 119470968 |
| Daniel Villarroel Abaduca | 402510622 |
| Nicolás Zárate Hernández | 208730258 |

---

## Prerequisites

- **Java Development Kit (JDK):** 21 or higher (configured for Java 26)
- **Apache Maven:** 3.8.1 or higher

### Environment Verification

```bash
java --version
mvn --version
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
- Execute the complete test suite (43+ JUnit 5 tests covering all parser combinators, AST printer, and C1–C12 test cases).
- Generate an executable Uber/Shaded JAR containing all dependencies at:
  ```
  target/cyphail-1.0-SNAPSHOT.jar
  ```

### Run the Application

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
| `.exit` | Terminate the interactive REPL session |
| `.use` | List all available graph datasets discovered in `data/` |
| `.use <graph_name>` | Load and activate a specific dataset (e.g., `.use person`, `.use client`) |
| `.tree <query>` | Parse a Cypher query and display its AST in pre-order notation |

### AST Inspection with `.tree`

The `.tree` command invokes `CyphailParser`, builds the internal AST, runs `VariableValidator`, and outputs the structured tree using `AstPrinter`:

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

If the query contains semantic variable errors (such as referencing an undefined variable or using a variable before declaration), `.tree` halts formatting and displays the validation error:
```
cyphail> .tree MATCH (p:Person) WHERE q.age > 60 RETURN q AS name
Error: Variable 'q' is not defined in clause WHERE
```

---

## Dynamic Data Architecture (`data/` Directory & Hot-Reload)

To eliminate hardcoded data ("alambramiento"), `JsonGraphLoader` reads datasets directly from the `data/` directory relative to execution:

* **Available Datasets:**
  * `data/movies.json` — Movie nodes and metadata.
  * `data/books.json` — Book catalog with pages and authors.
  * `data/person.json` — People records and ages.
  * `data/order.json` — Customer orders, statuses, and links.
  * `data/amigos.json` — Social network relationships.
  * `data/client.json` — VIP clients, ratings, and loyalty badges.

### Hot-Reload Demonstration
Because files are loaded directly from disk via Google Gson rather than from inside the compiled JAR:
1. Open the REPL and select a graph: `.use person`.
2. Inspect or query the nodes.
3. Edit `data/person.json` in any text editor (modify an age or add a node) and save.
4. Run `.use person` or query again in the open REPL: the updated data is reflected immediately **without restarting or recompiling**.

---

## Test Cases (Sprint P1: C1 to C12)

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

## CodeGen: Cyphail → Prolog Transpiler (Sprint P2)

The `codegen` package transpiles a validated AST into Prolog goals that the SWI-Prolog engine (`prolog/cy_engine.pl`) executes through MQI. `CodeGen` is an AST visitor written with sealed types and pattern-matching `switch`; it threads an immutable state (symbol table + emitted goals) from one method to the next.

```java
GeneratedQuery gen = CodeGen.generate(query, currentGraph);  // currentGraph: the one selected with .use
gen.goal();     // Prolog goal sent through MQI (no final period)
gen.columns();  // RETURN column names, in order (alias or query text)
gen.symbols();  // SymbolTable: Cyphail variable -> Prolog variable
```

### Generated goal

```text
MATCH (m:Movie) WHERE m.year > 2000 RETURN m.title
```
```prolog
cy_create_query(__QueryId, (cy_row(__M_title) :- user_movies_node(__M), user_movies_label(__M, 'Movie'),
    user_movies_prop(__M, year, __M_year), cy_gt(__M_year, 2000), user_movies_prop(__M, title, __M_title))),
cy_exec_query(__QueryId, ['m.title'], Rows), cy_erase_query(__QueryId)
```

The engine binds `Rows` to the table as text (`header\nrow1\nrow2...`, columns separated by a space); with no results it holds only the header. The broker extracts that binding from the MQI JSON response.

### Translation rules

| Cyphail | Prolog |
|---------|--------|
| `(a:L1:L2)` | `user_G_node(__A), user_G_label(__A, 'L1'), user_G_label(__A, 'L2')` |
| `(a:L)` again (join variable) | only `user_G_label(__A, 'L')` |
| `{name: "Ana"}` | `user_G_prop(__A, name, __A_name), cy_eq(__A_name, 'Ana')` |
| `a.age > 18` | `user_G_prop(__A, age, __A_age), cy_gt(__A_age, 18)` |
| `= <> < > <= >=` | `cy_eq cy_neq cy_lt cy_gt cy_le cy_ge` |
| `RETURN a.name AS nombre` | argument `__A_name` of `cy_row`, column `nombre` |
| `RETURN a` | argument `__A`: only the node id (P2 restriction) |

`G` is the current graph. A property is fetched only once even if it is used several times; a row whose property is missing is discarded.

### Variable mapping

- Cyphail variables become Prolog variables with the `__` prefix and the first letter in uppercase: `a` → `__A`, `persona` → `__Persona`.
- Property computations get their own variable: `a.name` → `__A_name`.
- `SymbolTable` keeps these associations to build the final table in Java, and resolves collisions with a `_N` suffix (`a` and `A` → `__A`, `__A_2`). `__QueryId` and `Rows` are reserved.

### P2 scope

As indicated by the professor in class, only read queries (`MATCH`, `WHERE`, `RETURN`) are transpiled. `CREATE` and `DELETE` are still parsed and shown by `.tree`, but `CodeGen` rejects them with a `CodeGenException` (`"CREATE is not supported in Sprint P2 (only MATCH, WHERE and RETURN)"`). Relationship patterns will be translated to the engine's `cy_edge(G, out|in|none, E, Type, A, B)` once the AST supports them.

---

## Project Structure

```
Cyphail-grupo01-3pm/
├── pom.xml                                   # Maven configuration (Java 26, Shade, JUnit 5)
├── README.md                                 # Official project documentation
├── cyphail.bat                               # Windows execution script
├── cyphail                                   # Unix execution script
├── data/                                     # Hot-reloadable graph datasets (JSON)
│   ├── amigos.json
│   ├── books.json
│   ├── client.json
│   ├── movies.json
│   ├── order.json
│   └── person.json
└── src/
    ├── main/java/org/cyphail/
    │   ├── Main.java                         # Application entrypoint
    │   ├── ast/                              # AST Model (Records) & AstPrinter
    │   │   ├── AstPrinter.java
    │   │   ├── Binary.java / BinaryOp.java
    │   │   ├── CreateClause.java / DeleteClause.java
    │   │   ├── MatchClause.java / WhereClause.java / ReturnClause.java
    │   │   ├── PatternNode.java / PropertyEntry.java / PropertyAccess.java
    │   │   └── Query.java / Projection.java / ProjectionItem.java
    │   ├── codegen/                          # Cyphail -> Prolog transpiler (P2)
    │   │   ├── CodeGen.java / GeneratedQuery.java
    │   │   ├── SymbolTable.java / PrologSyntax.java
    │   │   └── CodeGenException.java
    │   ├── cli/                              # Picocli command bindings
    │   │   ├── CyphailCommand.java
    │   │   └── ReplCommand.java
    │   ├── data/                             # JSON Data loader
    │   │   ├── FakeGraphData.java
    │   │   └── JsonGraphLoader.java
    │   ├── engine/                           # Query execution abstraction
    │   │   ├── Engine.java
    │   │   └── FakePrologEngine.java
    │   ├── parser/                           # Functional combinator parser
    │   │   ├── CyphailParser.java
    │   │   ├── combinators/Parsers.java
    │   │   ├── core/ (Parser, Result, Ok, Fail, InputString, TokenString, Lexer)
    │   │   └── lexer/Lexers.java
    │   ├── repl/                             # Interactive REPL & Command Pattern
    │   │   ├── CommandOutcome.java / CommandRegistry.java
    │   │   ├── QueryHandler.java / Repl.java / ReplCommand.java
    │   │   └── ReplMessages.java
    │   ├── util/                             # Table formatting & I/O helpers
    │   │   ├── IO.java
    │   │   └── TableFormatter.java
    │   └── validator/                        # Semantic scope validation
    │       └── VariableValidator.java
    └── test/java/org/cyphail/
        ├── codegen/
        │   ├── CodeGenTest.java              # Hand-built ASTs -> Prolog goals
        │   └── PrologSyntaxTest.java
        └── parser/
            ├── AstPrinterTest.java
            ├── CyphailParserTest.java
            ├── CyphailParserP1Test.java      # C1–C12 test suite
            ├── combinators/ParsersTest.java
            ├── lexer/LexerTest.java
            └── result/ResultTest.java
```

---

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 26 | Core programming language (Records, Pattern Matching, Sealed Types) |
| **Maven** | 3.8.1+ | Build lifecycle, dependency management, and Uber JAR packaging |
| **picocli** | 4.7.6 | Command line interface parsing |
| **Gson** | 2.11.0 | JSON deserialization for decoupled data loading |
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
