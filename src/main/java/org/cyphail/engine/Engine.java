package org.cyphail.engine;

public interface Engine {
    String execute(String command);
    void setCurrentGraph(String graphName);
    String getCurrentGraph();
}