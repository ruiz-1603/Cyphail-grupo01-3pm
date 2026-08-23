package org.cyphail.repl;

import org.cyphail.engine.Engine;
import org.cyphail.engine.FakePrologEngine;
import org.cyphail.data.FakeGraphData;
import org.cyphail.util.IO;

import java.util.Scanner;

public class Repl {
    private final Engine engine;
     private boolean running;


    public Repl(Engine engine) {
        this.engine = engine;
        this.running = true;
    }

    public void start() {
        welcomeMessage();
        replLoop();
    }

    private void welcomeMessage() {
        IO.println("────────────────────────────────────────");
        IO.println("  Cyphail-01-3pm  v0.1");
        IO.println("  ESCINF / UNA · EIF400-II-2026");
        IO.println("  Visit www.whatiscyphail.com for more information");
        IO.println("────────────────────────────────────────");
        IO.println("  Welcome to Cyphail!");
        IO.println("────────────────────────────────────────");
        IO.println("  Type '.help' for help or 'exit' to quit.\n");

    }

    private void replLoop() {
        Scanner scanner = new Scanner(System.in);
        while (running) {

            IO.print("cyphail> ");
            String command = scanner.nextLine().trim();

            if (command.isBlank()) {
                continue;
            }

            if (command.startsWith(".")) {
                handleCommand(command);
            } else {
                handleQuery(command);
            }
        }
    }

    private void handleCommand(String command) {
        String[] parts = command.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case ".exit": {
                IO.println("Thanks for using Cyphail!");
                running = false;
                break;
            }
            case ".help":
                help();
                break;
            case ".about":
                about();
                break;
            case ".use":
                handleUseCommand(parts);
                break;
            default:
                IO.println("Unknown command: " + command);
                break;
        }
    }

    private void handleUseCommand(String[] parts) {
        if (parts.length == 1) {
            printAvailableGraphs();
        } else {
            String graphName = parts[1].toLowerCase();
            if (FakeGraphData.graphExists(graphName)) {
                engine.setCurrentGraph(graphName);
                long time = (long) (Math.random() * 5) + 1;
                IO.println("OK. \"" + graphName + "\" graph available after " + time + "ms");
                IO.println();
            } else {
                IO.println("ERROR: Graph '" + graphName + "' not found.");
            }
        }
    }

    // Simulación de la lista de grafos disponibles
    private void printAvailableGraphs() {
        IO.println();
        String[][] graphs = new String[][] {
            {"Graph", "Description"},
            {"amigos", "Social Network"},
            {"tasks", "Tasks and resources"},
            {"teams", "Soccer Teams"},
            {"planets", "Planets in Solar System"}
        };

        // Imprimir tabla manualmente
        IO.println(String.format("%-15s %s", graphs[0][0], graphs[0][1]));
        IO.println("-".repeat(45));
        for (int i = 1; i < graphs.length; i++) {
            IO.println(String.format("%-15s %s", graphs[i][0], graphs[i][1]));
        }
        IO.println();
        IO.println("OK. Query available after 5 ms.");
    }

    private void handleQuery(String query) {
        if (engine.getCurrentGraph() == null) {
            IO.println("ERROR: No graph selected. Use .use <graph_name> first.");
            return;
        }

        String result = engine.execute(query);
        IO.println();
        IO.println(result);
    }

    private void help(){
        IO.println("Available Commands:");
        IO.println("  .help      - Show this help message");
        IO.println("  .about     - Show project information");
        IO.println("  .exit      - Exit the REPL");
        IO.println("  .use       - List available graphs or select a graph");
        IO.println("             - Usage: .use <graph_name>");
        IO.println("\n");
        IO.println("Query Examples:");
        IO.println("  MATCH (p:Persona) RETURN p.nombre, p.edad");
        IO.println("  MATCH (p1:Persona)-[r:AMIGO_DE]->(p2:Persona) RETURN p1.nombre, type(r), p2.nombre");
        IO.println("\n");
    }
    private void about() {
        IO.println("Project: Cyphail - Graph Query Prototype");
        IO.println("Course: EIF400-II-2026 ");
        IO.println("School: Universidad Nacional de Costa Rica");
        IO.println("Professor: Carlos Loría-Sáenz");
        IO.println("\n");
        IO.println("Team Members (Grupo 01-3pm):");
        IO.println("  - Priscilla Murillo Romero");
        IO.println("  - Aaron Ruiz Medina");
        IO.println("  - Samael Sanchez Mora");
        IO.println("  - Daniel Villarroel Abaduca");
        IO.println("  - Nicolás Zárate Hernández");
        IO.println("\n");
    }
}
