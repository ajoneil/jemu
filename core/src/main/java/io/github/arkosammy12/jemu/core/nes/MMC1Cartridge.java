package io.github.arkosammy12.jemu.core.nes;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;

import java.util.Arrays;
import java.util.Optional;

import static io.github.arkosammy12.jemu.core.nes.RP2C02.CIRAM_MIRROR_END;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.CIRAM_START;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.PALETTE_RAM_MIRROR_END;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.PALETTE_RAM_START;

public class MMC1Cartridge <E extends NESEmulator> extends NESCartridge<E> {

    private final byte[] programRom;
    private final byte[] programRam;
    private final byte[] characterRom;
    private final byte[] characterRam;

    private boolean loadRegisterWrittenOnThisCycle;
    private boolean loadRegisterIgnoreWrites;
    private int loadRegisterWriteCounter;

    private int loadRegister;
    private int control;
    private int chrBank0;
    private int chrBank1;
    private int prgBank;

    public MMC1Cartridge(E emulator, INESFile iNESFile) {
        super(emulator, iNESFile);

        byte[] programRomData = iNESFile.getProgramRom();
        this.programRom = Arrays.copyOf(programRomData, programRomData.length);

        this.programRam = new byte[iNESFile.getProgramRamSize()];

        Optional<byte[]> characterRomOptional = iNESFile.getCharacterRom();
        if (characterRomOptional.isEmpty()) {
            this.characterRom = null;
            this.characterRam = new byte[iNESFile.getCharacterRamSize()];
        } else {
            byte[] characterRomData = characterRomOptional.get();
            this.characterRom = Arrays.copyOf(characterRomData, characterRomData.length);
            this.characterRam = null;
        }
    }

    @Override
    public int readBytePPU(int address) {
        if (address >= 0x0000 && address <= 0x0FFF) {
            if (this.characterRom == null) {
                return (int) this.characterRam[address % this.characterRam.length] & 0xFF;
            } else {
                return (int) this.characterRom[address % this.characterRom.length] & 0xFF;
            }
        } else if (address >= 0x1000 && address <= 0x1FFF) {
            if (this.characterRom == null) {
                return (int) this.characterRam[address % this.characterRam.length] & 0xFF;
            } else {
                return (int) this.characterRom[address % this.characterRom.length] & 0xFF;
            }
        } else if (address >= CIRAM_START && address <= CIRAM_MIRROR_END) {
            return this.readByteVRAM(this.mapNametableAddress(address));
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_MIRROR_END) {
            return address & 0xFF;
        } else {
            throw new EmulatorException("Invalid NES MMC1 cartridge PPU read address $%04X!".formatted(address));
        }
    }

    @Override
    public void writeBytePPU(int address, int value) {
        if (address >= 0x0000 && address <= 0x0FFF) {
            if (this.characterRam != null) {
                this.characterRam[address % this.characterRam.length] = (byte) value;
            }
        } else if (address >= 0x1000 && address <= 0x1FFF) {
            if (this.characterRam != null) {
                this.characterRam[address % this.characterRam.length] = (byte) value;
            }
        } else if (address >= CIRAM_START && address <= CIRAM_MIRROR_END) {
            this.writeByteVRAM(this.mapNametableAddress(address), value);
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_MIRROR_END) {

        } else {
            throw new EmulatorException("Invalid NES MMC1 cartridge PPU write address $%04X!".formatted(address));
        }
    }

    @Override
    protected int mapNametableAddress(int address) {
        return switch (this.getNametableArrangement()) {
            case 0 -> (address & 0x3FF);
            case 1 -> 0x400 | (address & 0x3FF);
            case 2 -> {
                int vRamAddr = (address - CIRAM_START) & 0x0FFF;
                yield (vRamAddr & (1 << 10)) | (vRamAddr & 0x03FF);
            }
            case 3 -> {
                int vRamAddr = (address - CIRAM_START) & 0x0FFF;
                yield ((vRamAddr & (1 << 11)) >>> 1) | (vRamAddr & 0x03FF);
            }
            default -> throw new EmulatorException("NES MMC1 nametable arrangement bits not in [0, 3]!");
        };
    }

    @Override
    public int readByte(int address) {
        if (address >= 0x6000 && address <= 0x7FFF) {
            return -1;
        } else if (address >= 0x8000 && address <= 0xBFFF) {
            return -1;
        } else if (address >= 0xC000 && address <= 0xFFFF) {
            return -1;
        } else {
            return -1;
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= 0x6000 && address <= 0x7FFF) {

        } else if (address >= 0x8000 && address <= 0xFFFF) {
            if ((value & 0x80) != 0) {
                this.loadRegister = 0;
                this.loadRegisterWriteCounter = 0;
                this.control |= 0xC;
            } else {
                if (this.loadRegisterWriteCounter >= 4) {
                    int writeValue = ((value & 1) << 4) | (this.loadRegister & 0xF);
                    switch (address & 0x6000) {
                        case 0x0000 -> this.control = writeValue;
                        case 0x2000 -> this.chrBank0 = writeValue;
                        case 0x4000 -> this.chrBank1 = writeValue;
                        case 0x6000 -> this.prgBank = writeValue;
                    }
                    this.loadRegister = 0;
                    this.loadRegisterWriteCounter = 0;
                } else {
                    this.loadRegisterWrittenOnThisCycle = true;
                    if (!this.loadRegisterIgnoreWrites) {
                        this.loadRegisterIgnoreWrites = true;
                        this.loadRegister = ((value & 1) << 3) | (this.loadRegister >>> 1);
                        this.loadRegisterWriteCounter++;
                    }
                }
            }
        }
    }

    @Override
    public void cycle() {
        if (!this.loadRegisterWrittenOnThisCycle && this.loadRegisterIgnoreWrites) {
            this.loadRegisterIgnoreWrites = false;
        }
        this.loadRegisterWrittenOnThisCycle = false;
    }

    private int getNametableArrangement() {
        return this.control & 0b11;
    }

    private int getProgramRomBankMode() {
        return (this.control >>> 2) & 0b11;
    }

    private boolean getCharacterRomBankMode() {
        return (this.control & (1 << 4)) != 0;
    }

    private boolean isProgramRamEnabled() {
        return (this.prgBank & (1 << 4)) == 0;
    }

}
