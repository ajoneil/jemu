package io.github.arkosammy12.jemu.frontend.gui.internal.commands;

public sealed interface EmulatorCommandCallback permits PauseCommandCallback, PowerCycleCommandCallback, StepCycleCommandCallback, StepFrameCommandCallback, StopCommandCallback {
}
