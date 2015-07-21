package com.github.lrg10002.enkryptor.console;

import java.io.Console;

public class SystemWrapperImpl implements IConsoleWrapper {

    private Console console = System.console();


    @Override
    public void print(Object o) {
        console.printf(o.toString());
        console.flush();
    }

    @Override
    public void println(Object o) {
        console.printf(o.toString() + "\n");
        console.flush();
    }

    @Override
    public String readPassword() {
        return new String(console.readPassword());
    }

    @Override
    public String readPassword(String prompt) {
        return new String(console.readPassword(prompt + " "));
    }

    @Override
    public String readInput(String prompt) {
        return console.readLine(prompt + " ");
    }

    @Override
    public String readInput() {
        return console.readLine();
    }
}
