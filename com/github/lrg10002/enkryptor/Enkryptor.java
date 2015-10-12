package com.github.lrg10002.enkryptor;

import com.github.lrg10002.enkryptor.commands.ListCommand;
import com.martiansoftware.jsap.JSAPException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Enkryptor {

    public static Scanner in = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            parseArgs(args);
        } catch (JSAPException e) {
            System.out.println(TColor.RED + "[!] JSAP ERROR:" + TColor.RESET);
            e.printStackTrace();
        }

        //add(new File("/home/layne/Desktop/enktest"), new File("/home/layne/Desktop/hello.txt"), "folder1/hello.txt", "youdidnothingwrong");
    }

    public static void parseArgs(String[] args) throws JSAPException {
        if (args.length < 1) {
            return;
        }

        String c = args[0];
        args = Arrays.copyOfRange(args, 1, args.length);

        if (c.equals("l") || c.equals("list")) {
            ListCommand l = new ListCommand();
            l.execute(args);
        }
    }

    public static String promptPass() {
        if (System.console() != null) {
            return new String(System.console().readPassword("Enter Password: "));
        }
        System.out.print("Enter Password: ");
        return in.nextLine();
    }

    private static void init(File location, String password) {
        EKFile.init(location, password);
    }

    private static void add(File location, File in, String name, String password) {
        EKFile file = new EKFile(location);
        if (!file.checkPassword(password)) {
            throw new RuntimeException("Incorrect password!");
        }
        file.load(password);
        file.addFile(in, name, new ArrayList<>(), password);
        file.writeProperties(password);
    }

    private static void export(File location, String name, File out, String password) {
        EKFile file = new EKFile(location);
        if (!file.checkPassword(password)) {
            throw new RuntimeException("Incorrect password!");
        }
        file.load(password);
        file.exportFile(
                file.pathToId(name),
                out,
                password
        );
    }
}
