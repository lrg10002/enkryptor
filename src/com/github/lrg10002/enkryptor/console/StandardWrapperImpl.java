package com.github.lrg10002.enkryptor.console;

import java.util.Scanner;

public class StandardWrapperImpl implements IConsoleWrapper {

    private Scanner in = new Scanner(System.in);


    @Override
    public void print(Object o) {
        System.out.print(o);
    }

    @Override
    public void println(Object o) {
        System.out.println(o);
    }

    @Override
    public void printStackTrace(Throwable t) {
        t.printStackTrace(System.err);
    }

    @Override
    public String readPassword() {
        return readInput();
    }

    @Override
    public String readPassword(String prompt) {
        return readInput(prompt);
    }

    @Override
    public String readInput(String prompt) {
        System.out.print(prompt + " ");
        return readInput();
    }

    @Override
    public String readInput() {
        return in.nextLine();
    }
}
