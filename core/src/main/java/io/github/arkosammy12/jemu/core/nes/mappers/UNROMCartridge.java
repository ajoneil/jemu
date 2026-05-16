package io.github.arkosammy12.jemu.core.nes.mappers;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nes.NESCartridge;
import io.github.arkosammy12.jemu.core.nes.NESEmulator;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;

import java.util.Arrays;
import java.util.Optional;

import static io.github.arkosammy12.jemu.core.nes.RP2C02.CHR_ROM_END;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.CHR_ROM_START;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.CIRAM_MIRROR_END;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.CIRAM_START;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.PALETTE_RAM_MIRROR_END;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.PALETTE_RAM_START;

public class UNROMCartridge<E extends NESEmulator> extends NESCartridge<E> {

    private final byte[] programRom;
    private final byte[] characterRom;
    private final byte[] characterRam;

    private int bankSelect;

    private final boolean hasBusConflicts;

    public UNROMCartridge(E emulator, INESFile iNESFile) {
        super(emulator, iNESFile);

        byte[] programRomData = iNESFile.getProgramRom();
        this.programRom = Arrays.copyOf(programRomData, programRomData.length);

        Optional<byte[]> characterRomOptional = iNESFile.getCharacterRom();
        if (characterRomOptional.isEmpty()) {
            this.characterRom = null;
            this.characterRam = new byte[iNESFile.getCharacterRamSize()];
        } else {
            byte[] characterRomData = characterRomOptional.get();
            this.characterRom = Arrays.copyOf(characterRomData, characterRomData.length);
            this.characterRam = null;
        }

        this.hasBusConflicts = switch (iNESFile.getSubmapperNumber()) {
            case 0, 2 -> true;
            default -> false;
        };

    }

    @Override
    public int readBytePPU(int address) {
        if (address >= CHR_ROM_START && address <= CHR_ROM_END) {
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
            throw new EmulatorException("Invalid NES UXROM cartridge PPU read address $%04X!".formatted(address));
        }
    }

    @Override
    public void writeBytePPU(int address, int value) {
        if (address >= CHR_ROM_START && address <= CHR_ROM_END) {
            if (this.characterRam != null) {
                this.characterRam[address % this.characterRam.length] = (byte) value;
            }
        } else if (address >= CIRAM_START && address <= CIRAM_MIRROR_END) {
            this.writeByteVRAM(this.mapNametableAddress(address), value);
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_MIRROR_END) {

        } else {
            throw new EmulatorException("Invalid NES UXROM cartridge PPU write address $%04X!".formatted(address));
        }
    }

    @Override
    public int readByte(int address) {
        if (address >= 0x8000 && address <= 0xFFFF) {
            return (int) this.programRom[this.mapPrgRomAddress(address) % this.programRom.length] & 0xFF;
        } else {
            return -1;
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= 0x8000 && address <= 0xFFFF) {
            if (this.hasBusConflicts) {
                value &= (int) this.programRom[this.mapPrgRomAddress(address) % this.programRom.length] & 0xFF;
            }
            this.bankSelect = value & 0xF;
        }
    }

    private int mapPrgRomAddress(int address) {
        if (address >= 0xC000) {
            return (this.programRom.length - 0x4000) | (address & 0x3FFF);
        } else {
            return (this.bankSelect << 14) | (address & 0x3FFF);
        }
    }

}
