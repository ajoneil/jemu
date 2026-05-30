package io.github.arkosammy12.jemu.core.gameboy;

import io.github.arkosammy12.jemu.core.common.SystemHost;

import java.nio.file.Path;
import java.util.Optional;

public interface GameBoyHost extends SystemHost {

    Model getModel();

    Optional<Path> getSaveDataDirectory();

    enum Model {
        DMG,
        CGB,
    }

}
