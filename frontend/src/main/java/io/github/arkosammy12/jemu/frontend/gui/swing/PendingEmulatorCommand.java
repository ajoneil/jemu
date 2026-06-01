package io.github.arkosammy12.jemu.frontend.gui.swing;

import io.github.arkosammy12.jemu.frontend.gui.swing.commands.EmulatorCommand;

public sealed interface PendingEmulatorCommand permits MainWindow.PendingEmulatorCommandImpl {

    EmulatorCommand getEmulatorCommand();

    void acknowledge();

}
