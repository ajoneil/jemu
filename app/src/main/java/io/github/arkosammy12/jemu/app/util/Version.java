package io.github.arkosammy12.jemu.app.util;

import picocli.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class Version {

    public static final String VERSION;

    static {
        Properties props = new Properties();
        try (InputStream is = Version.class.getResourceAsStream("/version.properties")) {
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Could not load version.properties", e);
        }
        VERSION = props.getProperty("version", "unknown");
    }

    private Version() {}

    public static final class Provider implements CommandLine.IVersionProvider {

        @Override
        public String[] getVersion() {
            return new String[] { Version.VERSION };
        }
    }

}