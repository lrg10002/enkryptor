package com.github.lrg10002.enkryptor.core;

import com.github.lrg10002.enkryptor.console.ConsoleWrapper;
import com.github.lrg10002.enkryptor.file.EnkryptorFile;
import com.github.lrg10002.enkryptor.io.EnkryptorFileCreator;
import com.martiansoftware.jsap.*;
import com.martiansoftware.jsap.stringparsers.FileStringParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class Enkryptor {

    public static final Charset CHARSET = Charset.forName("UTF-8");

    public static ConsoleWrapper console;

    public static void main(String[] args) {
        args = new String[]{"-c", "/Users/Layne/Desktop/test.enk"};

        console = new ConsoleWrapper();

        JSAP jsap = new JSAP();
        try {
            setUpJsap(jsap);
        } catch (Exception e) {
            console.errln("Failed to start JSAP. This is an embarrassing error.");
            System.exit(1);
        }

        JSAPResult result = jsap.parse(args);
        if (!result.success()) {
            console.errln("Check your arguments! There seems to have been a slight problem...");
            System.exit(1);
        }

        File file = result.getFile("file");
        boolean create = result.userSpecified("create");
        if (create && file.exists()) {
            console.println("Oops! That file already exists!");
            System.exit(1);
        } else if (create) {
            //File source = result.getFile("create");
            //if (!source.exists()) {
               // console.println("Oops! The source folder you supplied does not exist!");
              //  System.exit(1);
           // } else if (!source.isDirectory()) {
              //  console.println("Oops! The source folder you supplied... well... its not a folder!");
             //   System.exit(1);
           // }
            launchCreate(file);
        }

        boolean add = result.userSpecified("add");
        if (add) {
            console.println();
            if (!file.exists()) {
                console.println("Oops! That Enkryptor file doesn't exist!");
                System.exit(1);
            }
            File source = result.getFile("add");
            if (!source.exists()) {
                console.println("Oops! The file or folder you specified doesn't exist!");
                System.exit(1);
            }
            if (source.isDirectory()) {
                launchAddDir(source, file);
            } else {
                launchAddFile(source, file);
            }
        }



    }

    private static void setUpJsap(JSAP jsap) throws JSAPException {
        Switch createOption = new Switch("create")
                .setShortFlag('c')
                .setLongFlag("create");
        FlaggedOption addOption = new FlaggedOption("add")
                .setShortFlag('a')
                .setLongFlag("add")
                .setRequired(false)
                .setAllowMultipleDeclarations(false);
        UnflaggedOption fileOption =
                new UnflaggedOption("file")
                        .setStringParser(FileStringParser.getParser())
                        .setRequired(true);
        fileOption.setHelp("The path to the Enkryptor file.");

        jsap.registerParameter(createOption);
        jsap.registerParameter(addOption);
        jsap.registerParameter(fileOption);
    }

    private static EnkryptorFile loadEnkryptorFile(File file) throws IOException {
        EnkryptorFile efile = new EnkryptorFile(file);
        efile.readHeader();
        console.println();
        console.println("You must enter credentials to open the Enkryptor file.");
        while (true) {
            String primary = console.prompt("Enter primary password:");
            if (efile.validatePrimaryPass(primary)) break;
            console.println("Incorrect password!");
        }
        String[] answers = new String[efile.getNumberOfQuestions()];
        console.println("You must now answer the following security questions:");
        for (int i = 0; i < answers.length; i++) {
            while (true) {
                String answer = console.prompt(efile.getQuestionDecrypted(i));
                if (efile.validateAnswer(i, answer)) break;
                console.println("Incorrect answer!");
            }
        }
        efile.setAnswers(answers);
        console.println("Unlocking...");
        efile.unlock();
        console.println("Enkryptor file unlocked.");

        return efile;
    }

    private static void launchCreate(File dest) {
        EnkryptorFileCreator creator = new EnkryptorFileCreator(dest);
        String primary;
        while (true) {
            primary = console.promptPass("Create a primary password:");
            if (primary.isEmpty()) {
                console.println("The password must not be empty!");
                continue;
            }
            String primary2 = console.promptPass("Confirm your primary password:");
            if (primary.equals(primary2)) break;
            console.println("Those two passwords did not match!");
        }
        int numQuestions;
        while (true) {
            String ns = console.prompt("How many security questions would you like to create?");
            if (ns.matches("[0-9]+")) {
                numQuestions = Integer.parseInt(ns);
                break;
            }
            console.println("Please enter a valid integer!");
        }
        String[] questions = new String[numQuestions];
        String[] answers = new String[numQuestions];
        for (int i = 0; i < numQuestions; i++) {
            String question;
            while (true) {
                question = console.prompt("Question " + i + " - Enter Question:");
                if (!question.isEmpty()) break;
                console.println("Please do not enter a blank question!");
            }
            String answer;
            while (true) {
                answer = console.prompt("Question " + i + " - Enter Answer:");
                if (!answer.isEmpty()) break;
                console.println("Please do not enter a blank question!");
            }
            questions[i] = question;
            answers[i] = answer;
        }
        try {
            creator.createFile(primary, questions, answers);
            console.println("Your Enkryptor file was initialized successfully.");
        } catch (IOException e) {
            boolean st = console.yesOrNo("An IO error occurred during Enkryptor file creation. Print stacktrace (y/n)?");
            if (st) console.printStackTrace(e);
        }

    }

    private static void launchAddFile(File source, File dest) {

    }

    private static void launchAddDir(File source, File dest) {

    }
}
