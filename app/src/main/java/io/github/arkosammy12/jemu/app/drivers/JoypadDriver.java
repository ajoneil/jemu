package io.github.arkosammy12.jemu.app.drivers;

import de.gurkenlabs.input4j.InputComponent;
import io.github.arkosammy12.jemu.app.util.MavenProperties;
import io.github.arkosammy12.jemu.app.adapters.AbstractSystemAdapter;

import de.gurkenlabs.input4j.InputDevices;
import de.gurkenlabs.input4j.components.XInput;
import io.github.arkosammy12.jemu.core.common.SystemController;

public class JoypadDriver implements AutoCloseable {

    private final Thread pollThread;
    private volatile boolean running = true;

    private final AbstractSystemAdapter systemAdapter;

    public JoypadDriver(AbstractSystemAdapter systemAdapter) {
        this.systemAdapter = systemAdapter;

        this.pollThread = new Thread(this::pollLoop, "%s-joypad-poll-thread".formatted(MavenProperties.ARTIFACT_ID));
        this.pollThread.setDaemon(true);
        this.pollThread.start();
    }

    private void pressButton(InputComponent.ID id)
    {
        SystemController.Action action = systemAdapter.getActionForJoypadEvent(id);

        if(action != null)
            systemAdapter.getEmulator().getSystemController().onActionPressed(action);
    }

    private void releaseButton(InputComponent.ID id)
    {
        SystemController.Action action = systemAdapter.getActionForJoypadEvent(id);

        if(action != null)
            systemAdapter.getEmulator().getSystemController().onActionReleased(action);
    }

    private void pollLoop() {
        var devices = InputDevices.init();

        // For now, only support controllers connected at application start
        if(devices.getAll().isEmpty())
            return;

        var device = devices.getAll().iterator().next();

        device.onButtonPressed(XInput.DPAD_UP, () -> pressButton(XInput.DPAD_UP));
        device.onButtonPressed(XInput.DPAD_DOWN, () -> pressButton(XInput.DPAD_DOWN));
        device.onButtonPressed(XInput.DPAD_LEFT, () -> pressButton(XInput.DPAD_LEFT));
        device.onButtonPressed(XInput.DPAD_RIGHT, () -> pressButton(XInput.DPAD_RIGHT));
        device.onButtonPressed(XInput.START, () -> pressButton(XInput.START));
        device.onButtonPressed(XInput.BACK, () -> pressButton(XInput.BACK));
        device.onButtonPressed(XInput.X, () -> pressButton(XInput.X));
        device.onButtonPressed(XInput.A, () -> pressButton(XInput.A));

        device.onButtonReleased(XInput.DPAD_UP, () -> releaseButton(XInput.DPAD_UP));
        device.onButtonReleased(XInput.DPAD_DOWN, () -> releaseButton(XInput.DPAD_DOWN));
        device.onButtonReleased(XInput.DPAD_LEFT, () -> releaseButton(XInput.DPAD_LEFT));
        device.onButtonReleased(XInput.DPAD_RIGHT, () -> releaseButton(XInput.DPAD_RIGHT));
        device.onButtonReleased(XInput.START, () -> releaseButton(XInput.START));
        device.onButtonReleased(XInput.BACK, () -> releaseButton(XInput.BACK));
        device.onButtonReleased(XInput.X, () -> releaseButton(XInput.X));
        device.onButtonReleased(XInput.A, () -> releaseButton(XInput.A));

        while (running)
        {
            device.poll();

            try{
                Thread.sleep(5); // test
            } catch (InterruptedException e) {
                running = false;
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void close() {
        running = false;
        pollThread.interrupt();

        try {
            pollThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
