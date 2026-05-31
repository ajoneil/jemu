package io.github.arkosammy12.jemu.app.drivers;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.core.common.AudioGenerator;
import io.github.arkosammy12.jemu.core.drivers.AudioDriver;
import org.jetbrains.annotations.Nullable;

public abstract class DefaultAudioRendererDriver implements AudioDriver {

    protected final Jemu jemu;
    protected final AudioGenerator<?> audioGenerator;

    public DefaultAudioRendererDriver(Jemu jemu, AudioGenerator<?> audioGenerator) {
        this.jemu = jemu;
        this.audioGenerator = audioGenerator;
    }

    @Override
    public int getSampleRate() {
        return this.jemu.getAudioEngine().getSampleRate();
    }

    @Override
    public int getSamplesPerFrame() {
        return this.jemu.getAudioEngine().getSamplesPerFrame();
    }

    public byte @Nullable [] getSampleFrame() {
        return this.audioGenerator.getSampleFrame().map(this::convertBitDepthIfNecessary).orElse(null);
    }

    protected abstract byte[] convertBitDepthIfNecessary(byte[] buf);

}
