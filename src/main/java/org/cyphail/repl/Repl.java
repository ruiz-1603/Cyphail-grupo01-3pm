package org.cyphail.repl;

import org.cyphail.engine.Engine;
import java.util.Scanner;

public class Repl {
    private final Engine engine;

    public Repl(Engine engine) {
        this.engine = engine;
    }

    public void start() {

        Scanner scanner = new Scanner(System.in);

        IO.println("------------------------------");
        IO.println("            CYPHAIL");
        IO.println("------------------------------");
        IO.println("Type 'exit' to quit.");
        IO.println();

        while (true) {

            IO.print("cyphail> ");

            String command = scanner.nextLine().trim();

            if (command.equalsIgnoreCase("exit")) {
                IO.println("Bye!");
                break;
            }

            if (command.isBlank()) {
                continue;
            }

            String result = engine.execute(command);

            IO.println(result);
        }
    }
}
