package io.github.arkosammy12.jemu.core.common;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface AudioGenerator {

    boolean isStereo();

    @NotNull
    SampleSize getBytesPerSample();

    Optional<byte[]> getSampleFrame();

    enum SampleSize {
        BYTES_1,
        BYTES_2
    }

}
