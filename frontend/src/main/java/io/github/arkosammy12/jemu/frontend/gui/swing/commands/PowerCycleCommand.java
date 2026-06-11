package io.github.arkosammy12.jemu.frontend.gui.swing.commands;

import io.github.arkosammy12.jemu.frontend.SystemDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record PowerCycleCommand(@NotNull SystemDescriptor systemDescriptor, boolean powerCycleIntoPaused) implements EmulatorCommand {}
