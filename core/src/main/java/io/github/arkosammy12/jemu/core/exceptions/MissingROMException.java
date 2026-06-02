package io.github.arkosammy12.jemu.core.exceptions;

public class MissingROMException extends EmulatorException {

    public MissingROMException(String systemName) {
        super(systemName + " requires a ROM file to start!");
    }

}
