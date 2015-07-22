package com.github.lrg10002.enkryptor.console;

public interface IConsoleWrapper {

    void print(Object o);

    void println(Object o);

    void printStackTrace(Throwable t);

    String readPassword();

    String readPassword(String prompt);

    String readInput(String prompt);

    String readInput();
}
