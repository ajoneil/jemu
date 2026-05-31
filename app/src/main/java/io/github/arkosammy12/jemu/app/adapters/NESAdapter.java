package io.github.arkosammy12.jemu.app.adapters;

import de.gurkenlabs.input4j.InputComponent;
import de.gurkenlabs.input4j.components.XInput;
import io.github.arkosammy12.jemu.app.io.initializers.CoreInitializer;
import io.github.arkosammy12.jemu.app.util.System;
import io.github.arkosammy12.jemu.core.common.Emulator;
import io.github.arkosammy12.jemu.core.nes.NESController;
import io.github.arkosammy12.jemu.core.nes.NESEmulator;
import org.jetbrains.annotations.Nullable;

import java.awt.event.KeyEvent;
import java.util.Optional;

public class NESAdapter extends AbstractSystemAdapter {

    private final String romTitle;
    private final System system;

    public NESAdapter(CoreInitializer initializer) {
        this.romTitle = initializer.getRomPath().map(path -> path.getFileName().toString()).orElse(null);
        this.system = System.NES;
        super(initializer);
    }

    @Override
    protected Emulator createEmulator() {
        return new NESEmulator(this);
    }

    @Override
    @Nullable
    protected NESController.Actions getActionForKeyCode(int keyCode) {
        return switch (keyCode) {
            case KeyEvent.VK_W -> NESController.Actions.UP;
            case KeyEvent.VK_S -> NESController.Actions.DOWN;
            case KeyEvent.VK_A -> NESController.Actions.LEFT;
            case KeyEvent.VK_D -> NESController.Actions.RIGHT;
            case KeyEvent.VK_ENTER -> NESController.Actions.START;
            case KeyEvent.VK_BACK_SPACE -> NESController.Actions.SELECT;
            case KeyEvent.VK_J -> NESController.Actions.A;
            case KeyEvent.VK_K -> NESController.Actions.B;
            default -> null;
        };
    }

    @Override
    @Nullable
    public NESController.Actions getActionForJoypadEvent(InputComponent.ID id) {
        if (id == XInput.DPAD_UP)
            return NESController.Actions.UP;
        else if (id == XInput.DPAD_DOWN)
            return NESController.Actions.DOWN;
        else if (id == XInput.DPAD_LEFT)
            return NESController.Actions.LEFT;
        else if (id == XInput.DPAD_RIGHT)
            return NESController.Actions.RIGHT;
        else if (id == XInput.START)
            return NESController.Actions.START;
        else if (id == XInput.BACK)
            return NESController.Actions.SELECT;
        else if (id == XInput.A)
            return NESController.Actions.A;
        else if (id == XInput.X)
            return NESController.Actions.B;
        else
            return null;
    }

    @Override
    public String getSystemName() {
        return this.system.getName();
    }

    @Override
    public Optional<String> getRomTitle() {
        return Optional.ofNullable(this.romTitle);
    }

    @Override
    public System getSystem() {
        return this.system;
    }

}
