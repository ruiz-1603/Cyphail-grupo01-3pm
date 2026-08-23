package org.cyphail.cli;

import org.cyphail.engine.Engine;
import org.cyphail.engine.FakePrologEngine;
import org.cyphail.repl.Repl;
import picocli.CommandLine.Command;

@Command(name = "repl",
        description = "Starts the Cyphail interactive REPL")

public class ReplCommand implements Runnable {
    private static final FakePrologEngine ENGINE = new FakePrologEngine();

    @Override
    public void run() {
        new Repl(ENGINE).start();
    }
}
