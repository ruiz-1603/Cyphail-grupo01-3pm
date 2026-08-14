package org.cyphail.cli;

import picocli.CommandLine.Command;

@Command(name = "cyphail",
        description = "Cyphail graph query prototype",
        mixinStandardHelpOptions = true,
        subcommands = {ReplCommand.class})

public class CyphailCommand implements Runnable {
    @Override
    public void run() {
        IO.println("Use --help to see available commands.");
    }
}
