package io.github.arkosammy12.jemu.core.nes;

public class RP2A07<E extends NESEmulator> extends RP2A03<E> {

    public RP2A07(E emulator, int apuSampleBufferSize) {
        super(emulator, apuSampleBufferSize);
    }

    // TODO: Proper DMA halt behavior with the SYNC pin. https://forums.nesdev.org/viewtopic.php?t=25939

}
