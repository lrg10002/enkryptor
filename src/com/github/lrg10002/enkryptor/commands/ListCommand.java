package com.github.lrg10002.enkryptor.commands;

import com.github.lrg10002.enkryptor.EKFile;
import com.github.lrg10002.enkryptor.Enkryptor;
import com.github.lrg10002.enkryptor.InteractiveSession;
import com.github.lrg10002.enkryptor.TColor;
import com.martiansoftware.jsap.*;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ListCommand extends Command {

    private void initJSAP(JSAP jsap) throws JSAPException {
        Switch recurse = new Switch("recurse")
                .setShortFlag('r')
                .setLongFlag("recurse");
        recurse.setHelp("Lists all files beneath starting point recursively.");
        jsap.registerParameter(recurse);

        FlaggedOption pattern = new FlaggedOption("match")
                .setRequired(false)
                .setDefault(".*")
                .setShortFlag('m')
                .setLongFlag("match")
                .setUsageName("match");
        pattern.setHelp("Specifies a Java regex matching filter.");
        jsap.registerParameter(pattern);

        UnflaggedOption start = new UnflaggedOption("start")
                .setRequired(false)
                .setDefault("/")
                .setGreedy(false);
        start.setHelp("The folder in which the files should be listed. Defaults to root directory.");
        jsap.registerParameter(start);
    }

    @Override
    public void execute(String[] args) throws JSAPException {
        JSAP jsap = new JSAP();

        FlaggedOption folder = new FlaggedOption("ekfolder")
                .setUsageName("ekfolder")
                .setDefault(".")
                .setAllowMultipleDeclarations(false)
                .setShortFlag('f')
                .setRequired(false)
                .setLongFlag("folder");
        folder.setHelp("The path to the Enkryptor folder. Defaults to current directory.");
        jsap.registerParameter(folder);

        FlaggedOption password = new FlaggedOption("password")
                .setUsageName("password")
                .setRequired(false)
                .setLongFlag("password")
                .setShortFlag('p')
                .setAllowMultipleDeclarations(false);
        password.setHelp("The Enkryptor folder password.");
        jsap.registerParameter(password);

        initJSAP(jsap);

        if (args[0].equalsIgnoreCase("help") ||
                args[0].equals("-h") || args[0].equals("-?") ||
                args[0].equals("?") || args.length < 1) {
            System.out.println();
            System.out.println("Usage:" + TColor.PURPLE + " list " + TColor.RESET + jsap.getUsage());
            System.out.println("\n" + jsap.getHelp());
            return;
        }

        JSAPResult res = jsap.parse(args);
        if (!res.success()) {
            System.out.println();
            System.out.println(TColor.RED + "\tUsage:" + TColor.RESET + " list " + jsap.getUsage());
            return;
        }

        File ekfolder = new File(res.getString("ekfolder"));
        boolean recurse = res.getBoolean("recurse");
        String pattern = res.getString("match");
        String start = res.getString("start");
        String pwd = res.userSpecified("password") ? res.getString("password") : Enkryptor.promptPass();


        listFiles(ekfolder, recurse, pattern, start, pwd);
    }

    @Override
    public void executeInteractive(InteractiveSession s, String[] args) throws JSAPException {

    }

    private void listFiles(File ekfolder, boolean recurse, String pattern, String start, String password) {
        pattern.replaceAll(Pattern.quote("%*"), "[a-zA-Z0-9]*");
        pattern.replaceAll(Pattern.quote("%+"), "[a-zA-Z0-9]+");




        if (!start.startsWith("/")) {
            start = "/" + start;
        }
        if (!start.endsWith("/")) {
            start = start + "/";
        }
        final String fstart = start;
        EKFile file = new EKFile(ekfolder);
        if (!file.checkPassword(password)) {
            throw new RuntimeException("Incorrect password!");
        }
        file.load(password);

        if (recurse) {
            System.out.println();
            file.properties.nameKeys.values().stream()
                    .filter(s -> s.startsWith(fstart))
                    .filter(s -> s.matches(pattern))
                    .forEachOrdered(s -> System.out.println(" " + s));
        } else {
            file.listFiles(start).stream().forEachOrdered(s -> System.out.println(" " + s));
        }
    }
}
