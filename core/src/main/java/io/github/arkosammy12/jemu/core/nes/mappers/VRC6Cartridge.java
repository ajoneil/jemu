package io.github.arkosammy12.jemu.core.nes.mappers;

import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.nes.NESCartridge;
import io.github.arkosammy12.jemu.core.nes.NESEmulator;
import io.github.arkosammy12.jemu.core.nes.ines.INESFile;
import io.github.arkosammy12.jemu.core.nes.ines.NES20File;

import java.util.Arrays;
import java.util.Optional;

import static io.github.arkosammy12.jemu.core.nes.RP2C02.CIRAM_MIRROR_END;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.CIRAM_START;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.PALETTE_RAM_MIRROR_END;
import static io.github.arkosammy12.jemu.core.nes.RP2C02.PALETTE_RAM_START;

public class VRC6Cartridge<E extends NESEmulator> extends NESCartridge<E> {

    private final byte[] programROM;
    protected final byte[] programRAM;
    private final byte[] characterROM;
    private final byte[] characterRAM;

    private final int a0Bit;
    private final int a1Bit;

    private final VRC4Cartridge.VRCIRQEngine vrcirqEngine = new VRC4Cartridge.VRCIRQEngine();

    private int prgSelect16K;
    private int prgSelect8K;
    private int ppuBankingStyle;

    private NametableArrangement nametableArrangement = NametableArrangement.HORIZONTAL;

    private int R0;
    private int R1;
    private int R2;
    private int R3;
    private int R4;
    private int R5;
    private int R6;
    private int R7;

    public VRC6Cartridge(E emulator, INESFile iNESFile) {
        super(emulator, iNESFile);

        byte[] programRomData = iNESFile.getProgramRom();
        this.programROM = Arrays.copyOf(programRomData, programRomData.length);

        int programRamSize = iNESFile.getProgramRamSize();
        if (iNESFile instanceof NES20File nes20File) {
            programRamSize += nes20File.getNonVolatileProgramRamSizeBytes();
        }

        this.programRAM = programRamSize > 0 ? new byte[programRamSize] : null;

        Optional<byte[]> characterRomOptional = iNESFile.getCharacterRom();
        if (characterRomOptional.isEmpty()) {
            this.characterROM = null;
            int characterRamSize = iNESFile.getCharacterRamSize();
            if (iNESFile instanceof NES20File nes20File) {
                characterRamSize += nes20File.getNonVolatileCharacterRamSizeBytes();
            }
            this.characterRAM = new byte[characterRamSize];
        } else {
            byte[] characterRomData = characterRomOptional.get();
            this.characterROM = Arrays.copyOf(characterRomData, characterRomData.length);
            this.characterRAM = null;
        }

        this.a0Bit = switch (iNESFile.getMapperNumber()) {
            case 24 -> 0;
            case 26 -> 1;
            default -> throw new EmulatorException("Invalid mapper number %d for VRC6!".formatted(this.iNESFile.getMapperNumber()));
        };

        this.a1Bit = switch (iNESFile.getMapperNumber()) {
            case 24 -> 1;
            case 26 -> 0;
            default -> throw new EmulatorException("Invalid mapper number %d for VRC6!".formatted(this.iNESFile.getMapperNumber()));
        };

    }

    @Override
    public int readBytePPU(int address) {
        this.observePPUAddress(address);
        if (address >= 0x0000 && address <= 0x1FFF) {
            if (this.characterROM == null) {
                return (int) this.characterRAM[this.mapChrAddress(address) % this.characterRAM.length] & 0xFF;
            } else {
                return (int) this.characterROM[this.mapChrAddress(address) % this.characterROM.length] & 0xFF;
            }
        } else if (address >= CIRAM_START && address <= CIRAM_MIRROR_END) {
            return this.readByteVRAM(this.mapNametableAddress(address));
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_MIRROR_END) {
            return address & 0xFF;
        } else {
            throw new EmulatorException("Invalid NES VRC2 cartridge PPU read address $%04X!".formatted(address));
        }
    }

    @Override
    public void writeBytePPU(int address, int value) {
        this.observePPUAddress(address);
        if (address >= 0x0000 && address <= 0x1FFF) {
            if (this.characterRAM != null) {
                this.characterRAM[this.mapChrAddress(address) % this.characterRAM.length] = (byte) value;
            }
        } else if (address >= CIRAM_START && address <= CIRAM_MIRROR_END) {
            this.writeByteVRAM(this.mapNametableAddress(address), value);
        } else if (address >= PALETTE_RAM_START && address <= PALETTE_RAM_MIRROR_END) {

        } else {
            throw new EmulatorException("Invalid NES VRC2 cartridge PPU write address $%04X!".formatted(address));
        }
    }

    @Override
    public int readByte(int address) {
        if (address >= 0x6000 && address <= 0x7FFF) {
            if (this.programRAM != null && this.isPrgRamEnabled()) {
                return (int) this.programRAM[this.mapPrgRamAddress(address) % this.programRAM.length] & 0xFF;
            } else {
                return -1;
            }
        } else if (address >= 0x8000 && address <= 0xFFFF) {
            return (int) this.programROM[this.mapPrgRomAddress(address) % this.programROM.length] & 0xFF;
        } else {
            return -1;
        }
    }

    @Override
    public void writeByte(int address, int value) {
        if (address >= 0x6000 && address <= 0x7FFF) {
            if (this.programRAM != null && this.isPrgRamEnabled()) {
                this.programRAM[this.mapPrgRamAddress(address) % this.programRAM.length] = (byte) value;
            }
        } else if (address >= 0x8000 && address <= 0x8FFF) {
            if (this.getRegisterSlot(address) <= 3) {
                this.prgSelect16K = value & 0xF;
            }
        } else if (address >= 0x9000 && address <= 0x9FFF) {
            switch (this.getRegisterSlot(address)) {
                case 0 -> {} // Pulse 1 duty and volume
                case 1 -> {} // Pulse 1 period low
                case 2 -> {} // Pulse 1 period high
                case 3 -> {} // Frequency scaling
            }
        } else if (address >= 0xA000 && address <= 0xAFFF) {
            switch (this.getRegisterSlot(address)) {
                case 0 -> {} // Pulse 2 duty and volume
                case 1 -> {} // Pulse 2 period low
                case 2 -> {} // Pulse 2 period high
            }
        } else if (address >= 0xB000 && address <= 0xBFFF) {
            switch (this.getRegisterSlot(address)) {
                case 0 -> {} // Saw volume
                case 1 -> {} // Saw period low
                case 2 -> {} // Saw period high
                case 3 -> {
                    this.ppuBankingStyle = value & 0xBF;

                    if ((value & (1 << 4)) != 0) {
                        throw new EmulatorException("Unimplemented VRC6 ROM nametables ($B003 bit 4 set): $%02X".formatted(value));
                    }
                    this.nametableArrangement = switch (value & 0xF) {
                        case 0x0 -> NametableArrangement.HORIZONTAL;
                        case 0x4 -> NametableArrangement.VERTICAL;
                        case 0x8 -> NametableArrangement.SINGLE_SCREEN_LOWER_BANK;
                        case 0xC -> NametableArrangement.SINGLE_SCREEN_UPPER_BANK;
                        default -> throw new EmulatorException("Unimplemented VRC6 nametable mirroring mode: $%02X".formatted(value & 0xF));
                    };
                }
            }
        } else if (address >= 0xC000 && address <= 0xCFFF) {
            if (this.getRegisterSlot(address) <= 3) {
                this.prgSelect8K = value & 0x1F;
            }
        } else if (address >= 0xD000 && address <= 0xDFFF) {
            switch (this.getRegisterSlot(address)) {
                case 0 -> this.R0 = value & 0xFF;
                case 1 -> this.R1 = value & 0xFF;
                case 2 -> this.R2 = value & 0xFF;
                case 3 -> this.R3 = value & 0xFF;
            }
        } else if (address >= 0xE000 && address <= 0xEFFF) {
            switch (this.getRegisterSlot(address)) {
                case 0 -> this.R4 = value & 0xFF;
                case 1 -> this.R5 = value & 0xFF;
                case 2 -> this.R6 = value & 0xFF;
                case 3 -> this.R7 = value & 0xFF;
            }
        } else if (address >= 0xF000 && address <= 0xFFFF) {
            switch (this.getRegisterSlot(address)) {
                case 0 -> this.vrcirqEngine.writeIRQLatch(value);
                case 1 -> this.vrcirqEngine.writeIRQControl(value);
                case 2 -> this.vrcirqEngine.writeIRQAcknowledge();
            }
        }
    }

    protected int getRegisterSlot(int address) {
        return ((address >> this.a0Bit) & 1) | (((address >> this.a1Bit) & 1) << 1);
    }

    private int mapChrAddress(int address) {
        address &= 0x1FFF;
        return switch (this.ppuBankingStyle & 0x03) {
            case 0 -> {
                if (address <= 0x03FF) {
                    yield (address & 0x3FF) | (this.R0 << 10);
                } else if (address <= 0x07FF) {
                    yield (address & 0x3FF) | (this.R1 << 10);
                } else if (address <= 0x0BFF) {
                    yield (address & 0x3FF) | (this.R2 << 10);
                } else if (address <= 0x0FFF) {
                    yield (address & 0x3FF) | (this.R3 << 10);
                } else if (address <= 0x13FF) {
                    yield (address & 0x3FF) | (this.R4 << 10);
                } else if (address <= 0x17FF) {
                    yield (address & 0x3FF) | (this.R5 << 10);
                } else if (address <= 0x1BFF) {
                    yield (address & 0x3FF) | (this.R6 << 10);
                } else {
                    yield (address & 0x3FF) | (this.R7 << 10);
                }
            }
            case 1 -> {
                int reg;
                if (address <= 0x07FF) {
                    reg = this.R0;
                } else if (address <= 0x0FFF) {
                    reg = this.R1;
                } else if (address <= 0x17FF) {
                    reg = this.R2;
                } else {
                    reg = this.R3;
                }
                int a10 = this.passPPUA10() ? (address >>> 10) & 1 : (reg & 1);
                yield (address & 0x3FF) | (a10 << 10) | ((reg >> 1) << 11);
            }
            case 2, 3 -> {
                if (address <= 0x03FF) {
                    yield (address & 0x3FF) | (this.R0 << 10);
                } else if (address <= 0x07FF) {
                    yield (address & 0x3FF) | (this.R1 << 10);
                } else if (address <= 0x0BFF) {
                    yield (address & 0x3FF) | (this.R2 << 10);
                } else if (address <= 0x0FFF) {
                    yield (address & 0x3FF) | (this.R3 << 10);
                } else {
                    int reg = address <= 0x17FF ? this.R4 : this.R5;
                    int a10 = this.passPPUA10() ? (address >>> 10) & 1 : (reg & 1);
                    yield (address & 0x3FF) | (a10 << 10) | ((reg >> 1) << 11);
                }
            }
            default -> throw new EmulatorException("Invalid PPU banking style bits for CHR banking!");
        };
    }

    @Override
    protected int mapNametableAddress(int address) {
        return this.mapNametableAddress(address, this.nametableArrangement);
        /*
        if ((this.ppuBankingStyle & (1 << 5)) != 0) {
            return switch (this.ppuBankingStyle & 3) {
                case 0 -> {

                }
                case 1 -> {

                }
                case 2 -> {

                }
                default -> throw new EmulatorException("Invalid PPU banking style bits for CHR banking!");
            };
        } else {

        }
         */
    }

    protected int mapPrgRomAddress(int address) {
        address &= 0x7FFF;
        if (address <= 0x3FFF) {
            return (address & 0x3FFF) | (this.prgSelect16K << 14);
        } else if (address <= 0x5FFF) {
            return (address & 0x1FFF) | (this.prgSelect8K << 13);
        } else {
            return (address & 0x1FFF) | (0b11111 << 13);
        }
    }

    private int mapPrgRamAddress(int address) {
        return address & 0x1FFF;
    }

    private boolean isPrgRamEnabled() {
        return (this.ppuBankingStyle & (1 << 7)) != 0;
    }

    private boolean passPPUA10() {
        return (this.ppuBankingStyle & (1 << 5)) != 0;
    }

    @Override
    public void cycle() {
        this.vrcirqEngine.cycle();
    }

    @Override
    public boolean getIRQSignal() {
        return this.vrcirqEngine.getIRQSignal();
    }

}
