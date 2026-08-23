# Cyphail - Graph Query Prototype

> A graph query processing prototype inspired by Cypher (Neo4j), built as part of the Programming Paradigms course at Universidad Nacional de Costa Rica.

##  Project Overview

**Cyphail** is an interactive graph query processor designed to teach three essential programming paradigms through practical implementation:

- **Object-Oriented Programming (OOP)** — Project structure and lifecycle management
- **Functional Programming (FP)** — Parser combinators and data transformations
- **Logic Programming (LP)** — Logical execution via SWI-Prolog

This is **Sprint P1.1**, focusing on professional project setup, an interactive REPL, and a modular architecture foundation.

**Course:** EIF400-II-2026: Paradigmas de Programación  
**Institution:** Universidad Nacional de Costa Rica  
**Professor:** Carlos Loría-Sáenz

---

## Team Members

**Grupo 01-3pm:**

| Name |
|------|
| Priscilla Murillo Romero |
| Aaron Ruiz Medina |
| Samael Sanchez Mora |
| Daniel Villarroel Abaduca |
| Nicolás Zárate Hernández |

---

##  Prerequisites

- **Java Development Kit (JDK)** 26 or higher
- **Maven** 3.8.1 or higher

### Installation

**Windows (with Chocolatey):**
```bash
choco install openjdk maven
```

**macOS (with Homebrew):**
```bash
brew install openjdk maven
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get install openjdk-26-jdk maven
```

### Verify Installation

```bash
java --version
mvn --version
```

---

##  Getting Started

### Build the Project

From the project root directory:

```bash
mvn clean package
```

This will:
- Compile the source code
- Run any tests
- Create an executable JAR in the `target/` directory

### Run the Application

Start the interactive REPL:

```bash
java -jar target/cyphail-1.0-SNAPSHOT.jar repl
```


##  Usage Guide

### System Commands

System commands start with a dot (`.`):

| Command | Description |
|---------|-------------|
| `.help` | Show available commands and examples |
| `.about` | Show project and team information |
| `.exit` | Exit the REPL |
| `.use` | List all available graphs |
| `.use <graph_name>` | Connect to a specific graph |

### Query Commands

Once a graph is selected, enter Cyphail query statements:

```cypher
MATCH (p:Persona) RETURN p.nombre, p.edad
```

```cypher
MATCH (p1:Persona)-[r:AMIGO_DE]->(p2:Persona) 
RETURN p1.nombre, type(r), p2.nombre
```

### Example Session

```
cyphail> .use
Graph       Description
------------------------------------
amigos      Social Network
tasks       Tasks and resources
teams       Soccer Teams
planets     Planets in Solar System

OK. Query available after 5 ms.

cyphail> .use amigos
OK. "amigos" graph available after 2ms

cyphail> MATCH (p:Persona) RETURN p.nombre, p.edad
p.nombre      p.edad
---------------------------
Ana           28
Luis          31
Carlos        25
Beatriz       34
David         29
Elena         22

OK. Query resolved after 42 ms.

cyphail> .exit
Goodbye!
```

---

##  Project Structure

```
Cyphail-grupo01-3pm/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/cyphail/
│   │   │       ├── cli/              # Command-line interface
│   │   │       │   ├── CyphailCommand.java
│   │   │       │   └── ReplCommand.java
│   │   │       ├── engine/           # Query execution engine
│   │   │       │   ├── Engine.java
│   │   │       │   └── FakePrologEngine.java
│   │   │       ├── repl/             # REPL implementation
│   │   │       │   └── Repl.java
│   │   │       ├── data/             # Fake data and graphs
│   │   │       │   └── FakeGraphData.java
│   │   │       ├── util/             # Utilities
│   │   │       │   ├── IO.java
│   │   │       │   └── TableFormatter.java
│   │   │       └── Main.java         # Application entry point
│   │   └── resources/
│   └── test/
│       └── java/
├── pom.xml                           # Maven configuration
├── README.md                          # Documentation
├── .gitignore                         # Git ignore rules
└── target/                            # Build output (generated)
```

---

##  Technology Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| Java | 26 | Programming language |
| Maven | 3.8.1+ | Build management |
| picocli | 4.7.6 | CLI framework |
| JUnit 5 | Latest | Testing framework |
| Gson | 2.11.0 | JSON processing |
| Markdown | — | Documentation |

---


##  References

- [Cypher Documentation](https://neo4j.com/docs/cypher-manual/)
- [Java 26 Documentation](https://docs.oracle.com/en/java/javase/26/)
- [Maven Documentation](https://maven.apache.org/)
- Groups Google - EIF400-II-2026

---

##  AI Usage Declaration

This project was developed with assistance from:

- **Claude AI (claude-sonnet-4)** — For understanding project requirements, code structure guidance, and explanations of programming concepts

**Code Authorship:** All code in this repository was written by the team members with AI assistance for learning purposes only. No automatic code-generation tools were used to produce the entire application.

Prompts used:
"Guiame por medio de explicaciones detalladas (no me des el codigo directamente) para paso a paso implementar lo necesario para el .use con los grafos fake propuestos."

---

##  License

This is an academic project for educational purposes within the **Universidad Nacional de Costa Rica**.

---

##  Last Updated

August 22, 2026

</div>
