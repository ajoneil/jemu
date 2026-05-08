package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.common.Bus;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;

import static io.github.arkosammy12.jemu.core.nes.RP2C02.CIRAM_START;

public abstract class NESCartridge<E extends NESEmulator> implements Bus {

    protected final E emulator;
    protected final INESFile iNESFile;
    private final INESFile.NametableArrangement iNESFileNametableArrangement;

    private final byte[] vRam = new byte[0x800];

    public NESCartridge(E emulator, INESFile iNESFile) {
        this.emulator = emulator;
        this.iNESFile = iNESFile;
        this.iNESFileNametableArrangement = this.iNESFile.getNametableArrangement();
    }

    public static <E extends NESEmulator> NESCartridge<E> getCartridge(E emulator, INESFile iNESFile) {
        int mapperNumber = iNESFile.getMapperNumber();
        return switch (mapperNumber) {
            case 0 -> new NROMCartridge<>(emulator, iNESFile);
            case 1 -> new MMC1Cartridge<>(emulator, iNESFile);
            case 2 -> new UXROMCartridge<>(emulator, iNESFile);
            case 3 -> new CNROMCartridge<>(emulator, iNESFile);
            case 7 -> new AXROMCartridge<>(emulator, iNESFile);
            default -> throw new EmulatorException("Unimplemented iNES mapper number %d!".formatted(mapperNumber));
        };
    }

    public INESFile getINESFile() {
        return this.iNESFile;
    }

    abstract public int readBytePPU(int address);

    abstract public void writeBytePPU(int address, int value);

    protected int readByteVRAM(int address) {
        return (int) this.vRam[address] & 0xFF;
    }

    protected void writeByteVRAM(int address, int value) {
        this.vRam[address] = (byte) value;
    }

    protected int mapNametableAddress(int address) {
        int vRamAddr = (address - CIRAM_START) & 0x0FFF;
        return switch (this.iNESFileNametableArrangement) {
            case HORIZONTAL -> (vRamAddr & (1 << 10)) | (vRamAddr & 0x03FF);
            case VERTICAL -> ((vRamAddr & (1 << 11)) >>> 1) | (vRamAddr & 0x03FF);
        };
    }

    public void cycle() {

    }

    public boolean getIRQSignal() {
        return false;
    }

}
