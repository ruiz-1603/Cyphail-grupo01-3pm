package org.cyphail.parser.core;

public interface Input<T>{
    T input();
    int index();
}