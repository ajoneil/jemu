package io.github.arkosammy12.jemu.core.common;

public interface Processor {

    int cycle();

    static int setBit(int value, int mask) {
        return value | mask;
    }

}
