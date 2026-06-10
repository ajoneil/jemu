package io.github.arkosammy12.jemu.frontend.gui.swing.commands;

import io.github.arkosammy12.jemu.frontend.SystemDescriptor;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record PowerCycleCommand(@Nullable SystemDescriptor systemDescriptor, boolean powerCycleIntoPaused) implements EmulatorCommand {

    public Optional<SystemDescriptor> getSystemDescriptor() {
        return Optional.ofNullable(this.systemDescriptor);
    }

}
