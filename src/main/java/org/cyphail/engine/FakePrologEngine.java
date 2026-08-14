package org.cyphail.engine;

public class FakePrologEngine implements Engine {

    @Override
    public String execute(String command) {

        return """
                [Fake Prolog]
                Command received: %s
                Result: OK
                """.formatted(command);
    }
}
