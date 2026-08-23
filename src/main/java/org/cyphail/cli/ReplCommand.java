package org.cyphail.cli;

import org.cyphail.engine.Engine;
import org.cyphail.engine.FakePrologEngine;
import org.cyphail.repl.Repl;
import picocli.CommandLine.Command;

@Command(name = "repl",
        description = "Starts the Cyphail interactive REPL")

public class ReplCommand implements Runnable {
    @Override
    public void run() {
        FakePrologEngine engine = new FakePrologEngine();
        Repl repl = new Repl(engine);
        repl.start();
    }
}
