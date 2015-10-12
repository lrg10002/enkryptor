package com.github.lrg10002.enkryptor.commands;

import com.github.lrg10002.enkryptor.InteractiveSession;
import com.martiansoftware.jsap.JSAPException;

public abstract class Command {

    public abstract void execute(String[] args) throws JSAPException;

    public abstract void executeInteractive(InteractiveSession s, String[] args) throws JSAPException;
}
