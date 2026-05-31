package io.github.arkosammy12.jemu.app.drivers;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.core.common.AudioGenerator;

public class MonoAudioRendererDriver extends DefaultAudioRendererDriver {

    public MonoAudioRendererDriver(Jemu jemu, AudioGenerator<?> audioGenerator) {
        super(jemu, audioGenerator);
    }

    @Override
    protected byte[] convertBitDepthIfNecessary(byte[] buf) {
        return switch (this.audioGenerator.getBytesPerSample()) {
            case BYTES_1 -> {
                byte[] buf16 = new byte[this.jemu.getAudioEngine().getBytesPerFrame()];
                for (int i = 0; i < buf.length; i++) {
                    int sample16 = ((int) buf[i] & 0xFF) * 256;
                    buf16[i * 2] = (byte) ((sample16 & 0xFF00) >>> 8);
                    buf16[(i * 2) + 1] = (byte) (sample16 & 0xFF);
                }
                yield buf16;
            }
            case BYTES_2 -> buf;
        };
    }

}
