package io.github.arkosammy12.jemu.core.rca.cosmacvip;

import io.github.arkosammy12.jemu.core.rca.CDP1861;

import java.util.Arrays;

public class  VP590<E extends CosmacVIPEmulator> extends CDP1861<E> {

    private static final int[] BACKGROUND_COLORS = {
            0x000080,
            0x000000,
            0x008000,
            0X800000
    };

    private final byte[] colorRam = new byte[256];
    private int backgroundColorIndex = 0;
    private boolean hiresColor = false;
    private boolean colorRamModified = false;

    public VP590(E emulator) {
        super(emulator);
        Arrays.fill(this.colorRam, (byte) 0xF0);
    }

    public void writeColorRam(int address, int value) {
        this.colorRamModified = true;
        if ((address >= 0xC000 && address <= 0xCFFF) || (address >= 0xE000 && address <= 0xEFFF)) {
            hiresColor = false;
        } else if ((address >= 0xD000 && address <= 0xDFFF) || (address >= 0xF000 && address <= 0xFFFF)) {
            hiresColor = true;
        }
        this.colorRam[address & (this.hiresColor ? 0xFF : 0xE7)] = (byte) (0xF0 | (value & 7));
    }

    public int readColorRam(int address) {
        return (int) this.colorRam[address & (this.hiresColor ? 0xFF : 0xE7)] & 0xFF;
    }

    public void incrementBackgroundColorIndex() {
        this.backgroundColorIndex = (this.backgroundColorIndex + 1) % BACKGROUND_COLORS.length;
    }

    @Override
    protected int getPixelRGB(int dmaOutAddress, boolean bit) {
        if (!bit) {
            return BACKGROUND_COLORS[this.colorRamModified ? this.backgroundColorIndex : 0];
        } else {
            if (!this.colorRamModified) {
                return 0xFFFFFF;
            } else {
                int colorByte = this.readColorRam(dmaOutAddress);
                int color = 0x000000;
                if ((colorByte & 1) != 0) {
                    color |= 0xFF0000;
                }
                if ((colorByte & 0b100) != 0) {
                    color |= 0x00FF00;
                }
                if ((colorByte & 0b10) != 0) {
                    color |= 0x0000FF;
                }
                return color;
            }
        }
    }

}