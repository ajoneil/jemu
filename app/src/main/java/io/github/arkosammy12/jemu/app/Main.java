package io.github.arkosammy12.jemu.app;

import org.tinylog.Logger;

public class Main {

    static void main(String[] args) throws Exception {
        Jemu jemu = null;
        try {
            jemu = new Jemu(args);
            jemu.start();
        } catch (Throwable t) {
            Logger.error("jemu has crashed! {}", t);
        } finally {
            if (jemu != null) {
                jemu.onShutdown();
            }
        }
    }

}