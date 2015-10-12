package com.github.lrg10002.enkryptor.console;

import com.github.lrg10002.enkryptor.core.Konstants;

public class ConsoleWrapper {

    private IConsoleWrapper console;

    private int level = 0;
    private String levelString = "";

    public ConsoleWrapper() {
        if (System.console() == null) {
            console = new StandardWrapperImpl();
            printWelcome();
            warnOfStandard();
        } else {
            console = new SystemWrapperImpl();
            printWelcome();
        }
    }

    private void printWelcome() {
        console.println("===[Starting Enkryptor v" + Konstants.VERSION + "]===");
    }

    public void printExit() {
        console.println("===[Exiting Enkryptor v" + Konstants.VERSION + "]===");
    }

    private void warnOfStandard() {
        if (!yesOrNo("You are not running Enkryptor from a true console, therefore any passwords you enter will be visible. Do you wish to continue:")) {
            printExit();
            System.exit(0);
        }
    }

    public boolean yesOrNo(String prompt) {
        String res = console.readInput(prompt).toUpperCase();
        for (String s : Konstants.NEGATIVES) {
            if (res.equals(s)) {
                return false;
            }
        }
        for (String s : Konstants.POSITIVES) {
            if (res.equals(s)) {
                return true;
            }
        }
        if (res.startsWith("T") || res.startsWith("Y")) {
            return true;
        }
        if (res.startsWith("F") || res.startsWith("N")) {
            return false;
        }
        console.println("Please specify... yes or no?");
        return yesOrNo(prompt);
    }

    public void indent() {
        level++;
        levelString = "";
        for (int i = 0; i < level; i++) {
            levelString += "    ";
        }
    }

    public void outdent() {
        level--;
        if (level < 0) {
            level = 0;
        }
        levelString = "";
        for (int i = 0; i < level; i++) {
            levelString += "    ";
        }
    }

    public void println(Object o) {
        console.print(levelString + ">");
        console.println(o);
    }

    public void println() {
        console.println("");
    }

    public void errln(Object o) {
        console.println("[ERROR]: " + o);
    }

    public String prompt(Object o) {
        return console.readInput(o.toString());
    }

    public String promptPass(Object o) {
        return console.readPassword(o.toString());
    }

    public void printStackTrace(Throwable t) {
        console.printStackTrace(t);
    }


}
