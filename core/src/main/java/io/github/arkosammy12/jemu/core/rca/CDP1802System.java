package io.github.arkosammy12.jemu.core.rca;

import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.cpu.CDP1802;

public interface CDP1802System extends Emulator {

    CDP1802 getCpu();

}
