package io.github.arkosammy12.jemu.app;

import io.github.arkosammy12.jemu.app.io.CLIArgs;
import org.tinylog.Logger;

public class Main {

    static void main(String[] args) {
        CLIArgs cliArgs = null;
        if (args.length > 0) {
            cliArgs = new CLIArgs(args);
            if (cliArgs.exitImmediately()) {
                return;
            }
        }
        try {
            Jemu jemu = new Jemu(cliArgs);
            jemu.start();
        } catch (Throwable t) {
            Logger.error("jemu has crashed! {}", t);
        }
    }

}