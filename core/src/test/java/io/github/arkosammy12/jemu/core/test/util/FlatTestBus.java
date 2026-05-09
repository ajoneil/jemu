package io.github.arkosammy12.jemu.core.test.util;

import io.github.arkosammy12.jemu.core.common.Bus;
import it.unimi.dsi.fastutil.ints.Int2ByteArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ByteMap;

public class FlatTestBus implements Bus {

    private final Int2ByteMap ram = new Int2ByteArrayMap();

    @Override
    public void writeByte(int address, int value) {
        this.ram.put(address, (byte) value);
    }

    @Override
    public int readByte(int address) {
        return (int) this.ram.computeIfAbsent(address, _ -> (byte) 0) & 0xFF;
    }

}
